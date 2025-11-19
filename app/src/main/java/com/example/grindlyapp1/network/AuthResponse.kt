package com.example.grindlyapp1.network

data class AuthResponse(
    val message: String,
    val token: String,
    val role: String,
    val firstTime: Boolean,
    val userId: String,
    val fullName: String
)