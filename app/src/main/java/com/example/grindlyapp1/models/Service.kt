package com.example.grindlyapp1.models


data class Service(
    val id: String = "",
    val title: String = "",             // Default empty string instead of null
    val category: String = "",          // Default empty string
    val name: String = "",              // Default empty string
    val price: Double = 0.0,            // Default 0.0 instead of null
    val pricingModel: String = "",      // Default empty string
    val location: String = "",          // Default empty string
    val profilePicURL: String = "",     // Default empty string
    val workSampleURL: String = "",     // Default empty string
    val isFavourite: Boolean = false,
    val rating: Float = 0f,             // Default 0f
    val reviewCount: Int = 0            // Default 0
)





