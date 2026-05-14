package com.soren.bill.data.dao

import androidx.room.*
import com.soren.bill.data.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY created_at ASC")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}
