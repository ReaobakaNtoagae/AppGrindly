package com.example.grindlyapp1.network

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val userType: String
)
