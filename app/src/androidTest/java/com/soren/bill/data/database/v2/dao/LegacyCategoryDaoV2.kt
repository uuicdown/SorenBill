package com.soren.bill.data.database.v2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface LegacyCategoryDaoV2 {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: com.soren.bill.data.database.v2.entity.LegacyCategoryV2): Long
}
