package com.soren.bill.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.Account
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.entity.Wallet
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountBalance(val account: Account, val income: Double, val expense: Double) {
    val balance: Double get() = income - expense
}
data class AccountGroup(val label: String, val accounts: List<AccountBalance>, val totalBalance: Double)

data class AssetsUiState(
    val groups: List<AccountGroup> = emptyList(),
    val netAsset: Double = 0.0, val totalAsset: Double = 0.0, val totalLiability: Double = 0.0,
    val wallets: List<Wallet> = emptyList(), val selectedWalletId: Long? = null,
    val selectedWalletName: String = "全部钱包", val isLoading: Boolean = true
)

class AssetsViewModel(private val repository: BillRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllWallets().collect { wallets ->
                _uiState.update { it.copy(wallets = wallets) }
                if (_uiState.value.selectedWalletId == null && wallets.isNotEmpty())
                    selectWallet(wallets.first().id)
            }
        }
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(repository.getAllAccounts(), repository.getAllTransactions()) { accounts, transactions ->
                buildState(accounts, transactions)
            }.collect { _uiState.update { it } }
        }
    }

    fun selectWallet(walletId: Long?) {
        _uiState.update {
            val name = if (walletId == null) "全部钱包" else it.wallets.firstOrNull { w -> w.id == walletId }?.name ?: "全部钱包"
            it.copy(selectedWalletId = walletId, selectedWalletName = name)
        }
    }

    private fun buildState(accounts: List<Account>, transactions: List<Transaction>): AssetsUiState {
        val walletId = _uiState.value.selectedWalletId
        val filteredTxs = if (walletId == null) transactions else transactions.filter { it.walletId == walletId }
        val visible = accounts.filter { !it.isHidden }
        val balances = visible.map { account ->
            val atxs = filteredTxs.filter { it.accountId == account.id }
            AccountBalance(account, atxs.filter { it.type == "income" }.sumOf { it.amount }, atxs.filter { it.type == "expense" }.sumOf { it.amount })
        }
        val groups = buildGroups(balances)
        val totalAsset = balances.filter { it.balance >= 0 }.sumOf { it.balance }
        val totalLiability = balances.filter { it.balance < 0 }.sumOf { it.balance }
        return _uiState.value.copy(groups = groups, netAsset = totalAsset + totalLiability, totalAsset = totalAsset, totalLiability = totalLiability, isLoading = false)
    }

    private fun buildGroups(balances: List<AccountBalance>): List<AccountGroup> {
        val bankCards = balances.filter { it.account.type == "bank_card" }
        val creditCards = balances.filter { it.account.type == "credit_card" }
        val payments = balances.filter { it.account.type in listOf("wechat", "alipay") }
        val payables = balances.filter { it.account.type == "loan" }
        return listOfNotNull(
            if (bankCards.isNotEmpty()) AccountGroup("储蓄卡", bankCards, bankCards.sumOf { it.balance }) else null,
            if (payments.isNotEmpty()) AccountGroup("网络支付", payments, payments.sumOf { it.balance }) else null,
            if (creditCards.isNotEmpty()) AccountGroup("信用/网贷", creditCards, creditCards.sumOf { it.balance }) else null,
            if (payables.isNotEmpty()) AccountGroup("应付账户", payables, payables.sumOf { it.balance }) else null
        )
    }

    fun addAccount(name: String, type: String, creditLimit: Double, paymentDueDay: Int, initialBalance: Double) {
        viewModelScope.launch { repository.insertAccount(Account(name = name, type = type, creditLimit = creditLimit, paymentDueDay = paymentDueDay)) }
    }
    fun updateAccount(account: Account) { viewModelScope.launch { repository.updateAccount(account) } }
    fun deleteAccount(account: Account) { viewModelScope.launch { repository.deleteAccount(account.id) } }
    fun adjustBalance(bal: AccountBalance, newBal: Double) {
        val diff = newBal - bal.balance
        if (diff == 0.0) return
        viewModelScope.launch {
            val type = if (diff > 0) "income" else "expense"
            val cats = repository.getCategoriesByType(type).first()
            val catId = cats.firstOrNull { it.name == "余额调整" }?.id ?: cats.firstOrNull()?.id ?: return@launch
            val walletId = repository.getAllWallets().first().firstOrNull()?.id ?: return@launch
            repository.insertTransaction(Transaction(amount = Math.abs(diff), type = type, walletId = walletId, accountId = bal.account.id, categoryId = catId, date = System.currentTimeMillis(), note = "手动调整余额"))
        }
    }
    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AssetsViewModel(repository) as T
    }
}
