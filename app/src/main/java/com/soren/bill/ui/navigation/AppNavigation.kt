package com.soren.bill.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soren.bill.data.repository.BillRepository
import com.soren.bill.ui.add.AddTransactionScreen
import com.soren.bill.ui.add.AddTransactionViewModel
import com.soren.bill.ui.assets.AssetsScreen
import com.soren.bill.ui.assets.AssetsViewModel
import com.soren.bill.ui.calendar.CalendarScreen
import com.soren.bill.ui.home.HomeScreen
import com.soren.bill.ui.home.HomeViewModel
import com.soren.bill.ui.stats.StatsScreen
import com.soren.bill.ui.stats.StatsViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Home : Screen("home", "首页", Icons.Outlined.Home, Icons.Filled.Home)
    object Calendar : Screen("calendar", "日历", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    object Assets : Screen("assets", "资产", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet)
    object Stats : Screen("stats", "统计", Icons.Outlined.PieChart, Icons.Filled.PieChart)
}

val bottomNavItems = listOf(Screen.Home, Screen.Calendar, Screen.Assets, Screen.Stats)

@Composable
fun AppNavigation(repository: BillRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == screen.route
                        } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) screen.selectedIcon else screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
                HomeScreen(viewModel = vm, onAddClick = { navController.navigate("add_transaction") })
            }

            composable(Screen.Calendar.route) {
                CalendarScreen()
            }

            composable(Screen.Assets.route) {
                val vm: AssetsViewModel = viewModel(factory = AssetsViewModel.Factory(repository))
                AssetsScreen(viewModel = vm)
            }

            composable(Screen.Stats.route) {
                val vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory(repository))
                StatsScreen(viewModel = vm)
            }

            composable("add_transaction") {
                val vm: AddTransactionViewModel = viewModel(factory = AddTransactionViewModel.Factory(repository))
                AddTransactionScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
