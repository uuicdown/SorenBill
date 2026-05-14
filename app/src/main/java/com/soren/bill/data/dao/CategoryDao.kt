package com.soren.bill.data.dao

import androidx.room.*
import com.soren.bill.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY created_at ASC")
    fun getByType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY type, created_at ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM categories WHERE type = :type")
    suspend fun countByType(type: String): Int
}
