package com.soren.bill.data.database.v2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface LegacyWalletDaoV2 {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: com.soren.bill.data.database.v2.entity.LegacyWalletV2): Long
}
