package com.soren.bill.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.soren.bill.data.preferences.AppPreferences
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject

/**
 * 通知栏监听服务。
 *
 * 与 AccessibilityService 并行工作，捕获微信/支付宝的支出通知，
 * 解析金额和商户名后走相同的确认/静默写入管道。
 *
 * 用户需在系统设置中授予「通知使用权」。
 */
class PaymentNotificationListener : NotificationListenerService() {

    private val repository: BillRepository by inject()
    private val appPreferences: AppPreferences by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val TAG = "PaymentNotif"

        // 目标应用包名
        private val TARGET_PACKAGES = setOf(
            "com.tencent.mm",               // 微信
            "com.eg.android.AlipayGphone"   // 支付宝
        )

        // 微信支付通知标题关键词
        private val WECHAT_PAY_TITLES = listOf("微信支付", "微信收款")

        // 支付宝支出通知内容关键词
        private val ALIPAY_SPENDING_KEYWORDS = listOf(
            "支出", "消费", "付款", "转账", "扫码", "面对面"
        )

        // 金额提取正则
        private val AMOUNT_REGEX = Regex("""[¥￥]\s*(\d+\.?\d{0,2})""")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in TARGET_PACKAGES) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(NotificationCompat.EXTRA_TITLE, "") ?: ""
        val content = extras.getString(NotificationCompat.EXTRA_TEXT, "") ?: ""
        val bigText = extras.getString(NotificationCompat.EXTRA_BIG_TEXT) ?: ""
        val combined = "$content $bigText"

        Log.d(TAG, "收到通知: pkg=$pkg title=$title")

        val amount: Double
        val merchant: String?
        val paymentMethod: String

        when (pkg) {
            "com.tencent.mm" -> {
                // 微信：只看支付类通知标题
                if (WECHAT_PAY_TITLES.none { title.contains(it) }) return
                amount = extractAmount(combined) ?: return
                merchant = extractMerchant(combined)
                paymentMethod = "微信"
            }
            "com.eg.android.AlipayGphone" -> {
                // 支付宝：内容必须含支出关键词
                if (ALIPAY_SPENDING_KEYWORDS.none { combined.contains(it) }) return
                amount = extractAmount(combined) ?: return
                merchant = extractMerchant(combined)
                paymentMethod = "支付宝"
            }
            else -> return
        }

        Log.d(TAG, "通知解析成功: 金额=$amount 商户=$merchant 来源=$paymentMethod")

        val info = ParsedPaymentInfo(
            amount = amount,
            merchant = merchant,
            paymentMethod = paymentMethod,
            orderId = null // 通知通常不含订单号
        )

        serviceScope.launch {
            val shouldConfirm = appPreferences.confirmBeforeSaving.first()
            if (shouldConfirm) {
                // 路由 A：推送确认弹窗
                val suggestion = MerchantCategoryMapper.classify(merchant)
                PendingTransactionManager.postTransaction(info, suggestion?.targetCategoryName)
                Log.d(TAG, "已转发到确认通道")
            } else {
                // 路由 B：静默写入
                try {
                    TransactionSaver.save(info, repository)
                    Log.d(TAG, "通知静默记账成功: ¥$amount - $merchant")
                } catch (e: Exception) {
                    Log.e(TAG, "通知静默记账失败", e)
                }
            }
        }
    }

    /**
     * 从文本中提取金额
     */
    private fun extractAmount(text: String): Double? {
        return AMOUNT_REGEX.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    /**
     * 从通知文本中提取商户名
     * 微信/支付宝通知首行通常为商户名称
     */
    private fun extractMerchant(text: String): String? {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        return lines.firstOrNull { line ->
            line.length in 2..30 &&
                !line.contains("¥") && !line.contains("￥") &&
                !line.matches(Regex("""[\d\-:/年月日时分秒\s]+"""))
        }
    }

    override fun onListenerConnected() {
        Log.d(TAG, "NotificationListener 已连接")
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "NotificationListener 已断开")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
