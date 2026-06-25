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
import com.soren.bill.service.TransactionSaver
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
                "\u60a8\u770b\u770b\u662f\u4e0d\u662f\u8fd9\u7b14\uff1f",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Soren \u5e2e\u60a8\u8bb0\u4e0b\u4e86\uff0c\u786e\u8ba4\u4e00\u4e0b\u5427\uff1f",
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
                        DetailRow("\u5546\u6237", info.merchant)
                    }
                    if (suggestedCategory != null) {
                        DetailRow("\u63a8\u8350\u5206\u7c7b", suggestedCategory)
                    }
                    if (!info.paymentMethod.isNullOrBlank()) {
                        DetailRow("\u652f\u4ed8\u65b9\u5f0f", info.paymentMethod)
                    }
                    if (!info.orderId.isNullOrBlank()) {
                        DetailRow("\u8ba2\u5355\u53f7", info.orderId)
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
                    Text("\u4e0d\u662f\u6211\u7684")
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
                    Text("\u597d\u7684\uff0c\u8bb0\u4e0b\u5b83")
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

private suspend fun saveParsedTransaction(
    repository: BillRepository,
    info: ParsedPaymentInfo,
    suggestedCategory: String?
) {
    // 注释：suggestedCategory 在 TransactionSaver 内部通过 MerchantCategoryMapper 重新匹配，
    // 外部传入的分类仅在调用方用于预览，持久化以内部匹配为准。
    TransactionSaver.save(info, repository)
}
