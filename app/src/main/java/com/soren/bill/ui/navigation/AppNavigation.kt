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
import com.soren.bill.ui.accounts.AccountScreen
import com.soren.bill.ui.accounts.AccountViewModel
import com.soren.bill.ui.add.AddTransactionScreen
import com.soren.bill.ui.add.AddTransactionViewModel
import com.soren.bill.ui.assets.AssetsScreen
import com.soren.bill.ui.assets.AssetsViewModel
import com.soren.bill.ui.category.CategoryScreen
import com.soren.bill.ui.category.CategoryViewModel
import com.soren.bill.ui.home.HomeScreen
import com.soren.bill.ui.home.HomeViewModel
import com.soren.bill.ui.insights.InsightsScreen
import com.soren.bill.ui.insights.InsightsViewModel
import com.soren.bill.ui.profile.ProfileScreen
import com.soren.bill.ui.settings.SettingsScreen
import com.soren.bill.ui.stats.StatsScreen
import com.soren.bill.ui.stats.StatsViewModel
import com.soren.bill.ui.wallets.WalletScreen
import com.soren.bill.ui.wallets.WalletViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Home : Screen("home", "\u6d41\u6c34", Icons.Outlined.Home, Icons.Filled.Home)
    object Insights : Screen("insights", "\u5206\u6790", Icons.Outlined.Insights, Icons.Filled.Insights)
    object Assets : Screen("assets", "\u8d44\u4ea7", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet)
    object Profile : Screen("profile", "\u6211\u7684", Icons.Outlined.Person, Icons.Filled.Person)
}

val bottomNavItems = listOf(Screen.Home, Screen.Insights, Screen.Assets, Screen.Profile)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(bottomBar = {
        if (showBottomBar) {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(if (selected) screen.selectedIcon else screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = { navController.navigate(screen.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }
                    )
                }
            }
        }
    }) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Home.route, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) {
                val vm: HomeViewModel = koinViewModel()
                HomeScreen(viewModel = vm, onAddClick = { navController.navigate("add_transaction") }, onStatsClick = { navController.navigate("stats") })
            }
            composable(Screen.Insights.route) {
                val vm: InsightsViewModel = koinViewModel()
                InsightsScreen(viewModel = vm)
            }
            composable(Screen.Assets.route) {
                val vm: AssetsViewModel = koinViewModel()
                AssetsScreen(viewModel = vm)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onCategoryClick = { navController.navigate("category") },
                    onWalletClick = { navController.navigate("wallets") },
                    onAccountClick = { navController.navigate("accounts") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("add_transaction") {
                val vm: AddTransactionViewModel = koinViewModel()
                AddTransactionScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable("stats") {
                val vm: StatsViewModel = koinViewModel()
                StatsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable("category") {
                val vm: CategoryViewModel = koinViewModel()
                CategoryScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable("wallets") {
                val vm: WalletViewModel = koinViewModel()
                WalletScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable("accounts") {
                val vm: AccountViewModel = koinViewModel()
                AccountScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}