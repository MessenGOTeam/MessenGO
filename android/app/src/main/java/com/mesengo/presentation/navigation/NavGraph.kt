package com.mesengo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mesengo.data.local.prefs.TokenManager
import com.mesengo.presentation.auth.LoginScreen
import com.mesengo.presentation.auth.OtpScreen
import com.mesengo.presentation.main.MainScreen
import com.mesengo.presentation.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    tokenManager: TokenManager
) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigate = {
                    val dest = if (tokenManager.isLoggedIn()) Screen.Main.route
                    else Screen.Login.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onOtpSent = { phone ->
                    navController.navigate(Screen.Otp.createRoute(phone))
                }
            )
        }
        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                onVerified = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}
