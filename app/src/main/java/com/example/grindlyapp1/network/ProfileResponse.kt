package com.example.grindlyapp1.network

import com.example.grindlyapp1.network.ServicePackage
data class ProfileResponse(
    val userId: String? = null,
    val title: String? = null,
    val category: String? = null,
    val location: String? = null,
    val price: String? = null,
    val pricingModel: String? = null,
    val description: String? = null,
    val profilePictureURL: String? = null,
    val workImageURLs: List<String>? = emptyList(),
    val documentURLs: List<String>? = emptyList(),
    val verifiedBadgeTier: String? = "none",
    val servicePackages: List<ServicePackage>? = emptyList(),
    val packageStatus: String?
)
