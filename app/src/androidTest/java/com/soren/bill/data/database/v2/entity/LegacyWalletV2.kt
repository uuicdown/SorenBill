package com.soren.bill.data.database.v2.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class LegacyWalletV2(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "currency") val currency: String = "CNY",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0
)
