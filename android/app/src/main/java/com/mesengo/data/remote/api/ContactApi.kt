package com.mesengo.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ContactApi {

    @GET("api/v1/users/search")
    suspend fun searchByPhone(@Query("phone") phone: String): Response<SearchUserResponse>

    @GET("api/v1/users/me")
    suspend fun getMyProfile(): Response<MyProfileResponse>

    @POST("api/v1/users/device")
    suspend fun updateDevice(@Body body: UpdateDeviceDto): Response<ApiResponse>

    @POST("api/v1/contacts/request")
    suspend fun requestContact(@Body body: ContactRequestDto): Response<ApiResponse>

    @POST("api/v1/contacts/respond")
    suspend fun respondContact(@Body body: ContactRespondDto): Response<ApiResponse>

    @GET("api/v1/contacts")
    suspend fun getContacts(@Query("type") type: String? = null): Response<GetContactsResponse>
}

data class ContactRequestDto(
    val peerId: String? = null,
    val contactUserId: String? = null,
    val contactPhone: String? = null,
    val message: String? = null
)

data class ContactRespondDto(
    val peerId: String,
    val action: String // "accept" or "reject"
)

data class GetContactsResponse(
    val success: Boolean,
    val data: GetContactsData?
)

data class GetContactsData(
    val contacts: List<ServerContactItem>?
)

data class ServerContactItem(
    val id: String,
    val ownerId: String,
    val contactUserId: String,
    val status: String,
    val alias: String? = null,
    val profile: SearchUserItem? = null
)

data class SearchUserResponse(
    val success: Boolean,
    val message: String?,
    val data: SearchUserData?
)

data class SearchUserData(
    val user: SearchUserItem,
    val relationship: RelationshipItem?
)

data class SearchUserItem(
    val _id: String,
    val displayName: String,
    val phone: String,
    val avatarMediaId: String? = null,
    val statusText: String? = null,
    val isSelf: Boolean = false
)

data class RelationshipItem(
    val status: String? = "none",
    val isSelf: Boolean = false
)

data class MyProfileResponse(
    val success: Boolean,
    val data: MyProfileData?
)

data class MyProfileData(
    val user: SearchUserItem
)

data class UpdateDeviceDto(
    val deviceId: String,
    val deviceName: String,
    val fcmToken: String,
    val appVersion: String
)
