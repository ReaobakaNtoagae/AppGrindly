package com.example.grindlyapp1.models

data class Service(
    val id: String = "",
    val title: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val pricingModel: String = "",
    val location: String = "",
    val rating: Float? = null,
    val category: String? = null,
    val profilePicURL: String? = null,
    val workSampleURL: String? = null,
    var isFavourite: Boolean = false,
    val reviewCount: Int = 0
)


