package com.example.grindlyapp1.models

data class Service(
    val id: String,
    val title: String,
    val name: String,
    val price: Double,
    val pricingModel: String,
    val location: String,
    val rating: String?,
    val category: String?,
    val profilePicURL: String?,
    val workSampleURL: String?,
    var isFavourite: Boolean = false // <-- now var
)


