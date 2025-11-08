package com.example.grindlyapp1.network

data class HustlerProfile(
    val hustlerId: String,
    val name: String,
    val profilePictureURL: String,
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

data class AdminVerificationsResponse(
    val success: Boolean,
    val hustlers: List<HustlerProfile> = emptyList()
)

data class VerifyHustlerRequest(
    val hustlerId: String,
    val action: String
)

data class VerifyHustlerResponse(
    val success: Boolean,
    val message: String?,
    val verificationStatus: String?
)