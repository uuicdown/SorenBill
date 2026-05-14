package com.soren.bill.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.util.Calendar

data class DayInfo(
    val day: Int,
    val date: Long,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val expense: Double = 0.0,
    val income: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)

    var currentMonth by remember { mutableStateOf(cal.timeInMillis) }
    val displayCal = Calendar.getInstance().apply { timeInMillis = currentMonth }

    val monthLabel = remember(currentMonth) { DateUtils.formatMonth(currentMonth) }
    val daysInMonth = remember(currentMonth) { displayCal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayOfWeek = remember(currentMonth) {
        Calendar.getInstance().apply {
            timeInMillis = currentMonth
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK) - 1
    }

    val days = remember(currentMonth) {
        val list = mutableListOf<DayInfo>()
        if (firstDayOfWeek > 0) {
            for (i in 0 until firstDayOfWeek) {
                list.add(DayInfo(0, 0, false, false))
            }
        }
        for (d in 1..daysInMonth) {
            val dateCal = Calendar.getInstance().apply {
                timeInMillis = currentMonth
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isToday = d == todayDay &&
                    dateCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
                    dateCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
            list.add(DayInfo(d, dateCal.timeInMillis, true, isToday))
        }
        list
    }

    val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    displayCal.add(Calendar.MONTH, -1)
                    currentMonth = displayCal.timeInMillis
                }) {
                    Text("<", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(monthLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    displayCal.add(Calendar.MONTH, 1)
                    currentMonth = displayCal.timeInMillis
                }) {
                    Text(">", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                dayNames.forEach { name ->
                    Text(
                        name,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(days) { day ->
                    CalendarDay(day)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "日均预算: ¥0.00",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendChip("超出日预算", ExpenseRed.copy(alpha = 0.15f), ExpenseRed)
                LegendChip("未超出日预算", IncomeGreen.copy(alpha = 0.15f), IncomeGreen)
            }
        }
    }
}

@Composable
fun CalendarDay(day: DayInfo) {
    if (day.day == 0) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    val bgColor = when {
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val textColor = when {
        day.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${day.day}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (day.expense > 0) {
                Text(
                    DateUtils.formatAmount(day.expense),
                    fontSize = 8.sp,
                    color = ExpenseRed
                )
            }
        }
    }
}

@Composable
fun LegendChip(label: String, bg: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
