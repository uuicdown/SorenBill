package com.soren.bill.di

import com.soren.bill.data.database.BillDatabase
import com.soren.bill.data.preferences.AppPreferences
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.ui.add.AddTransactionViewModel
import com.soren.bill.ui.assets.AssetsViewModel
import com.soren.bill.ui.calendar.CalendarViewModel
import com.soren.bill.ui.home.HomeViewModel
import com.soren.bill.ui.profile.ProfileViewModel
import com.soren.bill.ui.stats.StatsViewModel
import com.soren.bill.ui.insights.InsightsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { BillDatabase.getInstance(androidContext()) }
    single { get<BillDatabase>().walletDao() }
    single { get<BillDatabase>().accountDao() }
    single { get<BillDatabase>().categoryDao() }
    single { get<BillDatabase>().transactionDao() }
    
    single { 
        BillRepository(
            walletDao = get(),
            accountDao = get(),
            categoryDao = get(),
            transactionDao = get()
        )
    }

    single { AppPreferences(androidContext()) }

    viewModel { HomeViewModel(get()) }
    viewModel { StatsViewModel(get()) }
    viewModel { InsightsViewModel(get()) }
    viewModel { CalendarViewModel(get()) }
    viewModel { AssetsViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { AddTransactionViewModel(get()) }
}
