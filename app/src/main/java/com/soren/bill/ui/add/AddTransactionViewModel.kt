package com.soren.bill.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.*
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val amount: String = "",
    val type: String = "expense",
    val selectedCategory: Category? = null,
    val selectedAccount: Account? = null,
    val selectedWallet: Wallet? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val lastUsedCategoryId: Long? = null,
    val lastUsedAccountId: Long? = null,
    val lastUsedWalletId: Long? = null,
    val saved: Boolean = false
)

class AddTransactionViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getCategoriesByType("expense").collect { cats ->
                _uiState.update { state ->
                    val selected = if (state.type == "expense") {
                        cats.firstOrNull { it.id == state.lastUsedCategoryId }
                            ?: state.selectedCategory?.takeIf { selected ->
                                cats.any { it.id == selected.id }
                            }
                            ?: cats.firstOrNull()
                    } else {
                        state.selectedCategory
                    }
                    state.copy(expenseCategories = cats, selectedCategory = selected)
                }
            }
        }
        viewModelScope.launch {
            repository.getCategoriesByType("income").collect { cats ->
                _uiState.update { state ->
                    val selected = if (state.type == "income") {
                        cats.firstOrNull { it.id == state.lastUsedCategoryId }
                            ?: state.selectedCategory?.takeIf { selected ->
                                cats.any { it.id == selected.id }
                            }
                            ?: cats.firstOrNull()
                    } else {
                        state.selectedCategory
                    }
                    state.copy(incomeCategories = cats, selectedCategory = selected)
                }
            }
        }
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _uiState.update { state ->
                    val newState = state.copy(accounts = accounts)
                    if (state.selectedAccount == null && accounts.isNotEmpty()) {
                        newState.copy(
                            selectedAccount = accounts.firstOrNull { it.id == state.lastUsedAccountId }
                                ?: accounts.first()
                        )
                    } else newState
                }
            }
        }
        viewModelScope.launch {
            repository.getAllWallets().collect { wallets ->
                _uiState.update { state ->
                    val newState = state.copy(wallets = wallets)
                    if (state.selectedWallet == null && wallets.isNotEmpty()) {
                        newState.copy(
                            selectedWallet = wallets.firstOrNull { it.id == state.lastUsedWalletId }
                                ?: wallets.first()
                        )
                    } else newState
                }
            }
        }
    }

    fun setAmount(value: String) {
        // Only allow valid number input
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amount = value) }
        }
    }

    fun setType(type: String) {
        val categories = if (type == "expense") _uiState.value.expenseCategories
        else _uiState.value.incomeCategories
        _uiState.update {
            it.copy(
                type = type,
                selectedCategory = categories.firstOrNull { cat ->
                    cat.id == it.lastUsedCategoryId
                } ?: categories.firstOrNull()
            )
        }
    }

    fun setCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setAccount(account: Account) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun setWallet(wallet: Wallet) {
        _uiState.update { it.copy(selectedWallet = wallet) }
    }

    fun setDate(timestamp: Long) {
        _uiState.update { it.copy(date = timestamp) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return
        if (amount <= 0) return
        val category = state.selectedCategory ?: return
        val account = state.selectedAccount ?: return
        val wallet = state.selectedWallet ?: return

        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = amount,
                    type = state.type,
                    walletId = wallet.id,
                    accountId = account.id,
                    categoryId = category.id,
                    date = state.date,
                    note = state.note.ifBlank { null }
                )
            )
            _uiState.update {
                it.copy(
                    saved = true,
                    lastUsedCategoryId = category.id,
                    lastUsedAccountId = account.id,
                    lastUsedWalletId = wallet.id
                )
            }
        }
    }

    fun reset() {
        val state = _uiState.value
        _uiState.update {
            AddTransactionUiState(
                type = state.type,
                expenseCategories = state.expenseCategories,
                incomeCategories = state.incomeCategories,
                accounts = state.accounts,
                wallets = state.wallets,
                selectedAccount = state.selectedAccount,
                selectedWallet = state.selectedWallet,
                selectedCategory = state.selectedCategory,
                lastUsedCategoryId = state.lastUsedCategoryId,
                lastUsedAccountId = state.lastUsedAccountId,
                lastUsedWalletId = state.lastUsedWalletId
            )
        }
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(repository) as T
        }
    }
}

