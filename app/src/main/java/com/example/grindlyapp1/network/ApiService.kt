package com.example.grindlyapp1.network

import com.example.grindlyapp1.models.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

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
    suspend fun getServices(
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("filterCategory") filter: String? = null
    ): List<Service>

    @GET("services/{id}")
    suspend fun getServiceDetails(@Path("id") id: String): ComboResponse

    // ---------- Reviews ----------
    @GET("reviews/{serviceId}")
    suspend fun getReviews(@Path("serviceId") serviceId: String): ReviewResponse

    @POST("reviews")
    suspend fun submitReview(
        @Header("Authorization") token : String,
        @Body request: SubmitReviewRequest
    ): ApiResponse


    @GET("favourites")
    suspend fun getFavourites(@Header("Authorization") token: String): FavouriteResponse

    @POST("favourites")
    suspend fun toggleFavourite(
        @Header("Authorization") token: String,
        @Body request: FavouriteRequest
    ): ApiResponse
}



