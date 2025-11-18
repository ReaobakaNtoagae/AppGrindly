package com.example.grindlyapp1.network

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String,
    val role: String
)
