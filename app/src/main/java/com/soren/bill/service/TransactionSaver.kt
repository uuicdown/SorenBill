package com.soren.bill.service

import com.soren.bill.data.entity.Account
import com.soren.bill.data.entity.Category
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.entity.Wallet
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.util.AccountingConstants
import kotlinx.coroutines.flow.first

/**
 * 记账持久化工具（共享写入逻辑）。
 * AccessibilityService / NotificationListener / MainActivity 共用。
 *
 * 注意：所有 Flow.first() 调用在空数据库下也能正常返回空列表，
 * 后续通过 [run] 块自动创建默认数据。
 */
object TransactionSaver {

    suspend fun save(info: ParsedPaymentInfo, repository: BillRepository) {
        val walletId = resolveWalletId(repository)
        val accountId = resolveAccountId(repository, info.paymentMethod)
        val categoryId = resolveCategoryId(repository, info.merchant)

        val note = buildString {
            if (!info.merchant.isNullOrBlank()) append(info.merchant)
            if (!info.orderId.isNullOrBlank()) {
                if (isNotEmpty()) append(" | ")
                append("订单: ${info.orderId}")
            }
        }.takeIf { it.isNotBlank() }

        val transaction = Transaction(
            amount = info.amount,
            type = AccountingConstants.TYPE_EXPENSE,
            walletId = walletId,
            accountId = accountId,
            categoryId = categoryId,
            date = System.currentTimeMillis(),
            note = note
        )
        repository.insertTransaction(transaction)
    }

    private suspend fun resolveWalletId(repository: BillRepository): Long {
        val wallets = repository.getAllWallets().first()
        return wallets.firstOrNull()?.id ?: run {
            repository.insertWallet(Wallet(name = AccountingConstants.CATEGORY_DEFAULT_WALLET, currency = "CNY"))
            repository.getAllWallets().first().first().id
        }
    }

    private suspend fun resolveAccountId(repository: BillRepository, paymentMethod: String?): Long {
        val accounts = repository.getAllAccounts().first()
        return accounts.firstOrNull()?.id ?: run {
            val accName = when (paymentMethod) {
                AccountingConstants.PAY_WECHAT -> AccountingConstants.PAY_WECHAT
                AccountingConstants.PAY_ALIPAY -> AccountingConstants.PAY_ALIPAY
                else -> AccountingConstants.CATEGORY_DEFAULT_ACCOUNT
            }
            val accType = when (paymentMethod) {
                AccountingConstants.PAY_WECHAT -> AccountingConstants.ACCOUNT_WECHAT
                AccountingConstants.PAY_ALIPAY -> AccountingConstants.ACCOUNT_ALIPAY
                else -> AccountingConstants.ACCOUNT_OTHER
            }
            repository.insertAccount(Account(name = accName, type = accType))
            repository.getAllAccounts().first().first().id
        }
    }

    private suspend fun resolveCategoryId(repository: BillRepository, merchant: String?): Long {
        val suggestion = MerchantCategoryMapper.classify(merchant)
        val categoryName = suggestion?.targetCategoryName ?: AccountingConstants.CATEGORY_OTHER
        val expenseCategories = repository.getCategoriesByType(AccountingConstants.TYPE_EXPENSE).first()
        return expenseCategories.firstOrNull { it.name == categoryName }?.id ?: run {
            repository.insertCategory(Category(name = categoryName, type = AccountingConstants.TYPE_EXPENSE))
            repository.getCategoriesByType(AccountingConstants.TYPE_EXPENSE).first()
                .firstOrNull { it.name == categoryName }?.id
                ?: error("无法创建或获取分类: $categoryName")
        }
    }

    /** 获取余额调整分类 ID，用于 addAccount / adjustBalance */
    suspend fun resolveAdjustmentCategoryId(repository: BillRepository, type: String): Long {
        val cats = repository.getCategoriesByType(type).first()
        return cats.firstOrNull { it.name == AccountingConstants.CATEGORY_ADJUSTMENT }?.id
            ?: cats.firstOrNull()?.id
            ?: error("无可用的${type}分类，请先通过「我的 → 分类管理」添加")
    }
}
