package com.mesengo.presentation.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mesengo.domain.usecase.RequestOtpUseCase
import com.mesengo.domain.usecase.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val requestOtpUseCase: RequestOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase
) : ViewModel() {

    var otpRequestState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var otpVerifyState by mutableStateOf<UiState>(UiState.Idle)
        private set

    fun requestOtp(phone: String) {
        viewModelScope.launch {
            otpRequestState = UiState.Loading
            otpRequestState = try {
                requestOtpUseCase(phone)
                UiState.Success
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Không thể gửi OTP. Vui lòng thử lại sau.")
            }
        }
    }

    fun verifyOtp(phone: String, otp: String) {
        viewModelScope.launch {
            otpVerifyState = UiState.Loading
            otpVerifyState = try {
                verifyOtpUseCase(phone, otp)
                UiState.Success
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Mã xác thực OTP không chính xác hoặc đã hết hạn.")
            }
        }
    }

    fun resetOtpRequestState() {
        otpRequestState = UiState.Idle
    }

    fun resetOtpVerifyState() {
        otpVerifyState = UiState.Idle
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}
