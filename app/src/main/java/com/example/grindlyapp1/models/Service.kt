package com.example.grindlyapp1.models

data class Service(
    val id: String,
    val name: String,
    val title: String,
    val category: String,
    val price: Double,
    val pricingModel: String,
    val workSampleURL: String,
    val rating: String?,
    val location: String,
    val profilePicURL: String,
    var isFavourite: Boolean = false // added property
)
