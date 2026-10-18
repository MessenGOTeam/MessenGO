package com.mesengo.presentation.auth.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Modern Bouncy Shimmer Button with tactile press scale,
 * dynamic gradient sweep and loading spinner.
 */
@Composable
fun ShimmerGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = "Đang xử lý...",
    height: Dp = 56.dp,
    shapeRadius: Dp = 18.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    // Shimmer light sweep animation across the button
    val infiniteTransition = rememberInfiniteTransition(label = "button_shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val isInteractive = enabled && !isLoading

    val activeBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3B82F6),
            Color(0xFF2563EB),
            Color(0xFF1D4ED8)
        )
    )

    val disabledBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        )
    )

    val shimmerBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.28f),
            Color.Transparent
        ),
        startX = shimmerTranslate,
        endX = shimmerTranslate + 220f
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .shadow(
                elevation = if (isInteractive) 8.dp else 0.dp,
                shape = RoundedCornerShape(shapeRadius),
                ambientColor = Color(0xFF2563EB).copy(alpha = 0.35f),
                spotColor = Color(0xFF1D4ED8).copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(shapeRadius))
            .background(if (isInteractive) activeBrush else disabledBrush)
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                coroutineScope.launch {
                                    pressScale.animateTo(0.96f, animationSpec = spring(stiffness = 500f))
                                }
                                tryAwaitRelease()
                                coroutineScope.launch {
                                    pressScale.animateTo(1f, animationSpec = spring(stiffness = 400f))
                                }
                            },
                            onTap = { onClick() }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Overlay shimmer highlight if interactive
        if (isInteractive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(shimmerBrush)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = if (isInteractive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}
