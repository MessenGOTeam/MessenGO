package com.mesengo.domain.usecase

import com.mesengo.domain.repository.AuthRepository
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String): Result<String> {
        val result = authRepository.requestOtp(phone)
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: Exception("Không thể gửi mã OTP")
        }
        return result
    }
}
