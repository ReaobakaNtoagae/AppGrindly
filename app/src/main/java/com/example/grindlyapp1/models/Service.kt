package com.example.grindlyapp1.models

data class Service(
    val id: String = "",
    val title: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val pricingModel: String = "",
    val location: String = "",
    val rating: String = " ",
    val category: String? = null,
    val profilePictureURL: String? ,
    val workImageURL: String?,
    var isFavourite: Boolean = false,
    val reviewCount: Int = 0
)
