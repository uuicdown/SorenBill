package com.soren.bill.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Account
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountBalance(
    val account: Account,
    val income: Double = 0.0,
    val expense: Double = 0.0
) {
    val balance: Double get() = income - expense
}

data class WalletUiState(
    val accountBalances: List<AccountBalance> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = true
)

class WalletViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllAccounts(),
                repository.getAllTransactions()
            ) { accounts, transactions ->
                buildState(accounts, transactions)
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    private fun buildState(accounts: List<Account>, transactions: List<Transaction>): WalletUiState {
        val balances = accounts.map { account ->
            val accountTransactions = transactions.filter { it.accountId == account.id }
            val income = accountTransactions
                .filter { it.type == "income" }
                .sumOf { it.amount }
            val expense = accountTransactions
                .filter { it.type == "expense" }
                .sumOf { it.amount }
            AccountBalance(account, income, expense)
        }
        val totalIncome = balances.sumOf { it.income }
        val totalExpense = balances.sumOf { it.expense }

        return WalletUiState(
            accountBalances = balances,
            totalBalance = totalIncome - totalExpense,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            isLoading = false
        )
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletViewModel(repository) as T
        }
    }
}
