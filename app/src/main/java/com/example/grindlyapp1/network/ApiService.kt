package com.example.grindlyapp1.network

import com.example.grindlyapp1.network.ApiResponse
import com.example.grindlyapp1.network.ProfileRequest
import com.example.grindlyapp1.network.ServicePackageUpdateRequest
import com.example.grindlyapp1.network.UserProfileUpdateRequest
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path


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

    @POST("profile")
    fun updateServicePackages(
        @Body request: ServicePackageUpdateRequest
    ): Call<ApiResponse>

    @POST("/profile/update")
    fun updateProfile(@Body request: UserProfileUpdateRequest): Call<ApiResponse>
}
