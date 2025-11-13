package com.example.grindlyapp1.repository

import com.example.grindlyapp1.data.FavouriteDao
import com.example.grindlyapp1.data.FavouriteEntity
import com.example.grindlyapp1.network.FavouriteRequest
import com.example.grindlyapp1.network.ApiResponse
import com.example.grindlyapp1.network.RetrofitClient

class FavouritesRepository(private val dao: FavouriteDao) {

    private val api = RetrofitClient.api

    suspend fun toggleFavourite(token: String, serviceId: String, isOnline: Boolean): ApiResponse? {
        val favEntity = FavouriteEntity(serviceId, isFavourite = true, isSynced = !isOnline)
        dao.insert(favEntity)

        return if (isOnline) {
            try {
                val response = api.toggleFavourite("Bearer $token", FavouriteRequest(serviceId))
                if (response.success) {
                    dao.insert(favEntity.copy(isSynced = true))
                }
                response
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun syncUnsynced(token: String) {
        val unsynced = dao.getUnsynced()
        for (fav in unsynced) {
            try {
                val response = api.toggleFavourite("Bearer $token", FavouriteRequest(fav.serviceId))
                if (response.success) {
                    dao.insert(fav.copy(isSynced = true))
                }
            } catch (_: Exception) {
                // Keep unsynced
            }
        }
    }

    suspend fun getLocalFavourites(): List<FavouriteEntity> = dao.getAll()
}
