package com.soren.bill.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.*
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val currentMonthTimestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true
) {
    val categoryMap: Map<Long, String>
        get() = (expenseCategories + incomeCategories).associate { it.id to it.name }
}

class HomeViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var transactionJob: Job? = null

    init {
        loadMasterData()
        switchMonth(System.currentTimeMillis())
    }

    private fun loadMasterData() {
        viewModelScope.launch {
            repository.getCategoriesByType("expense").collect { cats ->
                _uiState.update { it.copy(expenseCategories = cats) }
            }
        }
        viewModelScope.launch {
            repository.getCategoriesByType("income").collect { cats ->
                _uiState.update { it.copy(incomeCategories = cats) }
            }
        }
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        viewModelScope.launch {
            repository.getAllWallets().collect { wallets ->
                _uiState.update { it.copy(wallets = wallets) }
            }
        }
    }

    fun switchMonth(timestamp: Long) {
        val monthStart = DateUtils.getMonthStart(timestamp)
        val monthEnd = DateUtils.getMonthEnd(timestamp)
        _uiState.update { it.copy(currentMonthTimestamp = timestamp, isLoading = true) }

        transactionJob?.cancel()
        transactionJob = viewModelScope.launch {
            repository.getTransactionsByMonth(monthStart, monthEnd).collect { transactions ->
                _uiState.update { it.copy(transactions = transactions, isLoading = false) }
                refreshSummary(monthStart, monthEnd)
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(transaction.id) }
    }

    private suspend fun refreshSummary(monthStart: Long, monthEnd: Long) {
        val expense = repository.sumByType("expense", monthStart, monthEnd)
        val income = repository.sumByType("income", monthStart, monthEnd)
        _uiState.update { it.copy(monthlyExpense = expense, monthlyIncome = income) }
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
