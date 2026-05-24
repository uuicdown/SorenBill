package com.soren.bill.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.soren.bill.data.database.BillDatabase
import com.soren.bill.data.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var database: BillDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BillDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryTransaction() = runBlocking {
        val tx = Transaction(
            amount = 100.0, type = "expense",
            walletId = 1, accountId = 1, categoryId = 1,
            date = System.currentTimeMillis(), note = "午餐"
        )
        val id = transactionDao.insert(tx)
        assertTrue(id > 0)

        val loaded = transactionDao.getById(id)
        assertNotNull(loaded)
        assertEquals(100.0, loaded!!.amount, 0.001)
        assertEquals("expense", loaded.type)
        assertEquals("午餐", loaded.note)
    }

    @Test
    fun queryByMonth() = runBlocking {
        val now = System.currentTimeMillis()
        transactionDao.insert(Transaction(amount = 50.0, type = "expense", walletId = 1, accountId = 1, categoryId = 1, date = now))
        transactionDao.insert(Transaction(amount = 30.0, type = "income", walletId = 1, accountId = 1, categoryId = 2, date = now))

        val monthStart = 0L
        val monthEnd = Long.MAX_VALUE
        val result = transactionDao.getByMonth(monthStart, monthEnd).first()
        assertEquals(2, result.size)
    }

    @Test
    fun deleteTransaction() = runBlocking {
        val tx = Transaction(amount = 20.0, type = "expense", walletId = 1, accountId = 1, categoryId = 1, date = System.currentTimeMillis())
        val id = transactionDao.insert(tx)
        transactionDao.delete(id)
        val loaded = transactionDao.getById(id)
        assertNull(loaded)
    }

    @Test
    fun sumByTypeAndDateRange() = runBlocking {
        val now = System.currentTimeMillis()
        transactionDao.insert(Transaction(amount = 100.0, type = "expense", walletId = 1, accountId = 1, categoryId = 1, date = now))
        transactionDao.insert(Transaction(amount = 50.0, type = "expense", walletId = 1, accountId = 1, categoryId = 1, date = now))
        transactionDao.insert(Transaction(amount = 200.0, type = "income", walletId = 1, accountId = 1, categoryId = 2, date = now))

        val expenseSum = transactionDao.sumByTypeAndDateRange("expense", 0L, Long.MAX_VALUE)
        assertEquals(150.0, expenseSum, 0.001)

        val incomeSum = transactionDao.sumByTypeAndDateRange("income", 0L, Long.MAX_VALUE)
        assertEquals(200.0, incomeSum, 0.001)
    }

    @Test
    fun getFilteredWithNullParams() = runBlocking {
        val now = System.currentTimeMillis()
        transactionDao.insert(Transaction(amount = 10.0, type = "expense", walletId = 1, accountId = 1, categoryId = 1, date = now))
        transactionDao.insert(Transaction(amount = 20.0, type = "expense", walletId = 1, accountId = 2, categoryId = 1, date = now))
        transactionDao.insert(Transaction(amount = 30.0, type = "expense", walletId = 2, accountId = 1, categoryId = 1, date = now))

        val filtered = transactionDao.getFiltered(
            type = "expense", accountId = 1, start = 0L, end = Long.MAX_VALUE
        ).first()
        assertEquals(2, filtered.size)
    }
}
