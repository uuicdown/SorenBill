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
    val wallets: List<Wallet> = emptyList(), val selectedWalletId: Long? = 1L,
    val selectedWalletName: String = "日常钱包", val isLoading: Boolean = true
)

class AssetsViewModel(private val repository: BillRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllWallets().collect { wallets ->
                if (wallets.isNotEmpty()) {
                    _uiState.update { it.copy(wallets = wallets, selectedWalletName = wallets.first().name) }
                }
            }
        }
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                updateState(accounts)
            }
        }
        viewModelScope.launch {
            repository.getAllTransactions().collect { _ ->
                // re-trigger on any transaction change
                val accounts = repository.getAllAccounts().first()
                updateState(accounts)
            }
        }
    }

    private suspend fun updateState(accounts: List<Account>) {
        val visible = accounts.filter { !it.isHidden }
        val allTxs = repository.getAllTransactions().first()
        val walletId = _uiState.value.selectedWalletId
        val filteredTxs = if (walletId == null) allTxs else allTxs.filter { it.walletId == walletId }

        val balances = visible.map { account ->
            val atxs = filteredTxs.filter { it.accountId == account.id }
            AccountBalance(account,
                atxs.filter { it.type == "income" }.sumOf { it.amount },
                atxs.filter { it.type == "expense" }.sumOf { it.amount })
        }
        val groups = buildGroups(balances)
        val totalAsset = balances.filter { it.balance >= 0 }.sumOf { it.balance }
        val totalLiability = balances.filter { it.balance < 0 }.sumOf { it.balance }
        _uiState.update {
            it.copy(groups = groups, netAsset = totalAsset + totalLiability,
                totalAsset = totalAsset, totalLiability = totalLiability, isLoading = false)
        }
    }

    fun selectWallet(walletId: Long?) {
        val name = if (walletId == null) "全部钱包" else _uiState.value.wallets.firstOrNull { it.id == walletId }?.name ?: "全部钱包"
        _uiState.update { it.copy(selectedWalletId = walletId, selectedWalletName = name) }
        viewModelScope.launch {
            val accounts = repository.getAllAccounts().first()
            updateState(accounts)
        }
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
        viewModelScope.launch {
            val accountId = repository.insertAccount(Account(name = name, type = type, creditLimit = creditLimit, paymentDueDay = paymentDueDay))
            if (initialBalance != 0.0) {
                // 创建初始余额调整
                val type = if (initialBalance > 0) "income" else "expense"
                val cats = repository.getCategoriesByType(type).first()
                val catId = cats.firstOrNull { it.name == "余额调整" }?.id ?: cats.firstOrNull()?.id ?: 0L
                val walletId = repository.getAllWallets().first().firstOrNull()?.id ?: 1L
                if (catId > 0) {
                    repository.insertTransaction(Transaction(
                        amount = Math.abs(initialBalance), type = type,
                        walletId = walletId, accountId = accountId, categoryId = catId,
                        date = System.currentTimeMillis(), note = "初始余额"
                    ))
                }
            }
        }
    }
    fun updateAccount(account: Account) { viewModelScope.launch { repository.updateAccount(account) } }
    fun deleteAccount(account: Account) { viewModelScope.launch { repository.deleteAccount(account.id) } }
    fun adjustBalance(bal: AccountBalance, newBal: Double) {
        val diff = newBal - bal.balance
        if (diff == 0.0) return
        viewModelScope.launch {
            try {
                val type = if (diff > 0) "income" else "expense"
                val cats = repository.getCategoriesByType(type).first()
                val catId = cats.firstOrNull { it.name == "余额调整" }?.id
                    ?: cats.firstOrNull()?.id ?: 1L
                val walletId = uiState.value.selectedWalletId
                    ?: repository.getAllWallets().first().firstOrNull()?.id ?: 1L
                repository.insertTransaction(Transaction(
                    amount = Math.abs(diff), type = type,
                    walletId = walletId, accountId = bal.account.id,
                    categoryId = catId, date = System.currentTimeMillis(),
                    note = "手动调整余额"
                ))
            } catch (_: Exception) { }
        }
    }
    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AssetsViewModel(repository) as T
    }
}
