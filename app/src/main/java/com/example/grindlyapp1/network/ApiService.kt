package com.example.grindlyapp1.network

import com.example.grindlyapp1.GenericResponse
import com.example.grindlyapp1.PasswordChangeRequest
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
    fun uploadProfileImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Call<ApiResponse>

    @GET("profile/{userId}")
    fun getProfile(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Call<ProfileResponse>

    @POST("profile")
    fun createOrUpdateProfile(
        @Header("Authorization") token: String,
        @Body profile: ProfileRequest
    ): Call<ApiResponse>

    @POST("profile")
    fun updateServicePackages(
        @Header("Authorization") token: String,
        @Body request: ServicePackageUpdateRequest
    ): Call<ApiResponse>

    @POST("/profile/update")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UserProfileUpdateRequest
    ): Call<ApiResponse>

    // ---------- Services ----------
    @GET("services")
    suspend fun getServices(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("filterCategory") filter: String? = null
    ): List<Service>

    @GET("services/{id}")
    suspend fun getServiceDetails(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): ComboResponse

    // ---------- Reviews ----------
    @GET("reviews/{serviceId}")
    suspend fun getReviews(
        @Header("Authorization") token: String,
        @Path("serviceId") serviceId: String
    ): ReviewResponse

    @POST("reviews")
    suspend fun submitReview(
        @Header("Authorization") token: String,
        @Body request: SubmitReviewRequest
    ): ApiResponse

    // ---------- Favourites ----------
    @GET("favourites")
    suspend fun getFavourites(@Header("Authorization") token: String): FavouriteResponse

    @POST("favourites")
    suspend fun toggleFavourite(
        @Header("Authorization") token: String,
        @Body request: FavouriteRequest
    ): ApiResponse


    @POST("user/change-password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: PasswordChangeRequest
    ): Call<GenericResponse>

    @DELETE("user/account")
    fun deleteAccount(
        @Header("Authorization") token: String,
        @Query("userId") userId: String
    ): Call<GenericResponse>

    @POST("user/logout")
    fun logout(
        @Header("Authorization") token: String
    ): Call<GenericResponse>
}
