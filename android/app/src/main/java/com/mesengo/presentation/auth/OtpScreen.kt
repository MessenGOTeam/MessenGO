package com.mesengo.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesengo.R
import com.mesengo.presentation.auth.components.AmbientBackground
import com.mesengo.presentation.auth.components.AnimatedMascot
import com.mesengo.presentation.auth.components.AnimatedOtpGrid
import com.mesengo.presentation.auth.components.MascotMood
import com.mesengo.presentation.auth.components.ShimmerGradientButton
import com.mesengo.ui.theme.MesenGOTheme
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    phone: String,
    onVerified: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val verifyState = viewModel.otpVerifyState
    var otpInput by rememberSaveable { mutableStateOf("") }
    var countdown by rememberSaveable { mutableIntStateOf(180) }
    var isTimerActive by rememberSaveable { mutableStateOf(true) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus OTP input on screen entry
    LaunchedEffect(Unit) {
        delay(250)
        focusRequester.requestFocus()
    }

    // Countdown Timer logic
    LaunchedEffect(isTimerActive, countdown) {
        if (isTimerActive && countdown > 0) {
            delay(1000)
            countdown -= 1
        } else if (countdown <= 0) {
            isTimerActive = false
        }
    }

    // Observe OTP verification success
    LaunchedEffect(verifyState) {
        if (verifyState is UiState.Success) {
            viewModel.resetOtpVerifyState()
            onVerified()
        }
    }

    fun submitOtp() {
        if (otpInput.length == 6) {
            keyboardController?.hide()
            focusManager.clearFocus()
            viewModel.verifyOtp(phone, otpInput)
        }
    }

    AmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top App Bar with back navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Xác nhận số điện thoại",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Curious Mascot looking for secret OTP digits
                    AnimatedMascot(
                        mood = if (verifyState is UiState.Success) MascotMood.SUCCESS else MascotMood.CURIOUS,
                        isTyping = otpInput.isNotEmpty(),
                        size = 110.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Mã bí mật đã tới! 💌",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Nhập mã gồm 6 chữ số vừa gửi qua SMS tới:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Target Phone Chip with Edit Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onBack() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = phone.ifEmpty { "+84 ••• ••• •••" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit_phone),
                            contentDescription = stringResource(R.string.otp_edit_phone),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // OTP Container Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(26.dp),
                                ambientColor = Color.Black.copy(alpha = 0.04f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 6-Cell Bouncy Animated OTP Grid
                            val isError = verifyState is UiState.Error
                            AnimatedOtpGrid(
                                otpValue = otpInput,
                                onOtpChange = { newOtp ->
                                    otpInput = newOtp
                                    if (verifyState is UiState.Error) {
                                        viewModel.resetOtpVerifyState()
                                    }
                                },
                                focusRequester = focusRequester,
                                isError = isError,
                                onComplete = { submitOtp() }
                            )

                            // Animated Error message banner
                            val errorMessage = (verifyState as? UiState.Error)?.message
                            AnimatedVisibility(
                                visible = errorMessage != null,
                                enter = fadeIn(animationSpec = tween(200)),
                                exit = fadeOut(animationSpec = tween(150))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Resend Timer / Action
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isTimerActive && countdown > 0) {
                                    val minutes = countdown / 60
                                    val seconds = countdown % 60
                                    val timeDisplay = String.format("%d:%02d", minutes, seconds)
                                    Text(
                                        text = "Chờ gửi lại mã sau ${timeDisplay} nha ⏱️",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.requestOtp(phone)
                                                countdown = 180
                                                isTimerActive = true
                                                otpInput = ""
                                                viewModel.resetOtpVerifyState()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_refresh),
                                            contentDescription = "Resend",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Gửi lại mã OTP mới 🔄",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Verify Action Button
                            val isLoading = verifyState is UiState.Loading
                            val isEnabled = otpInput.length == 6 && !isLoading

                            ShimmerGradientButton(
                                text = "Xác nhận & Bắt đầu trò chuyện ✨",
                                onClick = { submitOtp() },
                                enabled = isEnabled,
                                isLoading = isLoading,
                                loadingText = "Đang kiểm tra mã..."
                            )
                        }
                    }
                }

                // Bottom security disclaimer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mã xác thực có hiệu lực trong vòng 5 phút 🔐",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "OtpScreen Light")
@Composable
fun OtpScreenPreview() {
    MesenGOTheme {
        OtpScreen(
            phone = "+84912345678",
            onVerified = {},
            onBack = {}
        )
    }
}
