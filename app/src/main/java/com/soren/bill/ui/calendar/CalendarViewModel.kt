package com.soren.bill.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class CalendarUiState(
    val dailyExpenses: Map<Int, Double> = emptyMap(),
    val dailyIncomes: Map<Int, Double> = emptyMap(),
    val allTransactions: List<Transaction> = emptyList(),
    val selectedDayTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

class CalendarViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    fun loadMonth(timestamp: Long) {
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val monthStart = DateUtils.getMonthStart(timestamp)
                val monthEnd = DateUtils.getMonthEnd(timestamp)

                // Load all transactions for the month
                val allTxs = repository.getTransactionsByMonth(monthStart, monthEnd).first()

                // Build daily maps
                val expenses = mutableMapOf<Int, Double>()
                val incomes = mutableMapOf<Int, Double>()
                for (day in 1..daysInMonth) {
                    val dayStart = Calendar.getInstance().apply {
                        timeInMillis = timestamp
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
                    val dayTxs = allTxs.filter { it.date in dayStart..dayEnd }
                    val eSum = dayTxs.filter { it.type == "expense" }.sumOf { it.amount }
                    val iSum = dayTxs.filter { it.type == "income" }.sumOf { it.amount }
                    
                    if (eSum > 0) expenses[day] = eSum
                    if (iSum > 0) incomes[day] = iSum
                }
                _uiState.update { it.copy(dailyExpenses = expenses, dailyIncomes = incomes, allTransactions = allTxs, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectDay(dayStart: Long) {
        try {
            val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
            val txs = _uiState.value.allTransactions.filter { it.date in dayStart..dayEnd }
            _uiState.update { it.copy(selectedDayTransactions = txs) }
        } catch (e: Exception) {
            _uiState.update { it.copy(selectedDayTransactions = emptyList()) }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedDayTransactions = emptyList()) }
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(repository) as T
        }
    }
}
