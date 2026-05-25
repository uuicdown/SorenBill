package com.soren.bill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.soren.bill.data.preferences.ThemeMode
import com.soren.bill.ui.navigation.AppNavigation
import com.soren.bill.ui.theme.SorenBillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BillApplication

        setContent {
            val themeMode by app.themePreferences.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SorenBillTheme(darkTheme = isDark) {
                AppNavigation(repository = app.repository)
            }
        }
    }
}
