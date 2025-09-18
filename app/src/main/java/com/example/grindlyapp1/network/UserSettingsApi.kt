package com.example.grindlyapp1.network

import com.example.grindlyapp1.GenericResponse
import com.example.grindlyapp1.PasswordChangeRequest
import retrofit2.Call
import retrofit2.http.*

interface UserSettingsApi {

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
