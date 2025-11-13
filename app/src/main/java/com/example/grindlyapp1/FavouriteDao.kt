package com.example.grindlyapp1.data

import androidx.room.*

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourites WHERE isSynced = 0")
    suspend fun getUnsynced(): List<FavouriteEntity>

    @Query("SELECT * FROM favourites")
    suspend fun getAll(): List<FavouriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavouriteEntity)
}
