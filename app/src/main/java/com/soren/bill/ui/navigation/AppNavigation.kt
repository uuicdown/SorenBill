package com.soren.bill.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soren.bill.ui.add.AddTransactionScreen
import com.soren.bill.ui.add.AddTransactionViewModel
import com.soren.bill.ui.assets.AssetsScreen
import com.soren.bill.ui.assets.AssetsViewModel
import com.soren.bill.ui.calendar.CalendarScreen
import com.soren.bill.ui.calendar.CalendarViewModel
import com.soren.bill.ui.home.HomeScreen
import com.soren.bill.ui.home.HomeViewModel
import com.soren.bill.ui.profile.ProfileScreen
import com.soren.bill.ui.profile.ProfileViewModel
import com.soren.bill.ui.stats.StatsScreen
import com.soren.bill.ui.stats.StatsViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Home : Screen("home", "首页", Icons.Outlined.Home, Icons.Filled.Home)
    object Calendar : Screen("calendar", "日历", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    object Assets : Screen("assets", "资产", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet)
    object Profile : Screen("profile", "我的", Icons.Outlined.Person, Icons.Filled.Person)
}

val bottomNavItems = listOf(Screen.Home, Screen.Calendar, Screen.Assets, Screen.Profile)

@Composable
fun AppNavigation() {
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
                            icon = { Icon(if (selected) screen.selectedIcon else screen.icon, contentDescription = screen.title) },
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
                val vm: HomeViewModel = koinViewModel()
                HomeScreen(
                    viewModel = vm,
                    onAddClick = { navController.navigate("add_transaction") },
                    onStatsClick = { navController.navigate("stats") }
                )
            }

            composable(Screen.Calendar.route) {
                val vm: CalendarViewModel = koinViewModel()
                CalendarScreen(viewModel = vm)
            }

            composable(Screen.Assets.route) {
                val vm: AssetsViewModel = koinViewModel()
                AssetsScreen(viewModel = vm)
            }

            composable(Screen.Profile.route) {
                val vm: ProfileViewModel = koinViewModel()
                ProfileScreen(viewModel = vm)
            }

            composable("add_transaction") {
                val vm: AddTransactionViewModel = koinViewModel()
                AddTransactionScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }

            composable("stats") {
                val vm: StatsViewModel = koinViewModel()
                StatsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
        }
    }
}
