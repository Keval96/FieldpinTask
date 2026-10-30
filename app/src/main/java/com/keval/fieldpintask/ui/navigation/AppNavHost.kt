package com.keval.fieldpintask.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keval.fieldpintask.ui.main.MainScaffold
import com.keval.fieldpintask.ui.main.MainTab
import com.keval.fieldpintask.ui.map.MapScreen
import com.keval.fieldpintask.ui.sites.SitesScreen

object Routes {
    const val SITES = "sites"
    const val ARG_SITE_ID = "siteId"
    const val MAP = "map?$ARG_SITE_ID={$ARG_SITE_ID}"
    const val INSPECTION = "inspection/{$ARG_SITE_ID}"

    fun map(siteId: String? = null) = if (siteId == null) "map" else "map?$ARG_SITE_ID=$siteId"
    fun inspection(siteId: String) = "inspection/$siteId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SITES) {
        composable(Routes.SITES) {
            MainScaffold(selected = MainTab.Sites, onTabSelected = { navController.switchTab(it) }) {
                SitesScreen(onSiteClick = { site -> navController.navigate(Routes.map(site.id)) })
            }
        }
        composable(
            route = Routes.MAP,
            arguments = listOf(navArgument(Routes.ARG_SITE_ID) { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) {
            MainScaffold(selected = MainTab.Map, onTabSelected = { navController.switchTab(it) }) {
                MapScreen(onStartInspection = { id -> navController.navigate(Routes.inspection(id)) })
            }
        }
        composable(Routes.INSPECTION) { Text("Inspection") }
    }
}

private fun NavHostController.switchTab(tab: MainTab) {
    val route = if (tab == MainTab.Sites) Routes.SITES else Routes.map()
    navigate(route) {
        popUpTo(Routes.SITES) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
