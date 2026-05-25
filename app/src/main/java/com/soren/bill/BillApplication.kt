package com.soren.bill

import android.app.Application
import com.soren.bill.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BillApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BillApplication)
            modules(appModule)
        }
    }
}
