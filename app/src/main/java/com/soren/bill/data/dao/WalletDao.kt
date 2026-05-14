package com.soren.bill.data.dao

import androidx.room.*
import com.soren.bill.data.entity.Wallet
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY created_at ASC")
    fun getAll(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getById(id: Long): Wallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: Wallet): Long

    @Update
    suspend fun update(wallet: Wallet)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun count(): Int
}
