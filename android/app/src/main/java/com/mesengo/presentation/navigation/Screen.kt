package com.mesengo.presentation.navigation

// presentation/navigation/Screen.kt
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Otp : Screen("otp/{phone}") {
        fun createRoute(phone: String) = "otp/$phone"
    }
    object Main : Screen("main")
    object ChatDetail : Screen("chat/{conversationId}/{peerUserId}/{peerDisplayName}") {
        fun createRoute(conversationId: String, peerUserId: String, peerDisplayName: String): String {
            val encodedName = java.net.URLEncoder.encode(peerDisplayName, "UTF-8")
            return "chat/$conversationId/$peerUserId/$encodedName"
        }
    }
}