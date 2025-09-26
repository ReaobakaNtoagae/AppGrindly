package com.example.grindlyapp1.models

import com.example.grindlyapp1.network.ServicePackage

data class HustlerProfile(
    val hustlerId: String,
    val name: String,
    val profilePicURL: String,
    val title: String,
    val category: String,
    val price: Double,
    val phoneNumber: String,
    val description: String,
    val workImageURLs: List<String>? = emptyList(),
    val location: String? = null,
    val pricingModel: String? = null,
    val verifiedBadgeTier: String? = "none",
    val servicePackages: List<ServicePackage>? = emptyList(),
    val packageStatus: String?,
    val reviews: List<Review> = emptyList()
)
