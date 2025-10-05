package com.example.grindlyapp1.models

data class Review(
    val id: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val reviewerName: String = "Anonymous"
)
