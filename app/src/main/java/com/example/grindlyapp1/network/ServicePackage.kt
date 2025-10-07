    package com.example.grindlyapp1.network

    data class ServicePackage(
        val title: String,
        val price: Double? = 0.0,
        val services: String,
        val sampleImageURLs: List<String>
    )