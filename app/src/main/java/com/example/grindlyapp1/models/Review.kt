package com.example.grindlyapp1.models

data class Review(
    val id: String = "",
    val rating: Double,
    val comment: String = "",
    val reviewerName: String = "Anonymous"
)
