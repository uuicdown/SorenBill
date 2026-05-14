package com.soren.bill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.soren.bill.ui.navigation.AppNavigation
import com.soren.bill.ui.theme.SorenBillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BillApplication

        setContent {
            SorenBillTheme {
                AppNavigation(repository = app.repository)
            }
        }
    }
}
