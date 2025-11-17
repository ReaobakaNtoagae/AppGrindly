package com.example.grindlyapp1.network

data class FavouriteResponse(
    val success: Boolean,
    val message: String
)

data class GetFavouritesResponse(
    val success: Boolean,
    val favourites: List<FavouriteItem>
)

data class FavouriteItem(
    val serviceId: String,
    val timestamp: String? = null,
    val service: Service? = null
)