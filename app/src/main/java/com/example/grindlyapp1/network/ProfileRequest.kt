package com.example.grindlyapp1.network

data class ProfileRequest(
    val userId: String,
    val title: String,
    val category: String,
    val location: String,
    val price: Double? = 0.0,
    val pricingModel: String,
    val description: String,
    val profilePictureURL: String? = null,
    val workImageURLs: List<String> = emptyList(),
    val documentURLs: List<String> = emptyList(),
    val verificationStatus: String = "unverified",
    val servicePackages: List<ServicePackage>? = emptyList(),
    val packageStatus: String? = null,
    val hasProfile: Boolean = true
)
