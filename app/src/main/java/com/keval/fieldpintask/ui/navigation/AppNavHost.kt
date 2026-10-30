package com.keval.fieldpintask.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.keval.fieldpintask.ui.main.MainScaffold
import com.keval.fieldpintask.ui.main.MainTab
import com.keval.fieldpintask.ui.sites.SitesScreen

object Routes {
    const val SITES = "sites"
    const val MAP = "map"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SITES) {
        composable(Routes.SITES) {
            MainScaffold(selected = MainTab.Sites, onTabSelected = { navController.switchTab(it) }) {
                SitesScreen(onSiteClick = { navController.navigate(Routes.MAP) })
            }
        }
        composable(Routes.MAP) {
            MainScaffold(selected = MainTab.Map, onTabSelected = { navController.switchTab(it) }) {
                Text("Map")
            }
        }
    }
}

private fun NavHostController.switchTab(tab: MainTab) {
    val route = if (tab == MainTab.Sites) Routes.SITES else Routes.MAP
    navigate(route) {
        popUpTo(Routes.SITES) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
