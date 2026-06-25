package com.soren.bill.util

/**
 * 会计相关常量，消除各模块间的魔术字符串。
 * 所有收支类型、分类名等统一在此定义。
 */
object AccountingConstants {
    /** 交易类型 */
    const val TYPE_INCOME = "income"
    const val TYPE_EXPENSE = "expense"

    /** 余额调整分类名（通过 isAdjustment=true 标记） */
    const val CATEGORY_ADJUSTMENT = "余额调整"
    const val CATEGORY_OTHER = "其他"
    const val CATEGORY_DEFAULT_WALLET = "日常钱包"
    const val CATEGORY_DEFAULT_ACCOUNT = "微信/支付宝"

    /** 账户类型 */
    const val ACCOUNT_WECHAT = "wechat"
    const val ACCOUNT_ALIPAY = "alipay"
    const val ACCOUNT_BANK = "bank_card"
    const val ACCOUNT_CREDIT = "credit_card"
    const val ACCOUNT_LOAN = "loan"
    const val ACCOUNT_CASH = "cash"
    const val ACCOUNT_OTHER = "other"

    /** 支付方式 */
    const val PAY_WECHAT = "微信"
    const val PAY_ALIPAY = "支付宝"
}
