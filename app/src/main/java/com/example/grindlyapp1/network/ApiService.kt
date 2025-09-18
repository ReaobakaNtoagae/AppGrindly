package com.example.grindlyapp1.network

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @Multipart
    @POST("profileImage")
    fun uploadProfileImage(
        @Part image: MultipartBody.Part
    ): Call<ApiResponse>

    @GET("profile/{userId}")
    fun getProfile(
        @Path("userId") userId: String
    ): Call<ProfileResponse>

    @POST("profile")
    fun createOrUpdateProfile(
        @Body profile: ProfileRequest
    ): Call<ApiResponse>

    @POST("profile/packages")
    fun updateServicePackages(
        @Body request: ServicePackageUpdateRequest
    ): Call<ApiResponse>

    @POST("profile/update")
    fun updateProfile(
        @Body request: UserProfileUpdateRequest
    ): Call<ApiResponse>
}
