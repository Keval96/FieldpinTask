package com.keval.fieldpintask.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keval.fieldpintask.R
import com.keval.fieldpintask.ui.camera.CameraScreen
import com.keval.fieldpintask.ui.inspection.InspectionScreen
import com.keval.fieldpintask.ui.main.MainScaffold
import com.keval.fieldpintask.ui.main.MainTab
import com.keval.fieldpintask.ui.map.MapScreen
import com.keval.fieldpintask.ui.newsite.NewSiteScreen
import com.keval.fieldpintask.ui.sites.SitesScreen

object Routes {
    const val SITES = "sites"
    const val NEW_SITE = "newSite"
    const val ARG_SITE_ID = "siteId"
    const val KEY_PHOTO_PATH = "photoPath"
    const val MAP = "map?$ARG_SITE_ID={$ARG_SITE_ID}"
    const val INSPECTION = "inspection/{$ARG_SITE_ID}"
    const val CAMERA = "camera/{$ARG_SITE_ID}"

    fun map(siteId: String? = null) = if (siteId == null) "map" else "map?$ARG_SITE_ID=$siteId"
    fun inspection(siteId: String) = "inspection/$siteId"
    fun camera(siteId: String) = "camera/$siteId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SITES) {
        composable(Routes.SITES) {
            MainScaffold(
                selected = MainTab.Sites,
                onTabSelected = { navController.switchTab(it) },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Routes.NEW_SITE) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.sites_add)) },
                    )
                },
            ) {
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
        composable(Routes.NEW_SITE) {
            NewSiteScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.INSPECTION) { entry ->
            val siteId = checkNotNull(entry.arguments?.getString(Routes.ARG_SITE_ID))
            // The camera writes its result to this entry's own handle, not the ViewModel's.
            val newPhoto by entry.savedStateHandle.getStateFlow<String?>(Routes.KEY_PHOTO_PATH, null).collectAsStateWithLifecycle()
            InspectionScreen(
                onBack = { navController.popBackStack() },
                onAddPhoto = { navController.navigate(Routes.camera(siteId)) },
                newPhoto = newPhoto,
                onPhotoConsumed = { entry.savedStateHandle[Routes.KEY_PHOTO_PATH] = null },
            )
        }
        composable(Routes.CAMERA) {
            CameraScreen(
                onPhotoTaken = { path ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(Routes.KEY_PHOTO_PATH, path)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
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
