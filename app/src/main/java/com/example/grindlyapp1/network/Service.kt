package com.example.grindlyapp1.network


data class Service(
    val id: String = "",
    val hustlerId: String? = null,
    val title: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val pricingModel: String = "",
    val location: String = "",
    val rating: String = " ",
    val category: String? = null,
    val profilePictureURL: String? = null,
    val workImageURL: String? = null,
    var isFavourite: Boolean = false,
    val reviewCount: Int = 0
)
