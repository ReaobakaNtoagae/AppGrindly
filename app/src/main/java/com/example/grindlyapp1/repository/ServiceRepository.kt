package com.example.grindlyapp1.repository

import com.example.grindlyapp1.models.ComboResponse
import com.example.grindlyapp1.models.HustlerProfile
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.network.RetrofitClient

class ServiceRepository {

    private val api = RetrofitClient.api

    // Fetch combo data
    suspend fun fetchCombo(): ComboResponse? {
        val response = api.getComboData().execute()
        return if (response.isSuccessful) response.body() else null
    }

    // Fetch filtered services
    suspend fun fetchServices(search: String?, sort: String?, filter: String?): List<Service> {
        val response = api.getServices(search, sort, filter).execute()
        return response.body() ?: emptyList()
    }

    // Fetch service/hustler details
    suspend fun fetchServiceDetails(serviceId: String): HustlerProfile? {
        val response = api.getServiceDetails(serviceId).execute()
        return response.body()
    }
}
