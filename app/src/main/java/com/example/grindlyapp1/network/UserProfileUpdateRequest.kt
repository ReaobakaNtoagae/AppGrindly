package com.example.grindlyapp1.network

data class UserProfileUpdateRequest(
    val userId: String,
    val title: String,
    val category: String,
    val price: String,
    val location: String,
    val pricingModel: String,
    val description: String,
    val imageUris: List<String> = emptyList(),
    val docUris: List<String> = emptyList()
)