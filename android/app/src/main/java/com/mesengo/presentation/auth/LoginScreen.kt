package com.mesengo.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesengo.R
import com.mesengo.presentation.auth.components.AmbientBackground
import com.mesengo.presentation.auth.components.AnimatedMascot
import com.mesengo.presentation.auth.components.MascotMood
import com.mesengo.presentation.auth.components.ShimmerGradientButton
import com.mesengo.ui.theme.MesenGOTheme

@Composable
fun LoginScreen(
    onOtpSent: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val requestState = viewModel.otpRequestState
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe OTP request outcome
    LaunchedEffect(requestState) {
        if (requestState is UiState.Success) {
            val formattedPhone = formatPhoneNumberForApi(phoneInput)
            viewModel.resetOtpRequestState()
            onOtpSent(formattedPhone)
        }
    }

    fun submitPhone() {
        keyboardController?.hide()
        focusManager.clearFocus()

        val cleanPhone = phoneInput.filter { it.isDigit() }
        if (cleanPhone.length !in 9..10) {
            validationError = "Số điện thoại cần có từ 9 đến 10 chữ số bạn nhé!"
            return
        }
        validationError = null
        val targetPhone = formatPhoneNumberForApi(cleanPhone)
        viewModel.requestOtp(targetPhone)
    }

    AmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Animated Mascot & Warm Greeting
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // The living animated MessenGO Mascot "GoGo"
                AnimatedMascot(
                    mood = MascotMood.HAPPY,
                    isTyping = phoneInput.isNotEmpty(),
                    size = 115.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Friendly App Title & Warm Greeting
                Text(
                    text = "MessenGO",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Chào bạn mới ghé chơi! 👋",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Kết nối trò chuyện cùng hội bạn thân & người thương bất cứ lúc nào.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Main Interactive Glassmorphic Card
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
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Số điện thoại di động",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Phone Input Row with Country Chip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Vietnam Country Code Pill
                            Row(
                                modifier = Modifier
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_flag_vn),
                                    contentDescription = "Vietnam",
                                    modifier = Modifier
                                        .size(width = 22.dp, height = 15.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+84",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Phone TextField
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }
                                    if (filtered.length <= 10) {
                                        phoneInput = filtered
                                        validationError = null
                                        if (requestState is UiState.Error) {
                                            viewModel.resetOtpRequestState()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                placeholder = {
                                    Text(
                                        text = "0912 345 678",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { submitPhone() }
                                ),
                                trailingIcon = {
                                    if (phoneInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                phoneInput = ""
                                                validationError = null
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_clear),
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Real-time Friendly Helper Pill
                        val helperPillText = when {
                            validationError != null -> validationError!!
                            (requestState as? UiState.Error)?.message != null -> (requestState as UiState.Error).message
                            phoneInput.isEmpty() -> "💬 Nhập số điện thoại để nhận mã OTP nha"
                            phoneInput.length in 1..8 -> "⏳ Còn ${10 - phoneInput.length} chữ số nữa nè..."
                            phoneInput.length in 9..10 -> "✨ Số đẹp rồi! Sẵn sàng nhận mã OTP"
                            else -> ""
                        }

                        val isErrorMessage = validationError != null || requestState is UiState.Error

                        val helperBgColor by animateColorAsState(
                            targetValue = when {
                                isErrorMessage -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                phoneInput.length >= 9 -> Color(0xFF10B981).copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            animationSpec = tween(250),
                            label = "helperBg"
                        )

                        val helperTextColor by animateColorAsState(
                            targetValue = when {
                                isErrorMessage -> MaterialTheme.colorScheme.error
                                phoneInput.length >= 9 -> Color(0xFF059669)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(250),
                            label = "helperText"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(helperBgColor)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = helperPillText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = helperTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bouncy Shimmering Primary Button
                        val isLoading = requestState is UiState.Loading
                        val isEnabled = phoneInput.length >= 9 && !isLoading

                        ShimmerGradientButton(
                            text = "Tiếp tục — Nhận mã OTP 🚀",
                            onClick = { submitPhone() },
                            enabled = isEnabled,
                            isLoading = isLoading,
                            loadingText = "Đang gửi mã bí mật..."
                        )
                    }
                }
            }

            // Bottom Section: Security Badge & Cheerful Disclaimer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_shield_security),
                        contentDescription = "Security",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mã hóa bảo mật hai chiều & không spam 🔒",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Bằng việc tiếp tục, bạn đồng ý với Điều khoản & Chính sách của MessenGO.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/**
 * Formats phone number for VN standard:
 * "0912345678" -> "+84912345678"
 */
private fun formatPhoneNumberForApi(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.startsWith("84") -> "+$digits"
        digits.startsWith("0") -> "+84" + digits.substring(1)
        else -> "+84$digits"
    }
}

@Preview(showBackground = true, name = "LoginScreen Light")
@Composable
fun LoginScreenPreview() {
    MesenGOTheme {
        LoginScreen(onOtpSent = {})
    }
}
