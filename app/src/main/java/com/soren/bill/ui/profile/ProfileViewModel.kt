package com.soren.bill.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soren.bill.data.entity.*
import com.soren.bill.data.repository.BillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val wallets: List<Wallet> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList()
)

class ProfileViewModel(
    private val repository: BillRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllWallets().collect { wallets ->
                _uiState.update { it.copy(wallets = wallets) }
            }
        }
        viewModelScope.launch {
            repository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
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
    }

    fun addWallet(name: String, currency: String = "CNY") {
        viewModelScope.launch {
            repository.insertWallet(Wallet(name = name, currency = currency))
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch { repository.deleteWallet(wallet.id) }
    }

    fun updateWallet(wallet: Wallet) {
        viewModelScope.launch { repository.updateWallet(wallet) }
    }

    fun addAccount(name: String, type: String) {
        viewModelScope.launch {
            repository.insertAccount(Account(name = name, type = type))
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch { repository.deleteAccount(account.id) }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch { repository.updateAccount(account) }
    }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, type = type))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category.id) }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(repository) as T
        }
    }
}
