package com.soren.bill.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val holidayMap = mapOf(
    "2026-01-01" to "元旦", "2026-02-17" to "除夕", "2026-02-18" to "春节",
    "2026-05-01" to "劳动节", "2026-05-31" to "端午", "2026-10-01" to "国庆", "2026-10-04" to "中秋"
)

data class CalendarUiState(
    val dailyExpenses: Map<Int, Double> = emptyMap(),
    val dailyIncomes: Map<Int, Double> = emptyMap(),
    val allTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

class CalendarViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadMonth(timestamp: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val monthStart = DateUtils.getMonthStart(timestamp)
                val monthEnd = DateUtils.getMonthEnd(timestamp)
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                repository.getTransactionsByMonth(monthStart, monthEnd).collect { allTxs ->
                    val cal2 = Calendar.getInstance()
                    val expenses = mutableMapOf<Int, Double>()
                    val incomes = mutableMapOf<Int, Double>()
                    for (day in 1..daysInMonth) {
                        cal2.timeInMillis = timestamp
                        cal2.set(Calendar.DAY_OF_MONTH, day)
                        cal2.set(Calendar.HOUR_OF_DAY, 0)
                        cal2.set(Calendar.MINUTE, 0)
                        cal2.set(Calendar.SECOND, 0)
                        cal2.set(Calendar.MILLISECOND, 0)
                        val dayStart = cal2.timeInMillis
                        val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
                        val dayTxs = allTxs.filter { it.date in dayStart..dayEnd }
                        val eSum = dayTxs.filter { it.type == "expense" }.sumOf { it.amount }
                        val iSum = dayTxs.filter { it.type == "income" }.sumOf { it.amount }
                        if (eSum > 0) expenses[day] = eSum
                        if (iSum > 0) incomes[day] = iSum
                    }
                    _uiState.update { it.copy(dailyExpenses = expenses, dailyIncomes = incomes, allTransactions = allTxs, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun getDayCellInfo(dayTimestamp: Long, day: Int, todayYear: Int, todayMonth: Int, today: Int): DayCellInfo {
        val cal = Calendar.getInstance().apply { timeInMillis = dayTimestamp }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val isToday = day == today && cal.get(Calendar.MONTH) == todayMonth && cal.get(Calendar.YEAR) == todayYear
        val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(cal.timeInMillis))
        val holiday = holidayMap[dateStr]
        val expense = _uiState.value.dailyExpenses[day] ?: 0.0
        return DayCellInfo(isToday, isWeekend, holiday, expense, dateStr)
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(repository) as T
        }
    }
}

data class DayCellInfo(
    val isToday: Boolean,
    val isWeekend: Boolean,
    val holiday: String?,
    val expense: Double,
    val dateStr: String
)

