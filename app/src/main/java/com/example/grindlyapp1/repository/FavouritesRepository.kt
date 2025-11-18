package com.example.grindlyapp1.repository

import com.example.grindlyapp1.FavouriteDao
import com.example.grindlyapp1.FavouriteEntity
import com.example.grindlyapp1.network.FavouriteRequest
import com.example.grindlyapp1.network.ApiResponse
import com.example.grindlyapp1.network.FavouriteResponse
import com.example.grindlyapp1.network.RetrofitClient


class FavouritesRepository(private val dao: FavouriteDao) {

    private val api = RetrofitClient.api

    suspend fun toggleFavourite(token: String, serviceId: String, isOnline: Boolean): FavouriteResponse? {

        val current = dao.get(serviceId)
        val newState = !(current?.isFavourite ?: false)
        val action = if (newState) "ADD" else "REMOVE"

        val updated = FavouriteEntity(
            serviceId = serviceId,
            isFavourite = newState,
            isSynced = isOnline,
            pendingAction = if (isOnline) null else action
        )

        dao.insert(updated)

        if (!isOnline) return null

        return try {
            val response = api.toggleFavourite("Bearer $token", FavouriteRequest(serviceId))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    dao.markSynced(serviceId)
                }
                body
            } else null
        } catch (e: Exception) {
            dao.insert(updated.copy(isSynced = false, pendingAction = action))
            null
        }
    }

    suspend fun syncUnsynced(token: String) {
        val unsynced = dao.getUnsynced()
        for (fav in unsynced) {
            try {
                val response = api.toggleFavourite("Bearer $token", FavouriteRequest(fav.serviceId))
                if (response.isSuccessful && response.body()?.success == true) {
                    dao.markSynced(fav.serviceId)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun getLocalFavourites() = dao.getAll()
}

