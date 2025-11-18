package com.example.grindlyapp1.network

import com.google.gson.annotations.SerializedName

data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String,
    @SerializedName("fcmToken") val fcmToken: String? = null
)

// Also update your regular LoginRequest if needed
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("fcmToken") val fcmToken: String? = null
)