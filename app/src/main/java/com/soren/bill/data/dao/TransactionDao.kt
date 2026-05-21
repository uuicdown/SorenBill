package com.soren.bill.data.dao

import androidx.room.*
import com.soren.bill.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startOfMonth AND :endOfMonth 
        ORDER BY date DESC, created_at DESC
    """)
    fun getByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = :type AND date BETWEEN :start AND :end
    """)
    suspend fun sumByTypeAndDateRange(type: String, start: Long, end: Long): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = :type AND category_id = :categoryId 
        AND date BETWEEN :start AND :end
    """)
    suspend fun sumByCategoryAndDateRange(
        type: String, categoryId: Long, start: Long, end: Long
    ): Double

    @Query("""
        SELECT * FROM transactions 
        WHERE (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR category_id = :categoryId)
        AND (:accountId IS NULL OR account_id = :accountId)
        AND (:walletId IS NULL OR wallet_id = :walletId)
        AND date BETWEEN :start AND :end
        ORDER BY date DESC, created_at DESC
    """)
    fun getFiltered(
        type: String? = null,
        categoryId: Long? = null,
        accountId: Long? = null,
        walletId: Long? = null,
        start: Long,
        end: Long
    ): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions WHERE account_id = :accountId AND type = 'income'
    """)
    suspend fun getAccountIncome(accountId: Long): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions WHERE account_id = :accountId AND type = 'expense'
    """)
    suspend fun getAccountExpense(accountId: Long): Double

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0)
        FROM transactions
        WHERE date BETWEEN :dayStart AND :dayEnd
    """)
    suspend fun getDayExpense(dayStart: Long, dayEnd: Long): Double

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0)
        FROM transactions
        WHERE date BETWEEN :dayStart AND :dayEnd
    """)
    suspend fun getDayIncome(dayStart: Long, dayEnd: Long): Double

    @Query("""
        SELECT * FROM transactions
        WHERE date BETWEEN :dayStart AND :dayEnd
        ORDER BY date DESC, created_at DESC
    """)
    suspend fun getTransactionsForDay(dayStart: Long, dayEnd: Long): List<Transaction>
}
