package com.soren.bill.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.soren.bill.data.dao.AccountDao
import com.soren.bill.data.dao.CategoryDao
import com.soren.bill.data.dao.TransactionDao
import com.soren.bill.data.dao.WalletDao
import com.soren.bill.data.entity.Account
import com.soren.bill.data.entity.Category
import com.soren.bill.data.entity.Transaction
import com.soren.bill.data.entity.Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Wallet::class, Account::class, Category::class, Transaction::class],
    version = 2,
    exportSchema = false
)
abstract class BillDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: BillDatabase? = null

        fun getInstance(context: Context): BillDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): BillDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BillDatabase::class.java,
                "soren_bill.db"
            )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).seedDefaults()
                        }
                    }
                })
                .build()
        }
    }

    private suspend fun seedDefaults() {
        val walletDao = walletDao()
        val accountDao = accountDao()
        val categoryDao = categoryDao()

        if (walletDao.count() == 0) {
            walletDao.insert(Wallet(name = "日常钱包", currency = "CNY"))
        }

        if (accountDao.count() == 0) {
            accountDao.insert(Account(name = "微信", type = "wechat"))
            accountDao.insert(Account(name = "支付宝", type = "alipay"))
            accountDao.insert(Account(name = "银行卡", type = "bank_card"))
            accountDao.insert(Account(name = "信用卡", type = "credit_card"))
            accountDao.insert(Account(name = "现金", type = "cash"))
        }

        if (categoryDao.countByType("expense") == 0) {
            categoryDao.insert(Category(name = "餐饮", type = "expense"))
            categoryDao.insert(Category(name = "交通", type = "expense"))
            categoryDao.insert(Category(name = "购物", type = "expense"))
            categoryDao.insert(Category(name = "娱乐", type = "expense"))
            categoryDao.insert(Category(name = "居住", type = "expense"))
            categoryDao.insert(Category(name = "医疗", type = "expense"))
            categoryDao.insert(Category(name = "人情", type = "expense"))
            categoryDao.insert(Category(name = "教育", type = "expense"))
            categoryDao.insert(Category(name = "其它", type = "expense"))
        }

        if (categoryDao.countByType("income") == 0) {
            categoryDao.insert(Category(name = "工资", type = "income"))
            categoryDao.insert(Category(name = "奖金", type = "income"))
            categoryDao.insert(Category(name = "兼职", type = "income"))
            categoryDao.insert(Category(name = "理财", type = "income"))
            categoryDao.insert(Category(name = "退款", type = "income"))
            categoryDao.insert(Category(name = "其它", type = "income"))
        }
    }
}
