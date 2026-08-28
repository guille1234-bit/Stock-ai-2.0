package com.example.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.AppViewModelFactory
import com.example.ui.screens.ai.AiAssistantScreen
import com.example.ui.screens.ai.AiAssistantViewModel
import com.example.ui.screens.earnings.EarningsScreen
import com.example.ui.screens.earnings.EarningsViewModel
import com.example.ui.screens.expenses.ExpensesScreen
import com.example.ui.screens.expenses.ExpensesViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.movements.MovementsScreen
import com.example.ui.screens.movements.MovementsViewModel
import com.example.ui.screens.products.ProductsScreen
import com.example.ui.screens.products.ProductsViewModel
import com.example.ui.screens.purchases.PurchasesScreen
import com.example.ui.screens.purchases.PurchasesViewModel
import com.example.ui.screens.sales.SalesScreen
import com.example.ui.screens.sales.SalesViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Products : Screen("products", "Stock", Icons.Default.Inventory2)
    object Sales : Screen("sales", "Ventas", Icons.Default.PointOfSale)
    object Earnings : Screen("earnings", "Stats", Icons.Default.ReceiptLong)
    object Settings : Screen("settings", "Más", Icons.Default.Settings)
    object Ai : Screen("ai", "Stock AI", Icons.Default.AutoAwesome)
    object Purchases : Screen("purchases", "Compras", Icons.Default.ShoppingBag)
    object Expenses : Screen("expenses", "Gastos", Icons.Default.Payments)
    object Movements : Screen("movements", "Auditoría", Icons.Default.Timeline)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Products,
    Screen.Sales,
    Screen.Earnings,
    Screen.Settings
)

@Composable
fun AppNavigation(
    factory: AppViewModelFactory,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                // Subtle top border line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.Home.route) {
                val homeVm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = homeVm,
                    onNavigateToSales = { navController.navigate(Screen.Sales.route) },
                    onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                    onNavigateToPurchases = { navController.navigate(Screen.Purchases.route) },
                    onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                    onNavigateToEarnings = { navController.navigate(Screen.Earnings.route) },
                    onNavigateToAi = { navController.navigate(Screen.Ai.route) }
                )
            }

            composable(Screen.Sales.route) {
                val salesVm: SalesViewModel = viewModel(factory = factory)
                SalesScreen(viewModel = salesVm)
            }

            composable(Screen.Products.route) {
                val productsVm: ProductsViewModel = viewModel(factory = factory)
                ProductsScreen(viewModel = productsVm)
            }

            composable(Screen.Earnings.route) {
                val earningsVm: EarningsViewModel = viewModel(factory = factory)
                EarningsScreen(viewModel = earningsVm)
            }

            composable(Screen.Ai.route) {
                val aiVm: AiAssistantViewModel = viewModel(factory = factory)
                AiAssistantScreen(viewModel = aiVm)
            }

            composable(Screen.Purchases.route) {
                val purchasesVm: PurchasesViewModel = viewModel(factory = factory)
                PurchasesScreen(viewModel = purchasesVm)
            }

            composable(Screen.Expenses.route) {
                val expensesVm: ExpensesViewModel = viewModel(factory = factory)
                ExpensesScreen(viewModel = expensesVm)
            }

            composable(Screen.Movements.route) {
                val movementsVm: MovementsViewModel = viewModel(factory = factory)
                MovementsScreen(viewModel = movementsVm)
            }

            composable(Screen.Settings.route) {
                val settingsVm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(viewModel = settingsVm)
            }
        }
    }
}
