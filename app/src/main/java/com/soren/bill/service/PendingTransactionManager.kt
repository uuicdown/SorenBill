package com.soren.bill.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 后台 AccessibilityService 与前台 Compose UI 之间的数据桥梁。
 * 服务解析到支付数据后写入 pendingTransaction，UI 层 collect 并弹出底部确认弹窗。
 */
object PendingTransactionManager {
    private val _pendingTransaction = MutableStateFlow<ParsedPaymentInfo?>(null)
    val pendingTransaction: StateFlow<ParsedPaymentInfo?> = _pendingTransaction.asStateFlow()

    private val _suggestedCategory = MutableStateFlow<String?>(null)
    val suggestedCategory: StateFlow<String?> = _suggestedCategory.asStateFlow()

    fun postTransaction(info: ParsedPaymentInfo, suggestedCategory: String? = null) {
        _pendingTransaction.value = info
        _suggestedCategory.value = suggestedCategory
    }

    fun confirmAndClear() {
        _pendingTransaction.value = null
        _suggestedCategory.value = null
    }

    /** 是否有待确认的交易 */
    val hasPending: Boolean get() = _pendingTransaction.value != null
}
