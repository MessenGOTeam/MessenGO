package com.mesengo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.mesengo.data.local.prefs.TokenManager
import com.mesengo.presentation.navigation.NavGraph
import com.mesengo.ui.theme.MesenGOTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MesenGOTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    tokenManager = tokenManager
                )
            }
        }
    }
}