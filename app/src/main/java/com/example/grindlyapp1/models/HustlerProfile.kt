package com.example.grindlyapp1.models

data class HustlerProfile(
    val hustlerId: String,
    val name: String,
    val profilePicUrl: String,
    val serviceTitle: String,
    val category: String,
    val price: Double,
    val description: String,
    val workSamples: List<String> = emptyList(),
    val reviews: List<Review> = emptyList()
)
