package com.mesengo.presentation.auth.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 6-Digit Animated Bouncy OTP Input Grid with auto-focus,
 * spring scale pop per digit, and error shake animation.
 */
@Composable
fun AnimatedOtpGrid(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onComplete: (String) -> Unit = {}
) {
    // Shake animation when isError becomes true
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            for (i in 0..2) {
                shakeOffset.animateTo(-14f, animationSpec = tween(50))
                shakeOffset.animateTo(14f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = shakeOffset.value },
        contentAlignment = Alignment.Center
    ) {
        // Transparent BasicTextField spanning the entire area
        BasicTextField(
            value = otpValue,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(6)
                onOtpChange(digits)
                if (digits.length == 6) {
                    onComplete(digits)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (otpValue.length == 6) {
                        onComplete(otpValue)
                    }
                }
            ),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (index in 0 until 6) {
                        val digit = otpValue.getOrNull(index)?.toString() ?: ""
                        val isCurrent = otpValue.length == index

                        BouncyCell(
                            digit = digit,
                            isFocused = isCurrent,
                            isError = isError
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun BouncyCell(
    digit: String,
    isFocused: Boolean,
    isError: Boolean
) {
    // Bouncy scale-up pop when digit is entered
    val hasDigit = digit.isNotEmpty()
    val cellScale by animateFloatAsState(
        targetValue = if (hasDigit) 1.04f else if (isFocused) 1.02f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cellScale"
    )

    // Glowing border pulse for the currently focused cell
    val infiniteTransition = rememberInfiniteTransition(label = "cell_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    val borderColor = when {
        isError -> errorColor
        isFocused -> primaryColor.copy(alpha = pulseAlpha)
        hasDigit -> primaryColor.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val backgroundColor = when {
        isError -> errorColor.copy(alpha = 0.06f)
        isFocused -> primaryColor.copy(alpha = 0.08f)
        hasDigit -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderWidth = if (isFocused || isError) 2.dp else 1.2.dp

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 58.dp)
            .graphicsLayer {
                scaleX = cellScale
                scaleY = cellScale
            }
            .shadow(
                elevation = if (isFocused) 6.dp else if (hasDigit) 3.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = if (isFocused) primaryColor.copy(alpha = 0.3f) else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasDigit) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                ),
                color = if (isError) errorColor else MaterialTheme.colorScheme.onSurface
            )
        } else if (isFocused) {
            // Animated cursor line
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryColor.copy(alpha = cursorAlpha))
            )
        }
    }
}
