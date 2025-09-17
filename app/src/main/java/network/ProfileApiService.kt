package network

import network.ApiResponse
import network.ProfileRequest
import network.ServicePackageUpdateRequest
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ProfileApiService {

    @Multipart
    @POST("profileImage")
    fun uploadProfileImage(
        @Part image: MultipartBody.Part
    ): Call<ApiResponse>

    @GET("profile/{userId}")
    fun getProfile(
        @Path("userId") userId: String
    ): Call<ApiResponse>

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