package com.example.grindlyapp1.network
import com.google.gson.annotations.SerializedName

data class ServicePackageUpdateRequest(
    val userId: String,
    val servicePackages: List<ServicePackage>?,
    val packageStatus: String? = null

)
