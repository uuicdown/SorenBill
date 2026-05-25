package com.soren.bill

import android.app.Application
import com.soren.bill.data.database.BillDatabase
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.data.preferences.ThemePreferences

class BillApplication : Application() {
    val database by lazy { BillDatabase.getInstance(this) }
    val repository by lazy {
        BillRepository(
            database.walletDao(),
            database.accountDao(),
            database.categoryDao(),
            database.transactionDao()
        )
    }
    val themePreferences by lazy { ThemePreferences(this) }
}
