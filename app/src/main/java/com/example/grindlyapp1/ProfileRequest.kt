package com.example.grindlyapp1


    data class ProfileRequest(

    val userId: String,
    val title: String?,
    val category: String?,
    val location: String?,
    val price: String?,
    val description: String?,
    val profilePictureURL: String?,
    val workImageURLs: List<String>?,
    val documentURLs: List<String>?,
    val verifiedBadgeTier: String? = "none",
    val servicePackages: List<String>? = emptyList()
)

