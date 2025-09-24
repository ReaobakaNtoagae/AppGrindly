package com.example.grindlyapp1.network

import com.example.grindlyapp1.models.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ---------- Authentication ----------
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    // ---------- Profile ----------
    @Multipart
    @POST("profileImage")
    fun uploadProfileImage(@Part image: MultipartBody.Part): Call<ApiResponse>

    @GET("profile/{userId}")
    fun getProfile(@Path("userId") userId: String): Call<ProfileResponse>

    @POST("profile")
    fun createOrUpdateProfile(@Body profile: ProfileRequest): Call<ApiResponse>

    @POST("profile")
    fun updateServicePackages(@Body request: ServicePackageUpdateRequest): Call<ApiResponse>

    @POST("/profile/update")
    fun updateProfile(@Body request: UserProfileUpdateRequest): Call<ApiResponse>

    // ---------- Services ----------
    @GET("services")
    fun getServices(
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("filterCategory") filter: String? = null
    ): Call<List<Service>>

    @GET("services/{id}")
    fun getServiceDetails(@Path("id") id: String): Call<HustlerProfile>

    // ---------- Combo 3 ----------
    @GET("combo") // Make sure your backend endpoint matches
    fun getComboData(): Call<ComboResponse>
}
