package com.soren.bill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.data.entity.Account
import com.soren.bill.data.entity.Category
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.entity.Wallet
import com.soren.bill.data.preferences.AppPreferences
import com.soren.bill.data.preferences.ThemeMode
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.service.AutoAccountingAccessibilityService
import com.soren.bill.service.ParsedPaymentInfo
import com.soren.bill.service.PendingTransactionManager
import com.soren.bill.ui.navigation.AppNavigation
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.IncomeGreen
import com.soren.bill.ui.theme.SorenBillTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 检查是否从确认通知打开
        val isFromNotification = intent?.action == AutoAccountingAccessibilityService.ACTION_SHOW_CONFIRMATION

        setContent {
            val appPreferences: AppPreferences = koinInject()
            val repository: BillRepository = koinInject()
            val themeMode by appPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val pendingInfo by PendingTransactionManager.pendingTransaction.collectAsState()
            val suggestedCategory by PendingTransactionManager.suggestedCategory.collectAsState()
            var showConfirmation by remember { mutableStateOf(false) }

            // 如果从通知打开且有 pending 数据，自动弹窗
            LaunchedEffect(isFromNotification, pendingInfo) {
                if (pendingInfo != null) {
                    showConfirmation = true
                }
            }

            SorenBillTheme(darkTheme = isDark) {
                Box(Modifier.fillMaxSize()) {
                    AppNavigation()

                    // 自动记账确认弹窗
                    if (showConfirmation && pendingInfo != null) {
                        AutoAccountingConfirmationSheet(
                            info = pendingInfo!!,
                            suggestedCategory = suggestedCategory,
                            repository = repository,
                            onConfirm = {
                                showConfirmation = false
                                PendingTransactionManager.confirmAndClear()
                            },
                            onDismiss = {
                                showConfirmation = false
                                PendingTransactionManager.confirmAndClear()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoAccountingConfirmationSheet(
    info: ParsedPaymentInfo,
    suggestedCategory: String?,
    repository: BillRepository,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isExpense = true // 自动记账默认都是支出
    val color = if (isExpense) ExpenseRed else IncomeGreen

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                "检测到新账单",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "请确认以下自动识别的账单信息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // 金额大字
            Text(
                "¥${String.format("%.2f", info.amount)}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text("支出", style = MaterialTheme.typography.labelLarge, color = color)

            Spacer(Modifier.height(20.dp))

            // 详情卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (!info.merchant.isNullOrBlank()) {
                        DetailRow("商户", info.merchant)
                    }
                    if (suggestedCategory != null) {
                        DetailRow("建议分类", suggestedCategory)
                    }
                    if (!info.paymentMethod.isNullOrBlank()) {
                        DetailRow("支付方式", info.paymentMethod)
                    }
                    if (!info.orderId.isNullOrBlank()) {
                        DetailRow("订单号", info.orderId)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 确认 / 取消 按钮
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("忽略")
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                saveParsedTransaction(repository, info, suggestedCategory)
                                onConfirm()
                            } catch (e: Exception) {
                                // 容错：保存失败也关闭弹窗
                                onConfirm()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("确认入账")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** 将解析结果持久化写入数据库（与 Service 中 handleSilentSave 逻辑一致） */
private suspend fun saveParsedTransaction(
    repository: BillRepository,
    info: ParsedPaymentInfo,
    suggestedCategory: String?
) {
    // 获取或创建默认钱包
    val wallets = repository.getAllWallets().first()
    val walletId = wallets.firstOrNull()?.id ?: run {
        repository.insertWallet(Wallet(name = "默认钱包", currency = "CNY"))
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
    val categoryName = suggestedCategory ?: "其他"
    val expenseCategories = repository.getCategoriesByType("expense").first()
    var categoryId = expenseCategories.firstOrNull { it.name == categoryName }?.id

    if (categoryId == null) {
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
