package com.soren.bill.data.database.v2

import androidx.room.Database
import androidx.room.RoomDatabase
import com.soren.bill.data.database.v2.dao.LegacyAccountDaoV2
import com.soren.bill.data.database.v2.dao.LegacyCategoryDaoV2
import com.soren.bill.data.database.v2.dao.LegacyTransactionDaoV2
import com.soren.bill.data.database.v2.dao.LegacyWalletDaoV2
import com.soren.bill.data.database.v2.entity.LegacyAccountV2
import com.soren.bill.data.database.v2.entity.LegacyCategoryV2
import com.soren.bill.data.database.v2.entity.LegacyTransactionV2
import com.soren.bill.data.database.v2.entity.LegacyWalletV2

@Database(
    entities = [
        LegacyWalletV2::class,
        LegacyAccountV2::class,
        LegacyCategoryV2::class,
        LegacyTransactionV2::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LegacyBillDatabaseV2 : RoomDatabase() {
    abstract fun walletDao(): LegacyWalletDaoV2
    abstract fun accountDao(): LegacyAccountDaoV2
    abstract fun categoryDao(): LegacyCategoryDaoV2
    abstract fun transactionDao(): LegacyTransactionDaoV2
}
