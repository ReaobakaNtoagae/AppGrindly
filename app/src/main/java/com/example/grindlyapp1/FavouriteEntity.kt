package com.example.grindlyapp1

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val serviceId: String,
    val isFavourite: Boolean,
    val isSynced: Boolean,
    val pendingAction: String? = null
)
