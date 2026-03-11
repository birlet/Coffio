package com.example.coffio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.coffio.ui.screens.BrewingScreen
import com.example.coffio.ui.screens.ChartsScreen
import com.example.coffio.ui.screens.HistoryScreen
import com.example.coffio.ui.screens.HomeScreen
import com.example.coffio.ui.screens.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToBrewing = { navController.navigate(Screen.Brewing.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToCharts = { navController.navigate(Screen.Charts.route) }
            )
        }
        composable(Screen.Brewing.route) {
            BrewingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Charts.route) {
            ChartsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
