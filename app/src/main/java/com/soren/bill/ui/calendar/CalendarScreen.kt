package com.soren.bill.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.soren.bill.ui.theme.categoryIcon
import com.soren.bill.util.DateUtils
import java.text.SimpleDateFormat
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

    val cells = remember(current, uiState.dailyExpenses) {
        val list = mutableListOf<Int?>()
        repeat(firstDow) { list.add(null) }
        (1..daysInMonth).forEach { list.add(it) }
        list
    }

    val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // ---- 日历头部 ----
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { displayCal.add(Calendar.MONTH, -1); current = displayCal.timeInMillis; selectedDay = null }) {
                Icon(Icons.Default.ChevronLeft, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(monthLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { displayCal.add(Calendar.MONTH, 1); current = displayCal.timeInMillis; selectedDay = null }) {
                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        // ---- 星期标题 ----
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            dayNames.forEachIndexed { i, n ->
                val wk = i == 0 || i == 6
                Text(n, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                    color = if (wk) ExpenseRed.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(4.dp))

        // ---- 日历格子 ----
        val screenWidth = 360
        val cellSize = (screenWidth - 8) / 7
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
                            val isSelected = selectedDay == day
                            val bg = when {
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                info.isToday -> MaterialTheme.colorScheme.primary
                                else -> Color.Transparent
                            }
                            val tc = when {
                                info.isToday && !isSelected -> Color.White
                                info.holiday != null -> ExpenseRed
                                info.isWeekend -> ExpenseRed.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(Modifier.fillMaxSize().padding(2.dp)
                                .clip(if (info.isToday) CircleShape else RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable {
                                    dayCal.set(Calendar.HOUR_OF_DAY, 0); dayCal.set(Calendar.MINUTE, 0)
                                    selectedDate = dayCal.timeInMillis
                                    selectedDay = if (selectedDay == day) null else day // 再次点击取消选中
                                },
                                contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$day", fontSize = 13.sp, fontWeight = if (info.isToday || isSelected) FontWeight.Bold else FontWeight.Normal, color = tc)
                                    if (info.holiday != null) Text(info.holiday, fontSize = 8.sp, color = ExpenseRed, maxLines = 1, fontWeight = FontWeight.Medium)
                                    else if (info.expense > 0) Text(DateUtils.formatAmount(info.expense), fontSize = 7.sp, color = if (info.isToday && !isSelected) Color.White.copy(alpha = 0.7f) else ExpenseRed, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                val remaining = 7 - (end - start)
                for (i in 0 until remaining) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))

        // ---- 选中日的流水列表 ----
        if (selectedDay != null) {
            val dayStart = Calendar.getInstance().apply { timeInMillis = selectedDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
            val dayEnd = dayStart + 24L * 60 * 60 * 1000 - 1
            val txs = uiState.allTransactions.filter { it.date in dayStart..dayEnd }
            val dateLabel = remember(selectedDate) {
                SimpleDateFormat("MM月dd日", Locale.CHINA).format(Date(selectedDate))
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📋 $dateLabel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { selectedDay = null }) {
                    Text("收起", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))

            if (txs.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ReceiptLong, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))
                    Text("这天没有记录呢～", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    txs.forEach { tx ->
                        val isExpense = tx.type == "expense"
                        val amountColor = if (isExpense) ExpenseRed else IncomeGreen
                        val bgColor = amountColor.copy(alpha = 0.08f)

                        Surface(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent
                        ) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(categoryIcon("餐饮"), null, Modifier.size(18.dp), tint = amountColor)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(tx.note ?: "未备注", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isExpense) "-${DateUtils.formatAmount(tx.amount)}" else "+${DateUtils.formatAmount(tx.amount)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = amountColor)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            // 未选中时显示日统计摘要
            Spacer(Modifier.height(12.dp))
            Text("日预览: ¥0.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(6.dp), color = ExpenseRed.copy(alpha = 0.1f)) {
                    Text("点选日期查看流水", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}