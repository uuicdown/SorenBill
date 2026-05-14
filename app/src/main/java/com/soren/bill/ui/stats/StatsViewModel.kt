package com.soren.bill.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Category
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryStat(
    val category: Category,
    val amount: Double,
    val percentage: Float = 0f
)

data class StatsUiState(
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val expenseBreakdown: List<CategoryStat> = emptyList(),
    val incomeBreakdown: List<CategoryStat> = emptyList(),
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val monthStart = DateUtils.getMonthStart()
    private val monthEnd = DateUtils.getMonthEnd()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            combine(
                repository.getTransactionsByMonth(monthStart, monthEnd),
                repository.getCategoriesByType("expense"),
                repository.getCategoriesByType("income")
            ) { transactions, expenseCategories, incomeCategories ->
                buildState(transactions, expenseCategories, incomeCategories)
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    private fun buildState(
        transactions: List<Transaction>,
        expenseCategories: List<Category>,
        incomeCategories: List<Category>
    ): StatsUiState {
        val expense = transactions
            .filter { it.type == "expense" }
            .sumOf { it.amount }
        val income = transactions
            .filter { it.type == "income" }
            .sumOf { it.amount }

        val expenseBreakdown = expenseCategories.mapNotNull { category ->
            val sum = transactions
                .filter { it.type == "expense" && it.categoryId == category.id }
                .sumOf { it.amount }
            if (sum > 0) {
                CategoryStat(category, sum, if (expense > 0) (sum / expense).toFloat() else 0f)
            } else {
                null
            }
        }.sortedByDescending { it.amount }

        val incomeBreakdown = incomeCategories.mapNotNull { category ->
            val sum = transactions
                .filter { it.type == "income" && it.categoryId == category.id }
                .sumOf { it.amount }
            if (sum > 0) {
                CategoryStat(category, sum, if (income > 0) (sum / income).toFloat() else 0f)
            } else {
                null
            }
        }.sortedByDescending { it.amount }

        return StatsUiState(
            monthlyExpense = expense,
            monthlyIncome = income,
            expenseBreakdown = expenseBreakdown,
            incomeBreakdown = incomeBreakdown,
            isLoading = false
        )
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(repository) as T
        }
    }
}
