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

    /** 手动扫描触发器：UI 写入 true → 服务收到后执行扫描 → 写回 false */
    private val _scanRequest = MutableStateFlow(false)
    val scanRequest: StateFlow<Boolean> = _scanRequest.asStateFlow()

    /** 最后一次手动扫描的日志消息（用于 UI 展示） */
    private val _scanLog = MutableStateFlow<String?>(null)
    val scanLog: StateFlow<String?> = _scanLog.asStateFlow()

    fun postTransaction(info: ParsedPaymentInfo, suggestedCategory: String? = null) {
        _pendingTransaction.value = info
        _suggestedCategory.value = suggestedCategory
    }

    fun confirmAndClear() {
        _pendingTransaction.value = null
        _suggestedCategory.value = null
    }

    /** 请求一次手动扫描 */
    fun requestScan() { _scanRequest.value = true }

    /** 服务端调用：消费扫描请求 */
    fun consumeScanRequest(): Boolean {
        val pending = _scanRequest.value
        if (pending) _scanRequest.value = false
        return pending
    }

    /** 写入扫描日志（UI 层 collect 展示） */
    fun postScanLog(msg: String) { _scanLog.value = msg }

    /** 清除扫描日志 */
    fun clearScanLog() { _scanLog.value = null }

    /** 是否有待确认的交易 */
    val hasPending: Boolean get() = _pendingTransaction.value != null
}
