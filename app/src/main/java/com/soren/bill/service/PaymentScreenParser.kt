package com.soren.bill.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * 从微信/支付宝"支付成功"页面的 AccessibilityNodeInfo 树中提取交易信息。
 * 策略：纯文本匹配（不依赖混淆 View ID），仅适配最新版微信/支付宝。
 */
data class ParsedPaymentInfo(
    val amount: Double,
    val merchant: String?,
    val paymentMethod: String?,
    val orderId: String?
)

@Suppress("DEPRECATION")
object PaymentScreenParser {

    private val AMOUNT_REGEX = Regex("""[¥￥\$]\s*(\d+\.?\d{0,2})""")
    private val SUCCESS_KEYWORDS = listOf(
        "支付成功", "付款成功", "交易成功", "支付结果", "转账成功",
        "Payment Successful", "Payment Success", "Successful Payment",
        "Paid Successfully", "Transaction Successful"
    )
    private val MERCHANT_LABELS = listOf(
        "收款方", "商户", "收款商家", "付款对象", "对方账户", "收款人", "商品", "付款详情",
        "Merchant", "Payee", "Seller", "收款方"
    )
    private val METHOD_LABELS = listOf("支付方式", "付款方式", "Payment Method", "Payment")
    private val ORDER_LABELS = listOf(
        "订单号", "交易单号", "商户订单号", "转账单号",
        "Order ID", "Transaction ID", "Order Number", "Trade No."
    )
    /** 交易详情页/账单页的关键词（标题栏 + 页面内标签） */
    private val DETAIL_KEYWORDS = listOf(
        // 中文
        "账单详情", "交易详情", "支付详情", "订单详情",
        "交易记录", "账单", "明细",
        // English
        "Transaction Details", "Bill Details", "Payment Details", "Order Details",
        "Transaction Record", "Bill", "Details",
        // 通用状态标签（详情页一定包含）
        "交易状态", "支付状态", "收款方", "付款方",
        "Status", "Amount", "Merchant", "Order Number", "Order ID",
        "Transaction ID", "Trade No.", "Payment Method",
        "收款方式", "付款方式", "交易时间", "订单编号",
        "Time", "Date"
    )

    /** 金额+标签双检测：页面同时含 ¥ 金额和支付标签 → 判定为支付相关页面 */
    private val AMOUNT_AND_LABEL_KEYWORDS = listOf(
        "¥", "￥",
        "amount", "Amount", "金额",
        "商户", "merchant", "Merchant", "收款方", "Payee",
        "订单", "order", "Order", "交易", "Trade",
        "支付", "payment", "Payment"
    )

    /**
     * BFS 扫描文本，检查页面是否包含任意关键词
     */
    private fun screenContainsAny(root: AccessibilityNodeInfo?, keywords: List<String>): Boolean {
        if (root == null) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val combined = "$text $desc"
            if (keywords.any { combined.contains(it) }) {
                return true
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    /**
     * 判断当前窗口是否为支付成功页面
     */
    fun isPaymentSuccessScreen(root: AccessibilityNodeInfo?): Boolean {
        return screenContainsAny(root, SUCCESS_KEYWORDS)
    }

    /**
     * 判断当前窗口是否为交易详情页
     */
    fun isTransactionDetailScreen(root: AccessibilityNodeInfo?): Boolean {
        return screenContainsAny(root, DETAIL_KEYWORDS)
    }

    /**
     * 通过金额+标签双检测，判断是否为支付相关页面（不依赖精确标题）。
     * 兜底策略：成功页+详情页关键词都无法匹配时，只要页面同时含 ¥ 金额和支付标签就触发。
     */
    fun isPaymentRelatedPage(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val allTexts = extractAllTexts(root).joinToString(" ")
        val hasAmount = AMOUNT_REGEX.containsMatchIn(allTexts)
        val hasLabel = AMOUNT_AND_LABEL_KEYWORDS.any { allTexts.contains(it, ignoreCase = true) }
        return hasAmount && hasLabel
    }

    /**
     * 判断当前窗口是否为支付成功页或交易详情页（无障碍服务入口用）
     */
    fun isPaymentOrDetailScreen(root: AccessibilityNodeInfo?): Boolean {
        return isPaymentSuccessScreen(root)
            || isTransactionDetailScreen(root)
            || isPaymentRelatedPage(root)
    }

    /**
     * 从节点树中提取金额（取第一个匹配到的金额，通常也是最大的那个）
     */
    fun extractAmount(root: AccessibilityNodeInfo?): Double? {
        if (root == null) return null
        val amounts = mutableListOf<Double>()
        extractAllTexts(root).forEach { text ->
            AMOUNT_REGEX.findAll(text).forEach { match ->
                match.groupValues[1].toDoubleOrNull()?.let { amounts.add(it) }
            }
        }
        // 支付成功页通常只有一个核心金额，取最大的（排除可能的尾数/退款）
        return amounts.maxOrNull()
    }

    /**
     * 提取商户名称：搜索标签旁边的节点文本
     */
    fun extractMerchant(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null
        return extractValueNearLabel(root, MERCHANT_LABELS)
    }

    /**
     * 提取支付方式
     */
    fun extractPaymentMethod(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null
        return extractValueNearLabel(root, METHOD_LABELS)
    }

    /**
     * 提取订单号
     */
    fun extractOrderId(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null
        return extractValueNearLabel(root, ORDER_LABELS)
    }

    /**
     * 完整解析：从 root 节点提取全部支付信息
     */
    fun parse(root: AccessibilityNodeInfo?): ParsedPaymentInfo? {
        if (root == null) return null
        val amount = extractAmount(root) ?: return null
        return ParsedPaymentInfo(
            amount = amount,
            merchant = extractMerchant(root),
            paymentMethod = extractPaymentMethod(root),
            orderId = extractOrderId(root)
        )
    }

    // ----- 内部工具方法 -----

    /** BFS 收集所有文本（contentDescription 也计入） */
    private fun extractAllTexts(root: AccessibilityNodeInfo): List<String> {
        val result = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { result.add(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { result.add(it) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return result
    }

    /**
     * 搜索标签节点 → 向右/向下寻找紧邻的值节点
     * 微信/支付宝通常在同级或父子关系中
     */
    private fun extractValueNearLabel(root: AccessibilityNodeInfo, labels: List<String>): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString() ?: ""

            if (labels.any { text.contains(it) }) {
                // 1. 同级右兄弟（常见布局）
                val parent = node.parent ?: node
                var foundLabel = false
                for (i in 0 until parent.childCount) {
                    val child = parent.getChild(i) ?: continue
                    val childText = child.text?.toString() ?: ""
                    if (childText == text) {
                        foundLabel = true
                        continue
                    }
                    if (foundLabel && childText.isNotBlank() && childText.length in 2..40) {
                        // 排除纯数字日期/金额节点
                        if (!childText.matches(Regex("""[\d¥￥.\-/年月日:：\s]+"""))) {
                            return childText
                        }
                    }
                }
                // 2. 第一层子节点（常见布局：LinearLayout 内包含 Label + Value）
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    val childText = child.text?.toString() ?: ""
                    if (childText.isNotBlank() && childText != text && childText.length in 2..40) {
                        if (!childText.matches(Regex("""[\d¥￥.\-/年月日:：\s]+"""))) {
                            return childText
                        }
                    }
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }
}
