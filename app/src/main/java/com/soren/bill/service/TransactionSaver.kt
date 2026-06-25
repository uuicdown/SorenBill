package com.soren.bill.service

import com.soren.bill.data.entity.Account
import com.soren.bill.data.entity.Category
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.entity.Wallet
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.flow.first

/**
 * 记账持久化工具。
 * AccessibilityService 和 NotificationListener 共用同一套写入逻辑。
 */
object TransactionSaver {

    suspend fun save(info: ParsedPaymentInfo, repository: BillRepository) {
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
            val accName = when (info.paymentMethod) {
                "微信" -> "微信"
                "支付宝" -> "支付宝"
                else -> "微信/支付宝"
            }
            val accType = when (info.paymentMethod) {
                "微信" -> "wechat"
                "支付宝" -> "alipay"
                else -> "other"
            }
            repository.insertAccount(Account(name = accName, type = accType))
            accountId = repository.getAllAccounts().first().first().id
        }

        // 匹配分类
        val suggestion = MerchantCategoryMapper.classify(info.merchant)
        val categoryName = suggestion?.targetCategoryName ?: "其他"
        val expenseCategories = repository.getCategoriesByType("expense").first()
        var categoryId = expenseCategories.firstOrNull { it.name == categoryName }?.id

        if (categoryId == null) {
            repository.insertCategory(Category(name = categoryName, type = "expense"))
            categoryId = repository.getCategoriesByType("expense").first()
                .firstOrNull { it.name == categoryName }?.id
        }

        // 构建备注
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
}
