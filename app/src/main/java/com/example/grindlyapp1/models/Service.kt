package com.example.grindlyapp1.models

data class Service(
    val id: String,
    val hustlerName: String,
    val serviceTitle: String,
    val category: String,
    val price: Double,
    val thumbnailUrl: String,
    val rating: Float
)
