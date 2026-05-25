package com.soren.bill.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.soren.bill.MainActivity
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.entity.Wallet
import com.soren.bill.data.entity.Account
import com.soren.bill.data.entity.Category
import com.soren.bill.data.preferences.AppPreferences
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject

class AutoAccountingAccessibilityService : AccessibilityService() {

    private val repository: BillRepository by inject()
    private val appPreferences: AppPreferences by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val TAG = "AutoAccounting"
        const val CHANNEL_ID = "auto_accounting_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW_CONFIRMATION = "com.soren.bill.ACTION_SHOW_CONFIRMATION"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "AccessibilityService created and bound")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 只处理微信/支付宝的窗口变化和内容刷新
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in listOf("com.tencent.mm", "com.eg.android.AlipayGphone")) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        // 轻度延迟，确保页面渲染完全
        serviceScope.launch {
            delay(800) // 给微信/支付宝 UI 一点渲染时间
            handleEvent()
        }
    }

    private suspend fun handleEvent() {
        val root = rootInActiveWindow ?: run {
            Log.d(TAG, "rootInActiveWindow is null")
            return
        }

        if (!PaymentScreenParser.isPaymentSuccessScreen(root)) return

        Log.d(TAG, "检测到支付成功页面，开始解析...")
        val info = PaymentScreenParser.parse(root) ?: run {
            Log.d(TAG, "解析失败，无法提取金额")
            return
        }

        Log.d(TAG, "解析成功: 金额=${info.amount}, 商户=${info.merchant}, 订单号=${info.orderId}")

        val shouldConfirm = appPreferences.confirmBeforeSaving.first()

        if (shouldConfirm) {
            // 路由 A：弹出确认弹窗
            handleWithConfirmation(info)
        } else {
            // 路由 B：静默写入
            handleSilentSave(info)
        }
    }

    /**
     * 前台弹窗确认流程：
     * 1. 存入 PendingTransactionManager
     * 2. 发送通知引导用户打开 App
     * 3. UI 层检测到 pending 数据后自动弹出 BottomSheet
     */
    private fun handleWithConfirmation(info: ParsedPaymentInfo) {
        val suggestion = MerchantCategoryMapper.classify(info.merchant)
        PendingTransactionManager.postTransaction(info, suggestion?.targetCategoryName)

        // 发送通知，引导用户打开 App 确认
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_SHOW_CONFIRMATION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Soren 发现一笔花销～"
        val body = buildString {
            append("金额: ¥${String.format("%.2f", info.amount)}")
            if (!info.merchant.isNullOrBlank()) append("\n商户: ${info.merchant}")
            if (suggestion != null) append("\n建议分类: ${suggestion.targetCategoryName}")
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body.replace("\n", "  "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d(TAG, "已发送确认通知: $body")
    }

    /**
     * 静默写入流程：
     * 直接在后台把解析结果写入 Room 数据库
     */
    private suspend fun handleSilentSave(info: ParsedPaymentInfo) {
        try {
            saveTransaction(info)
            Log.d(TAG, "静默记账成功: ¥${info.amount} - ${info.merchant}")
        } catch (e: Exception) {
            Log.e(TAG, "静默记账失败: ${e.message}", e)
        }
    }

    /**
     * 将解析结果写入数据库。
     * 自动尝试匹配分类，默认回退到"其他"分类。
     */
    private suspend fun saveTransaction(info: ParsedPaymentInfo) {
        // 获取或创建默认钱包
        val wallets = repository.getAllWallets().first()
        val walletId = wallets.firstOrNull()?.id ?: run {
            repository.insertWallet(Wallet(name = "我的钱包", currency = "CNY"))
            repository.getAllWallets().first().first().id
        }

        // 获取或创建默认账户
        val accounts = repository.getAllAccounts().first()
        var accountId = accounts.firstOrNull()?.id
        if (accountId == null) {
            repository.insertAccount(Account(name = "微信/支付宝", type = "other"))
            accountId = repository.getAllAccounts().first().first().id
        }

        // 匹配分类
        val suggestion = MerchantCategoryMapper.classify(info.merchant)
        val categoryName = suggestion?.targetCategoryName ?: "其他"
        val expenseCategories = repository.getCategoriesByType("expense").first()
        var categoryId = expenseCategories.firstOrNull { it.name == categoryName }?.id

        if (categoryId == null) {
            // 自动创建新分类
            repository.insertCategory(Category(name = categoryName, type = "expense"))
            categoryId = repository.getCategoriesByType("expense").first()
                .firstOrNull { it.name == categoryName }?.id
        }

        val note = buildString {
            if (!info.merchant.isNullOrBlank()) append(info.merchant)
            if (!info.orderId.isNullOrBlank()) {
                if (isNotEmpty()) append(" | ")
                append("订单: ${info.orderId}")
            }
        }.takeIf { it.isNotBlank() }

        val transaction = Transaction(
            amount = info.amount,
            type = "expense",
            walletId = walletId,
            accountId = accountId!!,
            categoryId = categoryId!!,
            date = System.currentTimeMillis(),
            note = note
        )
        repository.insertTransaction(transaction)
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "AccessibilityService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Soren 自动记账",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于提醒您确认 Soren 自动识别的账单"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

