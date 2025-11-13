package com.example.grindlyapp1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val serviceId: String,
    val isFavourite: Boolean,
    val isSynced: Boolean = false
)
