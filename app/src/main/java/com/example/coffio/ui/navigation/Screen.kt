package com.example.coffio.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Brewing : Screen("brewing")
    object History : Screen("history")
    object Settings : Screen("settings")
    object Charts : Screen("charts")
}
