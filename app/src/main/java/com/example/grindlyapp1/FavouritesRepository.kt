package com.example.grindlyapp1

import retrofit2.Response

class FavouritesRepository {

    private val api = RetrofitInstance.api

    // Toggle favourite (add/remove)
    suspend fun toggleFavourite(token: String, serviceId: String): Response<FavouriteResponse> {
        val bearerToken = "Bearer $token" // Make sure it has "Bearer " prefix
        return api.toggleFavourite(bearerToken, FavouriteRequest(serviceId))
    }

    // Get all favourites
    suspend fun getFavourites(token: String): Response<GetFavouritesResponse> {
        val bearerToken = "Bearer $token"
        return api.getFavourites(bearerToken)
    }
}
