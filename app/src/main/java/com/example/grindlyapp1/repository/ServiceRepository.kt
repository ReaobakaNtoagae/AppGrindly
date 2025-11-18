package com.example.grindlyapp1.repository

import android.util.Log
import com.example.grindlyapp1.ServiceDao
import com.example.grindlyapp1.ServiceEntity
import com.example.grindlyapp1.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ServiceRepository(
    private val api: ApiService,
    private val serviceDao: ServiceDao
) {

    companion object {
        private const val TAG = "ServiceRepository"
    }

    // --- Unified error handler ---
    private inline fun <T> safeApiCall(default: T, block: () -> T): T {
        return try {
            block()
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            default
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error", e)
            default
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            default
        }
    }

    // --- Fetch all services ---
    suspend fun fetchServices(
        token: String,
        search: String? = null,
        sort: String? = null,
        filter: String? = null
    ): List<Service> = withContext(Dispatchers.IO) {
        safeApiCall(emptyList()) {
            api.getServices("Bearer $token", search, sort, filter)
        }
    }

    // --- Fetch services with offline fallback ---
    suspend fun fetchServicesWithCache(token: String): List<Service> = withContext(Dispatchers.IO) {
        val favourites = getUserFavourites(token).toSet()

        val services = safeApiCall(null) {
            val networkServices = api.getServices("Bearer $token")
            val entities = networkServices.map {
                ServiceEntity(
                    id = it.id,
                    hustlerId = it.hustlerId,
                    title = it.title,
                    name = it.name,
                    price = it.price.toString(),
                    pricingModel = it.pricingModel,
                    location = it.location,
                    rating = it.rating,
                    category = it.category,
                    profilePic = it.profilePictureURL,
                    workImage = it.workImageURL,
                    reviewCount = it.reviewCount
                )
            }

            serviceDao.clearAll()
            serviceDao.insertAll(entities)
            networkServices


        }

        services?.map {
            it.copy(isFavourite = favourites.contains(it.id))
        } ?: run {
            Log.e(TAG, "Offline → loading cached services")
            safeApiCall(emptyList()) {
                serviceDao.getAllServices().map {
                    Service(
                        id = it.id,
                        hustlerId = it.hustlerId,
                        title = it.title ?: "",
                        name = it.name ?: "",
                        price = it.price?.toDoubleOrNull() ?: 0.0,
                        pricingModel = it.pricingModel ?: "",
                        location = it.location ?: "",
                        rating = it.rating ?: "0",
                        category = it.category,
                        profilePictureURL = it.profilePic,
                        workImageURL = it.workImage,
                        reviewCount = it.reviewCount ?: 0,
                        isFavourite = favourites.contains(it.id)
                    )
                }
            }
        }
    }

    // --- Fetch service details ---
    suspend fun fetchServiceDetails(token: String, serviceId: String): ComboResponse? =
        withContext(Dispatchers.IO) {
            safeApiCall(null) {
                api.getServiceDetails("Bearer $token", serviceId)
            }
        }

    // --- Fetch reviews ---
    suspend fun fetchReviews(token: String, serviceId: String): List<Review> =
        withContext(Dispatchers.IO) {
            safeApiCall(emptyList()) {
                val response = api.getReviews("Bearer $token", serviceId)
                if (response.success) {
                    response.reviews.map {
                        Review(
                            id = it.id,
                            rating = it.rating?.toDouble() ?: 0.0,
                            comment = it.comment ?: "",
                            reviewerName = it.reviewerName ?: "Anonymous"
                        )
                    }
                } else emptyList()
            }
        }

    // --- Submit a review ---
    suspend fun submitReview(token: String, request: SubmitReviewRequest): ApiResponse? =
        withContext(Dispatchers.IO) {
            safeApiCall(null) {
                api.submitReview("Bearer $token", request)
            }
        }

    // --- Get user favourites ---
    suspend fun getUserFavourites(token: String): List<String> = withContext(Dispatchers.IO) {
        safeApiCall(emptyList()) {
            val response = api.getFavourites("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.favourites?.map { it.serviceId } ?: emptyList()
            } else emptyList()
        }
    }

    // --- Toggle favourite ---
    suspend fun toggleFavourite(token: String, serviceId: String): Boolean =
        withContext(Dispatchers.IO) {
            safeApiCall(false) {
                val response = api.toggleFavourite("Bearer $token", FavouriteRequest(serviceId))
                response.isSuccessful
            }
        }
}