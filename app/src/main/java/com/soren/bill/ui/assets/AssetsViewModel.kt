package com.soren.bill.ui.assets

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
    val income: Double,
    val expense: Double
) {
    val balance: Double get() = income - expense
}

data class AccountGroup(
    val label: String,
    val accounts: List<AccountBalance>,
    val totalBalance: Double
)

data class AssetsUiState(
    val groups: List<AccountGroup> = emptyList(),
    val netAsset: Double = 0.0,
    val totalAsset: Double = 0.0,
    val totalLiability: Double = 0.0,
    val isLoading: Boolean = true
)

class AssetsViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
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

    private fun buildState(accounts: List<Account>, transactions: List<Transaction>): AssetsUiState {
        val visible = accounts.filter { !it.isHidden }
        val balances = visible.map { account ->
            val accountTransactions = transactions.filter { it.accountId == account.id }
            val income = accountTransactions
                .filter { it.type == "income" }
                .sumOf { it.amount }
            val expense = accountTransactions
                .filter { it.type == "expense" }
                .sumOf { it.amount }
            AccountBalance(account, income, expense)
        }

        val groups = buildGroups(balances)
        val totalAsset = balances.filter { it.balance >= 0 }.sumOf { it.balance }
        val totalLiability = balances.filter { it.balance < 0 }.sumOf { it.balance }
        val netAsset = totalAsset + totalLiability

        return AssetsUiState(
            groups = groups,
            netAsset = netAsset,
            totalAsset = totalAsset,
            totalLiability = totalLiability,
            isLoading = false
        )
    }

    private fun buildGroups(balances: List<AccountBalance>): List<AccountGroup> {
        val bankCards = balances.filter { it.account.type == "bank_card" }
        val creditCards = balances.filter { it.account.type == "credit_card" }
        val payments = balances.filter { it.account.type in listOf("wechat", "alipay") }
        val payables = balances.filter { it.account.type == "loan" }

        return listOfNotNull(
            if (bankCards.isNotEmpty()) AccountGroup("储蓄卡", bankCards, bankCards.sumOf { it.balance }) else null,
            if (creditCards.isNotEmpty()) AccountGroup("信用卡", creditCards, creditCards.sumOf { it.balance }) else null,
            if (payments.isNotEmpty()) AccountGroup("网络支付账户", payments, payments.sumOf { it.balance }) else null,
            if (payables.isNotEmpty()) AccountGroup("应付账户", payables, payables.sumOf { it.balance }) else null
        )
    }

    fun addAccount(name: String, type: String, creditLimit: Double, paymentDueDay: Int) {
        viewModelScope.launch {
            repository.insertAccount(
                Account(name = name, type = type, creditLimit = creditLimit, paymentDueDay = paymentDueDay)
            )
        }
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AssetsViewModel(repository) as T
        }
    }
}
