package com.example.grindlyapp1.repository

import android.util.Log
import com.example.grindlyapp1.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ServiceRepository(private val api: ApiService) {

    companion object {
        private const val TAG = "ServiceRepository"
    }

    // --- Fetch all services ---
    suspend fun fetchServices(
        token: String,
        search: String? = null,
        sort: String? = null,
        filter: String? = null
    ): List<Service> = withContext(Dispatchers.IO) {
        try {
            api.getServices("Bearer $token", search, sort, filter)
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching services", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error fetching services", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error fetching services", e)
            emptyList()
        }
    }

    // --- Fetch details of a single service ---
    suspend fun fetchServiceDetails(token: String, serviceId: String): ComboResponse? = withContext(Dispatchers.IO) {
        try {
            api.getServiceDetails("Bearer $token", serviceId)
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching service details", e)
            null
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error fetching service details", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error fetching service details", e)
            null
        }
    }

    // --- Fetch reviews ---
    suspend fun fetchReviews(token: String, serviceId: String): List<Review> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReviews("Bearer $token", serviceId)
            if (response.success) {
                response.reviews.map { r ->
                    Review(
                        id = r.id,
                        rating = r.rating?.toDouble() ?: 0.0,
                        comment = r.comment ?: "",
                        reviewerName = r.reviewerName ?: "Anonymous"
                    )
                }
            } else emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching reviews", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error fetching reviews", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error fetching reviews", e)
            emptyList()
        }
    }

    // --- Submit a review ---
    suspend fun submitReview(token: String, request: SubmitReviewRequest): ApiResponse? = withContext(Dispatchers.IO) {
        try {
            api.submitReview("Bearer $token", request)
        } catch (e: IOException) {
            Log.e(TAG, "Network error submitting review", e)
            null
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error submitting review", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error submitting review", e)
            null
        }
    }

   
    suspend fun getUserFavourites(token: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFavourites("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.favourites?.map { it.serviceId } ?: emptyList()
            } else emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching favourites", e)
            emptyList()
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error fetching favourites", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error fetching favourites", e)
            emptyList()
        }
    }


    // --- Toggle favourite (add/remove) ---
    suspend fun toggleFavourite(token: String, serviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = FavouriteRequest(serviceId)
            val response = api.toggleFavourite("Bearer $token", request)
            response.isSuccessful
        } catch (e: IOException) {
            Log.e(TAG, "Network error toggling favourite", e)
            false
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error toggling favourite", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error toggling favourite", e)
            false
        }
    }
}
