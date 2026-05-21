package com.soren.bill.data.repository

import com.soren.bill.data.dao.*
import com.soren.bill.data.entity.*
import kotlinx.coroutines.flow.Flow

class BillRepository(
    private val walletDao: WalletDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    // Wallet
    fun getAllWallets(): Flow<List<Wallet>> = walletDao.getAll()
    suspend fun getWallet(id: Long): Wallet? = walletDao.getById(id)
    suspend fun insertWallet(wallet: Wallet): Long = walletDao.insert(wallet)
    suspend fun updateWallet(wallet: Wallet) = walletDao.update(wallet)
    suspend fun deleteWallet(id: Long) = walletDao.delete(id)

    // Account
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAll()
    suspend fun getAccount(id: Long): Account? = accountDao.getById(id)
    suspend fun insertAccount(account: Account): Long = accountDao.insert(account)
    suspend fun updateAccount(account: Account) = accountDao.update(account)
    suspend fun deleteAccount(id: Long) = accountDao.delete(id)

    // Category
    fun getCategoriesByType(type: String): Flow<List<Category>> = categoryDao.getByType(type)
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAll()
    suspend fun getCategory(id: Long): Category? = categoryDao.getById(id)
    suspend fun insertCategory(category: Category): Long = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(id: Long) = categoryDao.delete(id)

    // Transaction
    fun getTransactionsByMonth(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getByMonth(start, end)

    fun getRecentTransactions(limit: Int = 50): Flow<List<Transaction>> =
        transactionDao.getRecent(limit)

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    fun getFilteredTransactions(
        type: String?,
        categoryId: Long?,
        accountId: Long?,
        walletId: Long?,
        start: Long,
        end: Long
    ): Flow<List<Transaction>> =
        transactionDao.getFiltered(type, categoryId, accountId, walletId, start, end)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.update(transaction)

    suspend fun deleteTransaction(id: Long) = transactionDao.delete(id)

    suspend fun sumByType(type: String, start: Long, end: Long): Double =
        transactionDao.sumByTypeAndDateRange(type, start, end)

    suspend fun sumByCategory(type: String, categoryId: Long, start: Long, end: Long): Double =
        transactionDao.sumByCategoryAndDateRange(type, categoryId, start, end)

    suspend fun getAccountIncome(accountId: Long): Double =
        transactionDao.getAccountIncome(accountId)

    suspend fun getAccountExpense(accountId: Long): Double =
        transactionDao.getAccountExpense(accountId)

    suspend fun getDayExpense(dayStart: Long, dayEnd: Long): Double =
        transactionDao.getDayExpense(dayStart, dayEnd)

    suspend fun getDayIncome(dayStart: Long, dayEnd: Long): Double =
        transactionDao.getDayIncome(dayStart, dayEnd)

    suspend fun getTransactionsForDay(dayStart: Long, dayEnd: Long): List<Transaction> =
        transactionDao.getTransactionsForDay(dayStart, dayEnd)
}
