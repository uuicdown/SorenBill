package com.soren.bill.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class InsightsUiState(
    val weeklyTransactions: List<Transaction> = emptyList(),
    val weeklyTotal: Double = 0.0,
    val dailyAverages: List<Pair<String, Double>> = emptyList(),
    val currentBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val topCategories: List<Pair<String, Double>> = emptyList(),
    val trend: String = "",
    val isLoading: Boolean = true
)

class InsightsViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init { loadWeeklyData() }

    fun loadWeeklyData() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val weekStart = cal.timeInMillis
                val weekEnd = now

                repository.getTransactionsByMonth(weekStart, weekEnd).collect { txs ->
                    val expenseTxs = txs.filter { it.type == "expense" }
                    val total = expenseTxs.sumOf { it.amount }

                    val dayNames = listOf("\u5468\u4e00","\u5468\u4e8c","\u5468\u4e09","\u5468\u56db","\u5468\u4e94","\u5468\u516d","\u5468\u65e5")
                    val dailyData = mutableListOf<Pair<String, Double>>()
                    for (i in 0..6) {
                        val dayCal = Calendar.getInstance().apply { timeInMillis = weekStart + i * 86400000L }
                        val ds = dayCal.timeInMillis; val de = ds + 86400000L - 1
                        dailyData.add(dayNames[i] to expenseTxs.filter { it.date in ds..de }.sumOf { it.amount })
                    }

                    val categoryGroups = expenseTxs.groupBy { it.note ?: "\u672a\u77e5" }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }
                        .entries.sortedByDescending { it.value }.take(5)
                        .map { it.key to it.value }

                    val first3 = dailyData.take(3).sumOf { it.second }
                    val last4 = dailyData.takeLast(4).sumOf { it.second }
                    val trend = when {
                        last4 > first3 * 1.2 -> "\u4e0a\u5347 \u2197"
                        first3 > last4 * 1.2 -> "\u4e0b\u964d \u2198"
                        else -> "\u5e73\u7a33 \u2192"
                    }

                    _uiState.update {
                        it.copy(weeklyTransactions = expenseTxs, weeklyTotal = total,
                            dailyAverages = dailyData, currentBudget = 5000.0,
                            budgetRemaining = (5000.0 - total).coerceAtLeast(0.0),
                            topCategories = categoryGroups, trend = trend, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
