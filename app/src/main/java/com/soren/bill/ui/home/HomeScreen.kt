package com.soren.bill.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@Composable
fun HomeScreen(viewModel: HomeViewModel, onAddClick: () -> Unit, onStatsClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HomeMonthHeader(
                timestamp = uiState.currentMonthTimestamp,
                onSwitchMonth = { viewModel.switchMonth(it) },
                onStatsClick = onStatsClick
            )
            SummaryCard(income = uiState.monthlyIncome, expense = uiState.monthlyExpense)
            if (uiState.isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else if (uiState.transactions.isEmpty()) Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.EditCalendar, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text("还没有记录", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("点右下角 + 记一笔", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TxRow(tx, uiState.categoryMap[tx.categoryId] ?: "未分类") { viewModel.deleteTransaction(tx) }
                }
            }
        }
        FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "记一笔") }
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
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            SumItem("收入", income, com.soren.bill.ui.theme.IncomeGreen)
            Box(Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)))
            SumItem("支出", expense, com.soren.bill.ui.theme.ExpenseRed)
            Box(Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)))
            SumItem("结余", income - expense, if (income - expense >= 0) com.soren.bill.ui.theme.IncomeGreen else com.soren.bill.ui.theme.ExpenseRed)
        }
    }
}

@Composable
fun SumItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(DateUtils.formatAmount(amount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color))
    }
}

@Composable
fun TxRow(tx: com.soren.bill.data.entity.Transaction, categoryName: String, onDelete: () -> Unit) {
    var showDel by remember { mutableStateOf(false) }
    val color = if (tx.type == "expense") com.soren.bill.ui.theme.ExpenseRed else com.soren.bill.ui.theme.IncomeGreen
    Surface(Modifier.fillMaxWidth().clickable { showDel = true }, shape = RoundedCornerShape(10.dp), color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(categoryIcon(categoryName), null, Modifier.size(16.dp), tint = color.copy(alpha = 0.7f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (!tx.note.isNullOrBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(DateUtils.formatDisplayDate(tx.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
            }
            Text(if (tx.type == "expense") "-${DateUtils.formatAmount(tx.amount)}" else "+${DateUtils.formatAmount(tx.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = color))
        }
    }
    if (showDel) AlertDialog(onDismissRequest = { showDel = false }, title = { Text("删除") }, text = { Text("确定删除？") },
        confirmButton = { TextButton(onClick = { onDelete(); showDel = false }) { Text("删除", color = com.soren.bill.ui.theme.ExpenseRed) } },
        dismissButton = { TextButton(onClick = { showDel = false }) { Text("取消") } })
}
