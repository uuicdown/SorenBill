package com.soren.bill.ui.stats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.IncomeGreen
import com.soren.bill.util.DateUtils
import java.util.Calendar

private val pieColors = listOf(
    Color(0xFFE74C3C), Color(0xFF3498DB), Color(0xFF2ECC71),
    Color(0xFFF39C12), Color(0xFF9B59B6), Color(0xFF1ABC9C),
    Color(0xFFE67E22), Color(0xFF34495E), Color(0xFF95A5A6)
)

@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部栏：返回 + 月份切换
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = uiState.currentMonthTimestamp }
                    cal.add(Calendar.MONTH, -1)
                    viewModel.switchMonth(cal.timeInMillis)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上月")
                }
                Text(DateUtils.formatMonth(uiState.currentMonthTimestamp),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = uiState.currentMonthTimestamp }
                    cal.add(Calendar.MONTH, 1)
                    viewModel.switchMonth(cal.timeInMillis)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下月")
                }
                Spacer(Modifier.width(48.dp)) // balance with back button
            }
        }

        if (uiState.isLoading) {
            item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            } }
        } else {
            item {
                BigNumbersCard(expense = uiState.monthlyExpense, income = uiState.monthlyIncome)
            }

            if (uiState.expenseBreakdown.isNotEmpty()) {
                item {
                    Text("支出分布", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                item {
                    PieChartCard(uiState.expenseBreakdown, uiState.monthlyExpense)
                }
            }

            if (uiState.incomeBreakdown.isNotEmpty()) {
                item {
                    Text("收入来源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                item {
                    PieChartCard(uiState.incomeBreakdown, uiState.monthlyIncome)
                }
            }

            if (uiState.expenseBreakdown.isEmpty() && uiState.incomeBreakdown.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PieChart, null, Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                        Spacer(Modifier.height(12.dp))
                        Text("本月暂无收支数据", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun BigNumbersCard(expense: Double, income: Double) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn("本月支出", expense, ExpenseRed)
            StatColumn("本月收入", income, IncomeGreen)
            StatColumn("净结余", income - expense, if (income - expense >= 0) IncomeGreen else ExpenseRed)
        }
    }
}

@Composable
fun StatColumn(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            DateUtils.formatAmount(amount),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
        )
    }
}

@Composable
fun PieChartCard(breakdown: List<CategoryStat>, totalAmount: Double) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    breakdown.forEachIndexed { index, stat ->
                        val sweep = stat.percentage * 360f
                        drawArc(
                            color = pieColors[index % pieColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            size = Size(size.width, size.height)
                        )
                        startAngle += sweep
                    }
                    drawCircle(
                        color = Color.White,
                        radius = size.width * 0.35f
                    )
                }
                Text(
                    DateUtils.formatAmount(totalAmount),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                breakdown.take(6).forEachIndexed { index, stat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(pieColors[index % pieColors.size])
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stat.category.name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${(stat.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
