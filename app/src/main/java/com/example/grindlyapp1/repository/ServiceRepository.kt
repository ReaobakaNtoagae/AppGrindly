package com.example.grindlyapp1.repository

import android.util.Log
import com.example.grindlyapp1.models.ComboResponse
import com.example.grindlyapp1.models.Review
import com.example.grindlyapp1.models.ReviewResponse
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.network.ApiResponse
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.network.SubmitReviewRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ServiceRepository {

    private val api = RetrofitClient.api

    suspend fun fetchServices(
        search: String? = null,
        sort: String? = null,
        filter: String? = null
    ): List<Service> = withContext(Dispatchers.IO) {
        try {
            api.getServices(search, sort, filter)
        } catch (e: IOException) {
            Log.e("ServiceRepository", "Network error while fetching services", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e("ServiceRepository", "HTTP error while fetching services", e)
            emptyList()
        }
    }

    suspend fun fetchServiceDetails(id: String): ComboResponse? = withContext(Dispatchers.IO) {
        try {
            api.getServiceDetails(id)
        } catch (e: IOException) {
            Log.e("ServiceRepository", "Network error while fetching service details", e)
            null
        } catch (e: HttpException) {
            Log.e("ServiceRepository", "HTTP error while fetching service details", e)
            null
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Unexpected error while fetching service details", e)
            null
        }
    }

    // -------------------
    // NEW: Fetch reviews
    // -------------------
    suspend fun fetchReviews(serviceId: String): List<Review> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReviews(serviceId)
            if (response.success) {
                response.reviews.map {
                    Review(
                        id = it.id,
                        rating = it.rating?.toFloat() ?: 0f,   // <- safely handle nulls
                        comment = it.comment ?: "",
                        reviewerName = it.reviewerName ?: "Anonymous"
                    )
                }

            } else emptyList()
        } catch (e: IOException) {
            Log.e("ServiceRepository", "Network error while fetching reviews", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e("ServiceRepository", "HTTP error while fetching reviews", e)
            emptyList()
        }
    }

    suspend fun submitReview(request: SubmitReviewRequest, token: String): ApiResponse? {
        return try {
            RetrofitClient.api.submitReview("Bearer $token", request)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error submitting review", e)
            null
        }
    }


}
