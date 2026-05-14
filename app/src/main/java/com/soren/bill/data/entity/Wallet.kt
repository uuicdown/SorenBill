package com.soren.bill.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "currency") val currency: String = "CNY",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
