package com.mesengo.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/otp/request")
    suspend fun requestOtp(@Body body: OtpRequestDto): Response<ApiResponse>

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyDto): Response<AuthResponseDto>

    @POST("api/v1/auth/token/refresh")
    suspend fun refreshToken(@Body body: RefreshTokenDto): Response<AuthResponseDto>
}

data class OtpRequestDto(val phone: String)

data class OtpVerifyDto(
    val phone: String,
    val otp: String,
    val deviceId: String,
    val fcmToken: String,
    val deviceName: String
)

data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)

data class RefreshTokenDto(val refreshToken: String)
data class ApiResponse(val message: String)
