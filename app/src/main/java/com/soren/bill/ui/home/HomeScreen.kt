package com.soren.bill.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.util.DateUtils
import com.soren.bill.ui.theme.categoryIcon
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onAddClick: () -> Unit, onStatsClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTx by remember { mutableStateOf<com.soren.bill.data.entity.Transaction?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HomeMonthHeader(
                timestamp = uiState.currentMonthTimestamp,
                onSwitchMonth = { viewModel.switchMonth(it) },
                onStatsClick = onStatsClick
            )
            SummaryCard(income = uiState.monthlyIncome, expense = uiState.monthlyExpense)
            if (uiState.isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else if (uiState.transactions.isEmpty()) Column(Modifier.fillMaxSize().padding(bottom = 72.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                }
                Spacer(Modifier.height(24.dp))
                Text("本月暂无收支记录", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("点击右下角按钮记一笔", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TxRow(tx, uiState.categoryMap[tx.categoryId] ?: "未分类") { selectedTx = tx }
                }
            }
        }
        FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape, elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "记一笔") }
            
        selectedTx?.let { tx ->
            TransactionDetailDialog(
                tx = tx,
                categoryName = uiState.categoryMap[tx.categoryId] ?: "未分类",
                walletName = uiState.wallets.find { it.id == tx.walletId }?.name ?: "未知钱包",
                accountName = uiState.accounts.find { it.id == tx.accountId }?.name ?: "未知账户",
                onDismiss = { selectedTx = null },
                onDelete = { 
                    viewModel.deleteTransaction(tx)
                    selectedTx = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMonthHeader(timestamp: Long, onSwitchMonth: (Long) -> Unit, onStatsClick: () -> Unit) {
    val cal = Calendar.getInstance()
    val tc = Calendar.getInstance().apply { timeInMillis = timestamp }
    val isThisMonth = tc.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && tc.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
    var showPicker by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { Calendar.getInstance().apply { timeInMillis = timestamp; add(Calendar.MONTH, -1) }.let { onSwitchMonth(it.timeInMillis) } }) {
            Icon(Icons.Default.ChevronLeft, "上月", tint = MaterialTheme.colorScheme.primary)
        }
        Text(DateUtils.formatMonth(timestamp), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = { showPicker = true }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text("${cal.get(Calendar.DAY_OF_MONTH)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = onStatsClick) { Icon(Icons.Default.PieChart, "统计", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        IconButton(onClick = { Calendar.getInstance().apply { timeInMillis = timestamp; add(Calendar.MONTH, 1) }.let { onSwitchMonth(it.timeInMillis) } },
            enabled = !isThisMonth) {
            Icon(Icons.Default.ChevronRight, "下月", tint = if (isThisMonth) Color.Transparent else MaterialTheme.colorScheme.primary)
        }
    }

    if (showPicker) {
        val dps = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = { dps.selectedDateMillis?.let { onSwitchMonth(it) }; showPicker = false }) { Text("跳转") }
        }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } }) {
            DatePicker(state = dps)
        }
    }
}

@Composable
fun SummaryCard(income: Double, expense: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SumItem("收入", income, com.soren.bill.ui.theme.IncomeGreen, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
            SumItem("支出", expense, com.soren.bill.ui.theme.ExpenseRed, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
            SumItem("结余", income - expense, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        }
    }
}

@Composable
fun SumItem(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(DateUtils.formatAmount(amount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color))
    }
}

@Composable
fun TxRow(tx: com.soren.bill.data.entity.Transaction, categoryName: String, onClick: () -> Unit) {
    val isExpense = tx.type == "expense"
    val color = if (isExpense) com.soren.bill.ui.theme.ExpenseRed else com.soren.bill.ui.theme.IncomeGreen
    val bgColor = color.copy(alpha = 0.12f)
    
    Surface(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(categoryName), null, Modifier.size(24.dp), tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(DateUtils.formatDisplayDate(tx.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!tx.note.isNullOrBlank()) {
                        Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tx.note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            Text(
                if (isExpense) "-${DateUtils.formatAmount(tx.amount)}" else "+${DateUtils.formatAmount(tx.amount)}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = color)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailDialog(
    tx: com.soren.bill.data.entity.Transaction,
    categoryName: String,
    walletName: String,
    accountName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDel by remember { mutableStateOf(false) }
    val isExpense = tx.type == "expense"
    val color = if (isExpense) com.soren.bill.ui.theme.ExpenseRed else com.soren.bill.ui.theme.IncomeGreen
    val bgColor = color.copy(alpha = 0.12f)
    
    val timeFormat = remember { java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm", java.util.Locale.CHINA) }
    val timeStr = remember(tx.date) { timeFormat.format(java.util.Date(tx.date)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(categoryName), null, Modifier.size(32.dp), tint = color)
            }
            Spacer(Modifier.height(16.dp))
            Text(categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                if (isExpense) "-${DateUtils.formatAmount(tx.amount)}" else "+${DateUtils.formatAmount(tx.amount)}",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = color)
            )
            
            Spacer(Modifier.height(32.dp))
            
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("记录时间", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(timeStr, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("所属钱包", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(walletName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("交易账户", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(accountName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                if (!tx.note.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("备注信息", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(tx.note, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            TextButton(
                onClick = { showDel = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = com.soren.bill.ui.theme.ExpenseRed)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("删除此记录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDel) {
        AlertDialog(
            onDismissRequest = { showDel = false },
            title = { Text("删除") },
            text = { Text("确定删除？") },
            confirmButton = { 
                TextButton(onClick = { onDelete(); showDel = false }) { 
                    Text("删除", color = com.soren.bill.ui.theme.ExpenseRed) 
                } 
            },
            dismissButton = { 
                TextButton(onClick = { showDel = false }) { 
                    Text("取消") 
                } 
            }
        )
    }
}
