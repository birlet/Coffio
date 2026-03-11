package com.example.coffio.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Brewing : Screen("brewing/{drinkId}") {
        fun createRoute(drinkId: Long) = "brewing/$drinkId"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
    object Charts : Screen("charts")
}
