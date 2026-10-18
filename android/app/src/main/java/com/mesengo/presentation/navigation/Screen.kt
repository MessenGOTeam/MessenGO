package com.mesengo.presentation.navigation

// presentation/navigation/Screen.kt
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Otp : Screen("otp/{phone}") {
        fun createRoute(phone: String) = "otp/$phone"
    }
    object Main : Screen("main")
}