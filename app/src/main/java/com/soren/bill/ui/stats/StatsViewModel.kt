package com.soren.bill.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Category
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryStat(
    val category: Category, val amount: Double, val percentage: Float = 0f
)

data class StatsUiState(
    val monthlyExpense: Double = 0.0, val monthlyIncome: Double = 0.0,
    val expenseBreakdown: List<CategoryStat> = emptyList(),
    val incomeBreakdown: List<CategoryStat> = emptyList(),
    val currentMonthTimestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { switchMonth(System.currentTimeMillis()) }

    fun switchMonth(timestamp: Long) {
        val monthStart = DateUtils.getMonthStart(timestamp)
        val monthEnd = DateUtils.getMonthEnd(timestamp)
        _uiState.update { it.copy(currentMonthTimestamp = timestamp, isLoading = true) }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                repository.getTransactionsByMonth(monthStart, monthEnd),
                repository.getCategoriesByType("expense"),
                repository.getCategoriesByType("income")
            ) { txs, expCats, incCats -> Triple(txs, expCats, incCats) }
                .collect { (txs, expCats, incCats) ->
                    _uiState.update { buildState(it, txs, expCats, incCats, timestamp) }
                }
        }
    }

    private fun buildState(
        prev: StatsUiState, txs: List<Transaction>,
        expCats: List<Category>, incCats: List<Category>, ts: Long
    ): StatsUiState {
        val adjIds = (expCats + incCats).filter { it.isAdjustment }.map { it.id }.toSet()
        val realTxs = txs.filter { it.categoryId !in adjIds }
        val expense = realTxs.filter { it.type == "expense" }.sumOf { it.amount }
        val income = realTxs.filter { it.type == "income" }.sumOf { it.amount }
        val expBreak = expCats.mapNotNull { c ->
            val s = realTxs.filter { it.type == "expense" && it.categoryId == c.id }.sumOf { it.amount }
            if (s > 0) CategoryStat(c, s, if (expense > 0) (s / expense).toFloat() else 0f) else null
        }.sortedByDescending { it.amount }
        val incBreak = incCats.mapNotNull { c ->
            val s = realTxs.filter { it.type == "income" && it.categoryId == c.id }.sumOf { it.amount }
            if (s > 0) CategoryStat(c, s, if (income > 0) (s / income).toFloat() else 0f) else null
        }.sortedByDescending { it.amount }
        return prev.copy(monthlyExpense = expense, monthlyIncome = income,
            expenseBreakdown = expBreak, incomeBreakdown = incBreak,
            currentMonthTimestamp = ts, isLoading = false)
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(repository) as T
        }
    }
}

