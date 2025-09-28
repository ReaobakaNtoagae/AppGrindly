package com.example.grindlyapp1

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface FavouritesApi {
    @POST("favourites")
    suspend fun toggleFavourite(
        @Header("Authorization") token: String,
        @Body body: FavouriteRequest
    ): Response<FavouriteResponse>

    @GET("favourites")
    suspend fun getFavourites(
        @Header("Authorization") token: String
    ): Response<GetFavouritesResponse>
}

data class FavouriteRequest(val serviceId: String)
data class FavouriteResponse(val success: Boolean, val message: String)
data class GetFavouritesResponse(val success: Boolean, val favourites: List<String>)
