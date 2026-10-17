package com.mesengo.presentation.auth.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class MascotMood {
    HAPPY,      // Login Screen: welcoming, smiling
    CURIOUS,    // OTP Screen: looking for secret digits
    SUCCESS     // Verified: celebratory
}

/**
 * Animated MessenGO Mascot ("GoGo" - Chat Buddy).
 * Features idle floating bob, eye blinking, rosy cheeks, and reactive tilt.
 */
@Composable
fun AnimatedMascot(
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.HAPPY,
    isTyping: Boolean = false,
    size: Dp = 110.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_idle")

    // Smooth floating up & down bob
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Gentle breathing scale
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Periodic Eye Blinking
    val blinkAnim = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            // Fast close and open
            blinkAnim.animateTo(0.1f, animationSpec = tween(110))
            blinkAnim.animateTo(1f, animationSpec = tween(130))
        }
    }

    // Interactive head tilt when typing
    val headTilt = if (isTyping) -4f else 0f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = floatOffset
                scaleX = breatheScale
                scaleY = breatheScale
                rotationZ = headTilt
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Soft glowing drop shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2563EB).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.88f),
                    radius = w * 0.42f
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.5f, h * 0.88f)
            )

            // 2. Mascot Body - Chubby Friendly Chat Bubble Squircle
            val bodyPath = Path().apply {
                val bodyRect = RoundRect(
                    rect = Rect(w * 0.12f, h * 0.14f, w * 0.88f, h * 0.82f),
                    cornerRadius = CornerRadius(w * 0.32f, w * 0.32f)
                )
                addRoundRect(bodyRect)

                // Cute chat tail on the bottom-left
                moveTo(w * 0.28f, h * 0.78f)
                quadraticTo(
                    w * 0.18f, h * 0.92f,
                    w * 0.14f, h * 0.96f
                )
                quadraticTo(
                    w * 0.24f, h * 0.88f,
                    w * 0.40f, h * 0.82f
                )
                close()
            }

            // Body Gradient (Vibrant MessenGO Blue to energetic Cyan)
            drawPath(
                path = bodyPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF3B82F6),
                        Color(0xFF2563EB),
                        Color(0xFF06B6D4)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // Inner gloss highlight curve
            val highlightPath = Path().apply {
                val highlightRect = RoundRect(
                    rect = Rect(w * 0.18f, h * 0.18f, w * 0.82f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
                )
                addRoundRect(highlightRect)
            }
            drawPath(
                path = highlightPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
                )
            )

            // 3. Cheerful Antenna / Sparkle on Top
            val antennaTipX = w * 0.68f
            val antennaTipY = h * 0.06f
            val antennaBasePath = Path().apply {
                moveTo(w * 0.58f, h * 0.16f)
                quadraticTo(w * 0.65f, h * 0.10f, antennaTipX, antennaTipY)
            }
            drawPath(
                path = antennaBasePath,
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            // Golden Sparkle Star at tip
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = w * 0.055f,
                center = Offset(antennaTipX, antennaTipY)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.022f,
                center = Offset(antennaTipX, antennaTipY)
            )

            // 4. Expressive Eyes
            val eyeRadius = w * 0.055f
            val eyeBlinkHeight = eyeRadius * blinkAnim.value
            val leftEyeCenter = Offset(w * 0.38f, h * 0.44f)
            val rightEyeCenter = Offset(w * 0.62f, h * 0.44f)

            if (blinkAnim.value > 0.3f) {
                // Open sparkling eyes
                drawOval(
                    color = Color.White,
                    topLeft = Offset(leftEyeCenter.x - eyeRadius, leftEyeCenter.y - eyeBlinkHeight),
                    size = Size(eyeRadius * 2, eyeBlinkHeight * 2)
                )
                drawOval(
                    color = Color.White,
                    topLeft = Offset(rightEyeCenter.x - eyeRadius, rightEyeCenter.y - eyeBlinkHeight),
                    size = Size(eyeRadius * 2, eyeBlinkHeight * 2)
                )

                // Pupil & Catchlights
                val pupilRadius = eyeRadius * 0.65f
                val pupilBlinkHeight = pupilRadius * blinkAnim.value
                val eyeTargetY = if (isTyping) leftEyeCenter.y + 2f else leftEyeCenter.y

                drawOval(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(leftEyeCenter.x - pupilRadius, eyeTargetY - pupilBlinkHeight),
                    size = Size(pupilRadius * 2, pupilBlinkHeight * 2)
                )
                drawOval(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(rightEyeCenter.x - pupilRadius, eyeTargetY - pupilBlinkHeight),
                    size = Size(pupilRadius * 2, pupilBlinkHeight * 2)
                )

                // Sparkle white catchlights in pupils
                drawCircle(
                    color = Color.White,
                    radius = pupilRadius * 0.4f,
                    center = Offset(leftEyeCenter.x - pupilRadius * 0.25f, eyeTargetY - pupilRadius * 0.25f)
                )
                drawCircle(
                    color = Color.White,
                    radius = pupilRadius * 0.4f,
                    center = Offset(rightEyeCenter.x - pupilRadius * 0.25f, eyeTargetY - pupilRadius * 0.25f)
                )
            } else {
                // Closed smiling arch eyes during blink
                val eyeWidth = eyeRadius * 1.8f
                drawArc(
                    color = Color.White,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(leftEyeCenter.x - eyeWidth * 0.5f, leftEyeCenter.y - eyeRadius * 0.5f),
                    size = Size(eyeWidth, eyeRadius),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(rightEyeCenter.x - eyeWidth * 0.5f, rightEyeCenter.y - eyeRadius * 0.5f),
                    size = Size(eyeWidth, eyeRadius),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            // 5. Rosy Blushing Cheeks (Warm friendly coral pink)
            val blushRadius = w * 0.075f
            drawCircle(
                color = Color(0xFFFF6B81).copy(alpha = 0.55f),
                radius = blushRadius,
                center = Offset(w * 0.26f, h * 0.54f)
            )
            drawCircle(
                color = Color(0xFFFF6B81).copy(alpha = 0.55f),
                radius = blushRadius,
                center = Offset(w * 0.74f, h * 0.54f)
            )

            // 6. Happy Smiling Mouth
            val mouthPath = Path().apply {
                when (mood) {
                    MascotMood.HAPPY, MascotMood.SUCCESS -> {
                        moveTo(w * 0.44f, h * 0.54f)
                        quadraticTo(w * 0.50f, h * 0.64f, w * 0.56f, h * 0.54f)
                    }
                    MascotMood.CURIOUS -> {
                        // Cute surprised small "o" mouth
                        addOval(
                            Rect(
                                center = Offset(w * 0.50f, h * 0.56f),
                                radius = w * 0.04f
                            )
                        )
                    }
                }
            }
            if (mood == MascotMood.HAPPY || mood == MascotMood.SUCCESS) {
                drawPath(
                    path = mouthPath,
                    color = Color.White,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            } else {
                drawPath(
                    path = mouthPath,
                    color = Color.White
                )
            }
        }
    }
}
