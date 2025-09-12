package com.example.grindlyapp1.network


data class AuthResponse(
    val userId: String,
    val token: String,
    val userType: String
)
