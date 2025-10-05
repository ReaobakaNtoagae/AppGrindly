package com.example.grindlyapp1.models

data class Review(
    val id: String = "",
    val rating: Float?,
    val comment: String = "",
    val reviewerName: String = "Anonymous"
)
