package com.example.grindlyapp1.network
import com.google.gson.annotations.SerializedName

data class ServicePackage(
    @SerializedName("title") val title: String,
    @SerializedName("services") val services: String,
    @SerializedName("price") val price: Double,
    @SerializedName("sampleImageURLs") val sampleImageURLs: List<String>
)