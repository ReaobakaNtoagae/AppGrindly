package com.example.grindlyapp1

import androidx.room.*

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavouriteEntity)

    @Query("SELECT * FROM favourites WHERE serviceId = :serviceId LIMIT 1")
    suspend fun get(serviceId: String): FavouriteEntity?

    @Query("SELECT * FROM favourites WHERE isSynced = 0")
    suspend fun getUnsynced(): List<FavouriteEntity>

    @Query("SELECT * FROM favourites")
    suspend fun getAll(): List<FavouriteEntity>

    @Query("""
        UPDATE favourites 
        SET isSynced = 1, pendingAction = NULL 
        WHERE serviceId = :serviceId
    """)
    suspend fun markSynced(serviceId: String)

    companion object
}
