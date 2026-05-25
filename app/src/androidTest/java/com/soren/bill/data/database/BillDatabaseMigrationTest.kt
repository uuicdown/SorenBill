package com.soren.bill.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.soren.bill.data.database.v2.LegacyBillDatabaseV2
import com.soren.bill.data.database.v2.entity.LegacyCategoryV2
import com.soren.bill.data.database.v2.entity.LegacyWalletV2
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test"

    @Before
    fun setup() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate2To3_addsIsAdjustmentColumnWithDefault0() = runBlocking {
        // Create a legacy v2 database file using a v2 schema (no is_adjustment).
        val legacyDb = Room.databaseBuilder(context, LegacyBillDatabaseV2::class.java, dbName)
            .allowMainThreadQueries()
            .build()

        legacyDb.walletDao().insert(
            LegacyWalletV2(name = "Wallet", currency = "CNY", createdAt = 1L)
        )
        legacyDb.categoryDao().insert(
            LegacyCategoryV2(name = "餐饮", type = "expense", createdAt = 1L)
        )
        legacyDb.close()

        // Migrate to v3.
        val migratedDb = Room.databaseBuilder(context, BillDatabase::class.java, dbName)
            .addMigrations(BillDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        migratedDb.openHelper.writableDatabase

        val cursor = migratedDb.openHelper.readableDatabase.query(
            "PRAGMA table_info(`categories`)"
        )
        val nameIndex = cursor.getColumnIndex("name")
        assertTrue("PRAGMA table_info should include a 'name' column", nameIndex >= 0)

        var hasIsAdjustment = false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == "is_adjustment") {
                hasIsAdjustment = true
                break
            }
        }
        cursor.close()
        assertTrue("Expected categories.is_adjustment column after migration", hasIsAdjustment)

        val valueCursor = migratedDb.openHelper.readableDatabase.query(
            "SELECT is_adjustment FROM categories LIMIT 1"
        )
        assertTrue(valueCursor.moveToFirst())
        val isAdjustment = valueCursor.getInt(0)
        valueCursor.close()
        assertEquals(0, isAdjustment)

        // sanity: ensure db file exists (helps when collecting evidence).
        val dbFile = context.getDatabasePath(dbName)
        assertTrue(dbFile.exists())

        migratedDb.close()
    }
}
