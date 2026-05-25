package com.soren.bill.ui.insights

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soren.bill.ui.theme.ExpenseRed
import com.soren.bill.ui.theme.IncomeGreen
import com.soren.bill.util.DateUtils

@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text("Soren \u7684\u4e00\u5468\u5206\u6790", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("${DateUtils.formatMonth(System.currentTimeMillis())} \u7b2c\u4e09\u5468", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // 本周总支出卡片
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\u672c\u5468\u82b1\u9500", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(DateUtils.formatAmount(uiState.weeklyTotal), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 36.sp, color = ExpenseRed))
                Spacer(Modifier.height(4.dp))
                Text("\u8d8b\u52bf\uff1a${uiState.trend}", style = MaterialTheme.typography.bodyMedium, color = ExpenseRed)
            }
        }

        // 预算剩余
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(0.dp)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("\u672c\u6708\u9884\u7b97\u5269\u4f59", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(DateUtils.formatAmount(uiState.budgetRemaining), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = IncomeGreen))
                }
                TextButton(onClick = { /* TODO: \u8bbe\u7f6e\u9884\u7b97 */ }) {
                    Text("\u8bbe\u7f6e\u9884\u7b97 \u2192", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // 每日柱状简图
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("\u6bcf\u65e5\u82b1\u9500", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                val maxAmount = uiState.dailyAverages.maxOfOrNull { it.second } ?: 1.0
                uiState.dailyAverages.forEach { (day, amount) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(day, Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val barWidth = if (maxAmount > 0) (amount / maxAmount * 200).dp else 0.dp
                        Box(Modifier.height(14.dp).width(barWidth.coerceAtLeast(2.dp)).clip(RoundedCornerShape(4.dp)).background(ExpenseRed.copy(alpha = 0.7f)))
                        Spacer(Modifier.weight(1f))
                        Text(if (amount > 0) DateUtils.formatAmount(amount) else "\u2014", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // TOP 支出分类
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("\u672c\u5468 TOP \u82b1\u9500", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (uiState.topCategories.isEmpty()) {
                    Text("\u8fd8\u6ca1\u6709\u8bb0\u5f55\u5462\uff5e", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    uiState.topCategories.forEachIndexed { idx, (name, amount) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(DateUtils.formatAmount(amount), style = MaterialTheme.typography.bodyMedium.copy(color = ExpenseRed, fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // Soren 点评
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
            elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("💬 Soren \u7684\u5efa\u8bae", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                val tip = when {
                    uiState.weeklyTotal > 3000 -> "\u672c\u5468\u82b1\u9500\u504f\u9ad8\u54e6\uff0c\u53ef\u4ee5\u7559\u610f\u4e00\u4e0b\u975e\u5fc5\u8981\u5f00\u652f\uff5e Soren \u5efa\u8bae\u4e0b\u5468\u63a7\u5236\u5728 \u00a52,000 \u4ee5\u5185\u3002"
                    uiState.weeklyTotal > 1500 -> "\u6d88\u8d39\u8282\u594f\u6b63\u5e38\uff0c\u8ddf\u4e0a\u5468\u5dee\u4e0d\u591a\u3002\u4fdd\u6301\u5c31\u597d\uff5e"
                    uiState.weeklyTotal > 0 -> "\u672c\u5468\u82b1\u5f97\u5f88\u8282\u5236\u5462\uff01Soren \u4e3a\u60a8\u611f\u5230\u5f00\u5fc3 😊"
                    else -> "\u8fd9\u5468\u8fd8\u6ca1\u6709\u8bb0\u5f55\uff0c\u5feb\u53bb\u8bb0\u4e00\u7b14\u5427\uff5e"
                }
                Text(tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
