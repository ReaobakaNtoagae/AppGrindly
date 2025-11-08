package com.example.grindlyapp1.network

data class Review(
    val id: String = "",
    val rating: Double,
    val comment: String = "",
    val reviewerName: String = "Anonymous"
)
