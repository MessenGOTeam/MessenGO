package com.mesengo.presentation.splash

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SplashScreen(onNavigate: () -> Unit) {
    LaunchedEffect(Unit) {
        onNavigate()
    }
    Text(text = "Splash Screen")
}
