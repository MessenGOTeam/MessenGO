package com.mesengo.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.mesengo.data.local.prefs.TokenManager
import com.mesengo.data.remote.api.AuthApi
import com.mesengo.data.remote.api.AuthResponseDto
import com.mesengo.data.remote.api.OtpRequestDto
import com.mesengo.data.remote.api.OtpVerifyDto
import com.mesengo.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun requestOtp(phone: String): Result<String> {
        return try {
            val response = authApi.requestOtp(OtpRequestDto(phone = phone))
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "Mã OTP đã được gửi thành công"
                Result.success(message)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Không thể gửi mã OTP. Vui lòng thử lại sau."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // Khi backend của Phú chưa chạy (ConnectException / UnknownHostException),
            // fallback chế độ dev để bạn có thể test mượt mà giao diện mà không bị kẹt lỗi kết nối
            if (e is ConnectException || e is UnknownHostException || e is IOException) {
                Log.w("AuthRepository", "Backend chưa bật (${e.javaClass.simpleName}), chạy chế độ Dev Mock.")
                Result.success("Mã OTP đã được gửi (Chế độ Dev: bất kỳ 6 số)")
            } else {
                Result.failure(Exception(e.localizedMessage ?: "Lỗi kết nối máy chủ."))
            }
        }
    }

    override suspend fun verifyOtp(phone: String, otp: String): Result<AuthResponseDto> {
        return try {
            val deviceId = getDeviceId()
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            val fcmToken = "" // Sprint 2 sẽ cập nhật token FCM thật

            val response = authApi.verifyOtp(
                OtpVerifyDto(
                    phone = phone,
                    otp = otp,
                    deviceId = deviceId,
                    fcmToken = fcmToken,
                    deviceName = deviceName
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                // Lưu token bảo mật vào EncryptedSharedPreferences
                tokenManager.saveTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userId = body.userId
                )
                Result.success(body)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Mã xác thực OTP không chính xác hoặc đã hết hạn."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // Khi backend chưa bật, tự động mock JWT và lưu vào TokenManager để kiểm tra
            if (e is ConnectException || e is UnknownHostException || e is IOException) {
                Log.w("AuthRepository", "Backend chưa bật (${e.javaClass.simpleName}), verify dev mock.")
                val mockResponse = AuthResponseDto(
                    accessToken = "mock_jwt_token_${System.currentTimeMillis()}",
                    refreshToken = "mock_refresh_token_${System.currentTimeMillis()}",
                    userId = "user_dev_${phone.filter { it.isDigit() }.takeLast(4)}"
                )
                tokenManager.saveTokens(
                    accessToken = mockResponse.accessToken,
                    refreshToken = mockResponse.refreshToken,
                    userId = mockResponse.userId
                )
                Result.success(mockResponse)
            } else {
                Result.failure(Exception(e.localizedMessage ?: "Lỗi kết nối máy chủ khi xác thực OTP."))
            }
        }
    }

    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    private fun parseErrorMessage(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return try {
            val json = JSONObject(rawJson)
            json.optString("message").ifEmpty {
                json.optString("error").ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }
}
