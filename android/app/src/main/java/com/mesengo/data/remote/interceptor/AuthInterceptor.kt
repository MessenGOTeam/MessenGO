package com.mesengo.data.remote.interceptor

import com.mesengo.data.local.prefs.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Không gắn token cho các API auth (login, otp)
        if (request.url.encodedPath.contains("/auth/")) {
            return chain.proceed(request)
        }

        val token = tokenManager.getAccessToken()
        val newRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        return chain.proceed(newRequest)
    }
}
