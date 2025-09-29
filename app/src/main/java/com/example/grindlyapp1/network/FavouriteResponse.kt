package com.example.grindlyapp1.network

data class FavouriteResponse(
    val success: Boolean,
    val message: String
)

data class GetFavouritesResponse(
    val success: Boolean,
    val favourites: List<String> // List of service IDs
)
