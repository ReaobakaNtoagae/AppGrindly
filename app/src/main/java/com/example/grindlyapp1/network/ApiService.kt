package com.example.grindlyapp1.network

import com.example.grindlyapp1.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ---------- Auth ----------
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    // ---------- Profile ----------
    @Multipart
    @POST("profileImage")
    fun uploadProfileImage(
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

    @POST("profile/update")
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
    @POST("favourites")
    suspend fun toggleFavourite(
        @Header("Authorization") token: String,
        @Body body: FavouriteRequest
    ): Response<FavouriteResponse>

    @GET("favourites")
    suspend fun getFavourites(
        @Header("Authorization") token: String
    ): Response<GetFavouritesResponse>

    @POST("auth/logout")
    fun logout(
        @Header("Authorization") token: String
    ): Call<GenericResponse>

    // -------------------------
    // CHANGE PASSWORD
    // Backend route: POST /user/change-password
    // Body: { userId, oldPassword, newPassword }
    // -------------------------
    @POST("user/change-password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: PasswordChangeRequest
    ): Call<GenericResponse>

    // -------------------------
    // DELETE ACCOUNT
    // Backend route: DELETE /user/account?userId=xxxx
    // -------------------------
    @DELETE("user/account")
    fun deleteAccount(
        @Header("Authorization") token: String,
        @Query("userId") userId: String
    ): Call<GenericResponse>

    // -------------------------
    // TOGGLE NOTIFICATIONS
    // Backend route: POST /user/notifications
    // Body: { enable: Boolean }
    // Uses req.user.userId from token
    // -------------------------
    @POST("user/notifications")
    fun toggleNotifications(
        @Header("Authorization") token: String,
        @Body request: Map<String, Boolean>
    ): Call<GenericResponse>

    // -------------------------
    // TOGGLE BIOMETRICS
    // Backend route: POST /user/biometrics
    // Body: { enable: Boolean }
    // -------------------------
    @POST("user/biometrics")
    fun toggleBiometrics(
        @Header("Authorization") token: String,
        @Body request: Map<String, Boolean>
    ): Call<GenericResponse>


    // ---------- Bookings ----------
    @GET("bookings/client/{clientId}")
    suspend fun getClientBookings(
        @Header("Authorization") token: String,
        @Path("clientId") clientId: String
    ): Response<List<Booking>>

    @GET("bookings/hustler/{hustlerId}")
    suspend fun getHustlerBookings(
        @Header("Authorization") token: String,
        @Path("hustlerId") hustlerId: String
    ): Response<List<Booking>>

    @GET("bookings/{bookingId}")
    suspend fun getBookingById(
        @Header("Authorization") token: String,
        @Path("bookingId") bookingId: String
    ): Response<BookingResponse>

    @PATCH("bookings/{bookingId}/status")
    suspend fun updateBookingStatus(
        @Header("Authorization") token: String,
        @Path("bookingId") bookingId: String,
        @Body request: BookingStatusUpdateRequest
    ): Response<BookingStatusUpdateResponse>

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body bookingRequest: BookingRequest
    ): Response<ApiResponse>

    // ---------- Admin ----------
    @GET("admin/verifications")
    suspend fun getPendingHustlers(
        @Header("Authorization") token: String
    ): Response<AdminVerificationsResponse>

    @POST("admin/verify-hustler")
    suspend fun verifyHustler(
        @Header("Authorization") token: String,
        @Body request: VerifyHustlerRequest
    ): Response<VerifyHustlerResponse>


    @POST("update-fcm-token")
    fun updateFcmToken(
        @Body body: Map<String, String>
    ): Call<Void>

}

