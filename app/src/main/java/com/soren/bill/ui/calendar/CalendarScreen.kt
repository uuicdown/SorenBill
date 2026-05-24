package com.soren.bill.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.IncomeGreen
import com.soren.bill.util.DateUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val todayYear = cal.get(Calendar.YEAR)
    val todayMonth = cal.get(Calendar.MONTH)

    var current by remember { mutableStateOf(cal.timeInMillis) }
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(current) { viewModel.loadMonth(current) }

    val monthLabel = remember(current) { DateUtils.formatMonth(current) }
    val displayCal = remember(current) { Calendar.getInstance().apply { timeInMillis = current } }
    val daysInMonth = remember(current) { displayCal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDow = remember(current) {
        Calendar.getInstance().apply { timeInMillis = current; set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    }

    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf(0L) }

    // Build day cells as flat list with padding
    val cells = remember(current, uiState.dailyExpenses) {
        val list = mutableListOf<Int?>() // null = empty cell, Int = day number
        repeat(firstDow) { list.add(null) }
        (1..daysInMonth).forEach { list.add(it) }
        list
    }

    val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { displayCal.add(Calendar.MONTH, -1); current = displayCal.timeInMillis }) { Text("◀", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary) }
            Text(monthLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { displayCal.add(Calendar.MONTH, 1); current = displayCal.timeInMillis }) { Text("▶", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary) }
        }

        // Day names
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            dayNames.forEachIndexed { i, n ->
                val wk = i == 0 || i == 6
                Text(n, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                    color = if (wk) ExpenseRed.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(4.dp))

        // Calendar rows (max 6 rows) - use fixed height cells
        val screenWidth = 360 // approximate dp
        val cellSize = (screenWidth - 8) / 7 // 4dp padding each side
        for (row in 0..5) {
            val start = row * 7
            val end = minOf(start + 7, cells.size)
            if (start >= cells.size) break
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(cellSize.dp)) {
                for (i in start until end) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        val day = cells.getOrNull(i)
                        if (day != null) {
                            val dayCal = Calendar.getInstance().apply { timeInMillis = current; set(Calendar.DAY_OF_MONTH, day) }
                            val info = viewModel.getDayCellInfo(dayCal.timeInMillis, day, todayYear, todayMonth, today)
                            val bg = if (info.isToday) MaterialTheme.colorScheme.primary else Color.Transparent
                            val tc = when { info.isToday -> Color.White; info.holiday != null -> ExpenseRed; info.isWeekend -> ExpenseRed.copy(alpha = 0.5f); else -> MaterialTheme.colorScheme.onSurface }

                            Box(Modifier.fillMaxSize().padding(2.dp).clip(if (info.isToday) CircleShape else RoundedCornerShape(8.dp))
                                .background(bg).clickable { dayCal.set(Calendar.HOUR_OF_DAY, 0); dayCal.set(Calendar.MINUTE, 0); selectedDate = dayCal.timeInMillis; selectedDay = day },
                                contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$day", fontSize = 13.sp, fontWeight = if (info.isToday) FontWeight.Bold else FontWeight.Normal, color = tc)
                                    if (info.holiday != null) Text(info.holiday, fontSize = 8.sp, color = ExpenseRed, maxLines = 1, fontWeight = FontWeight.Medium)
                                    else if (info.expense > 0) Text(DateUtils.formatAmount(info.expense), fontSize = 7.sp, color = if (info.isToday) Color.White.copy(alpha = 0.7f) else ExpenseRed, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        Text("日均预算: ¥0.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(6.dp), color = ExpenseRed.copy(alpha = 0.1f)) {
                Text("超出预算", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = IncomeGreen.copy(alpha = 0.1f)) {
                Text("未超预算", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Day detail dialog
    selectedDay?.let { day ->
        val dayStart = Calendar.getInstance().apply { timeInMillis = selectedDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
        val dayEnd = dayStart + 24L * 60 * 60 * 1000 - 1
        val txs = uiState.allTransactions.filter { it.date in dayStart..dayEnd }
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text(DateUtils.formatMonth(selectedDate) + " ${day}日", fontWeight = FontWeight.Bold) },
            text = {
                if (txs.isEmpty()) Text("当天没有记录", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                else Column { txs.take(10).forEach { tx ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tx.note ?: "未备注", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(DateUtils.formatAmount(tx.amount), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold,
                            color = if (tx.type == "expense") ExpenseRed else IncomeGreen))
                    }
                }}
            },
            confirmButton = { TextButton(onClick = { selectedDay = null }) { Text("关闭") } }
        )
    }
}
