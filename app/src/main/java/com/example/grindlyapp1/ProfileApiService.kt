package com.example.grindlyapp1

import android.provider.ContactsContract
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
    ): Call<ContactsContract.Profile>

    @POST("profile")
    fun createOrUpdateProfile(
        @Body profile: ContactsContract.Profile
    ): Call<ApiResponse>
}
