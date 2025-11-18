package com.example.grindlyapp1.network

import com.google.gson.annotations.SerializedName

data class ServicePackageUpdateRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("servicePackages") val servicePackages: List<ServicePackage>,
    @SerializedName("packageStatus") val packageStatus: String
)