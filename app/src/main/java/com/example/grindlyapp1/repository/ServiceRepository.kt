package com.example.grindlyapp1.repository

import android.util.Log
import com.example.grindlyapp1.network.ComboResponse
import com.example.grindlyapp1.network.Review
import com.example.grindlyapp1.network.Service
import com.example.grindlyapp1.network.ApiResponse
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.network.SubmitReviewRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ServiceRepository {

    private val api = RetrofitClient.api
    private val TAG = "ServiceRepository"


    suspend fun fetchServices(
        token: String,
        search: String? = null,
        sort: String? = null,
        filter: String? = null
    ): List<Service> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching services | search=$search | sort=$sort | filter=$filter")

        try {
            val services = api.getServices(token, search, sort, filter)
            Log.d(TAG, "Fetched ${services.size} services successfully.")
            services.forEach {
                Log.d(TAG, "Service ${it.title}: profile=${it.profilePictureURL} work=${it.workImageURL}")
            }
            services
        } catch (e: IOException) {
            Log.e(TAG, "Network error while fetching services: ${e.message}", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error while fetching services: ${e.code()} ${e.message()}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while fetching services: ${e.message}", e)
            emptyList()
        }
    }

    // ------------------------
    // Fetch service details
    // ------------------------
    suspend fun fetchServiceDetails(token: String,id: String): ComboResponse? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching service details for ID: $id")

        try {
            val response = api.getServiceDetails(token,id)
            Log.d(TAG, "Fetched service details successfully: $response")
            response
        } catch (e: IOException) {
            Log.e(TAG, "Network error while fetching service details: ${e.message}", e)
            null
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error while fetching service details: ${e.code()} ${e.message()}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while fetching service details: ${e.message}", e)
            null
        }
    }

    // ------------------------
    // Fetch reviews
    // ------------------------
    suspend fun fetchReviews(token: String,serviceId: String): List<Review> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching reviews for service ID: $serviceId")

        try {
            val response = api.getReviews(token, serviceId)
            Log.d(TAG, "Raw reviews API response: $response")

            if (response.success) {
                val reviews = response.reviews.map {
                    Review(
                        id = it.id,
                        rating = it.rating?.toDouble() ?: 0.0,
                        comment = it.comment ?: "",
                        reviewerName = it.reviewerName ?: "Anonymous"
                    )
                }
                Log.d(TAG, "Fetched ${reviews.size} reviews successfully.")
                reviews
            } else {
                Log.w(TAG, "Fetch reviews failed: success flag was false.")
                emptyList()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error while fetching reviews: ${e.message}", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error while fetching reviews: ${e.code()} ${e.message()}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while fetching reviews: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun submitReview(token: String, request: SubmitReviewRequest): ApiResponse? {
        Log.d(TAG, "Submitting review: $request")

        return try {
            val response = RetrofitClient.api.submitReview("Bearer $token", request)
            Log.d(TAG, "Review submitted successfully: $response")
            response
        } catch (e: IOException) {
            Log.e(TAG, "Network error while submitting review: ${e.message}", e)
            null
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error while submitting review: ${e.code()} ${e.message()}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while submitting review: ${e.message}", e)
            null
        }
    }
}