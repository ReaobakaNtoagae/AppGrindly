package com.example.grindlyapp1.network

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val averageRating: String? = null
)