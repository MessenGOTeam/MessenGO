package com.mesengo.presentation.auth.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic ambient animated background with soft floating color orbs.
 * Creates an organic, modern, friendly atmosphere distinct from flat chat apps.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_orbs")

    // Slow drifting animation coordinates
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Breathing pulse for orb scale
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val baseBg = MaterialTheme.colorScheme.background

    // Warm, friendly palette orbs
    val primaryColor = MaterialTheme.colorScheme.primary
    val cyanColor = Color(0xFF06B6D4)
    val warmCoralColor = Color(0xFFFF8A65)
    val sunnyAmberColor = Color(0xFFFBBF24)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Primary Blue - floating top-left to center
            val orb1X = width * 0.25f + cos(phase) * (width * 0.15f)
            val orb1Y = height * 0.2f + sin(phase) * (height * 0.08f)
            val orb1Radius = width * 0.55f * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                        primaryColor.copy(alpha = 0f)
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = Offset(orb1X, orb1Y)
            )

            // Orb 2: Electric Cyan - floating top-right
            val orb2X = width * 0.8f + sin(phase + 1.5f) * (width * 0.12f)
            val orb2Y = height * 0.35f + cos(phase + 1.5f) * (height * 0.1f)
            val orb2Radius = width * 0.48f * (2f - pulse)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cyanColor.copy(alpha = if (isDark) 0.14f else 0.09f),
                        cyanColor.copy(alpha = 0f)
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = Offset(orb2X, orb2Y)
            )

            // Orb 3: Sunset Coral / Peach - warm friendly glow bottom-left
            val orb3X = width * 0.2f + cos(phase + 3f) * (width * 0.12f)
            val orb3Y = height * 0.75f + sin(phase + 3f) * (height * 0.09f)
            val orb3Radius = width * 0.52f * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        warmCoralColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                        warmCoralColor.copy(alpha = 0f)
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = orb3Radius
                ),
                radius = orb3Radius,
                center = Offset(orb3X, orb3Y)
            )

            // Orb 4: Sunny Amber sparkle center-bottom
            val orb4X = width * 0.75f + sin(phase * 1.5f) * (width * 0.1f)
            val orb4Y = height * 0.85f + cos(phase * 1.5f) * (height * 0.07f)
            val orb4Radius = width * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        sunnyAmberColor.copy(alpha = if (isDark) 0.1f else 0.06f),
                        sunnyAmberColor.copy(alpha = 0f)
                    ),
                    center = Offset(orb4X, orb4Y),
                    radius = orb4Radius
                ),
                radius = orb4Radius,
                center = Offset(orb4X, orb4Y)
            )
        }

        // Screen content on top of ambient canvas
        content()
    }
}
