package com.lorran.lupanutri.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lorran.lupanutri.LupaApp
import com.lorran.lupanutri.ui.lists.SavedScreen
import com.lorran.lupanutri.ui.manual.ManualScreen
import com.lorran.lupanutri.ui.product.ProductScreen
import com.lorran.lupanutri.ui.profile.ProfileScreen
import com.lorran.lupanutri.ui.scan.ScanScreen
import com.lorran.lupanutri.ui.search.SearchScreen
import com.lorran.lupanutri.ui.shopping.ShoppingScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("scan", "Escanear", Icons.Outlined.QrCodeScanner),
    Tab("search", "Buscar", Icons.Outlined.Search),
    Tab("shopping", "Lista", Icons.Outlined.ShoppingCart),
    Tab("saved", "Salvos", Icons.Outlined.BookmarkBorder),
    Tab("profile", "Perfil", Icons.Outlined.Person),
)

fun NavHostController.openProduct(code: String) = navigate("product/$code") { launchSingleTop = true }

@Composable
fun AppNav(app: LupaApp) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = TABS.any { it.route == route }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "scan", modifier = Modifier.padding(padding)) {
            composable("scan") {
                ScanScreen(
                    onCode = { nav.openProduct(it) },
                    onOpenProfile = { nav.navigate("profile") { launchSingleTop = true } },
                )
            }
            composable("search") {
                SearchScreen(app, onOpen = { nav.openProduct(it) })
            }
            composable("shopping") {
                ShoppingScreen(app, onOpen = { nav.openProduct(it) })
            }
            composable("saved") {
                SavedScreen(app, onOpen = { nav.openProduct(it) })
            }
            composable("profile") {
                ProfileScreen(app)
            }
            composable(
                "product/{code}",
                arguments = listOf(navArgument("code") { type = NavType.StringType }),
            ) { e ->
                val code = e.arguments?.getString("code").orEmpty()
                ProductScreen(
                    app = app,
                    code = code,
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.openProduct(it) },
                    onRegister = { nav.navigate("manual/$code") },
                    onEdit = { nav.navigate("manual/$code") },
                )
            }
            composable(
                "manual/{code}",
                arguments = listOf(navArgument("code") { type = NavType.StringType }),
            ) { e ->
                val code = e.arguments?.getString("code").orEmpty()
                ManualScreen(
                    app = app,
                    code = code,
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        nav.popBackStack()
                        if (nav.currentBackStackEntry?.destination?.route == "product/{code}") nav.popBackStack()
                        nav.openProduct(code)
                    },
                    onDeleted = {
                        nav.popBackStack()
                        if (nav.currentBackStackEntry?.destination?.route == "product/{code}") nav.popBackStack()
                    },
                )
            }
        }
    }
}
