package com.soren.bill.data.database.v2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface LegacyTransactionDaoV2 {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: com.soren.bill.data.database.v2.entity.LegacyTransactionV2): Long
}
