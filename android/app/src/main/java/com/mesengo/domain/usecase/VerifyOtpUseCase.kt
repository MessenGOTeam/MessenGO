package com.mesengo.domain.usecase

import com.mesengo.data.remote.api.AuthResponseDto
import com.mesengo.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String, otp: String): Result<AuthResponseDto> {
        val result = authRepository.verifyOtp(phone, otp)
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: Exception("Mã xác thực OTP không hợp lệ")
        }
        return result
    }
}
