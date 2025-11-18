package com.example.grindlyapp1

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val hustlerId: String?,
    val title: String?,
    val name: String?,
    val price: String?,
    val pricingModel: String?,
    val location: String?,
    val rating: String?,
    val category: String?,
    val profilePic: String?,
    val workImage: String?,
    val reviewCount: Int?
)