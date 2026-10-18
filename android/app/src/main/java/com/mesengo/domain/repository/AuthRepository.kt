package com.mesengo.domain.repository

import com.mesengo.data.remote.api.AuthResponseDto

interface AuthRepository {
    suspend fun requestOtp(phone: String): Result<String>
    suspend fun verifyOtp(phone: String, otp: String): Result<AuthResponseDto>
}
