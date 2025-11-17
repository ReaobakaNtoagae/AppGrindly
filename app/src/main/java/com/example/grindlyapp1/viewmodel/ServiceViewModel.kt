package com.example.grindlyapp1.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grindlyapp1.models.*
import com.example.grindlyapp1.network.ComboResponse
import com.example.grindlyapp1.network.Review
import com.example.grindlyapp1.network.Service
import com.example.grindlyapp1.network.SubmitReviewRequest
import com.example.grindlyapp1.repository.ServiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceViewModel(
    private val repo: ServiceRepository,
    private val userToken: String
) : ViewModel() {

    companion object {
        private const val TAG = "ServiceViewModel"
    }

    // --- StateFlows ---
    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> get() = _services

    private val _serviceDetail = MutableStateFlow<ComboResponse?>(null)
    val serviceDetail: StateFlow<ComboResponse?> get() = _serviceDetail

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> get() = _reviews

    private var allServices: List<Service> = emptyList()

    // --- Load services ---
    fun loadServicesList() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching services...")
                val serviceList = repo.fetchServices(userToken)

                // Merge favourites and normalize data
                val currentFavourites = _services.value.associateBy({ it.id }, { it.isFavourite })
                val mergedList = serviceList.map { svc ->
                    svc.copy(
                        isFavourite = currentFavourites[svc.id] ?: svc.isFavourite,
                        rating = (svc.rating.toFloatOrNull() ?: 0f).toString(),
                        reviewCount = svc.reviewCount ?: 0
                    )
                }

                allServices = mergedList
                _services.value = mergedList
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching services", e)
                _services.value = emptyList()
            }
        }
    }

    // --- Load service details ---
    fun loadServiceDetails(serviceId: String) {
        viewModelScope.launch {
            try {
                val details = repo.fetchServiceDetails(userToken, serviceId)
                _serviceDetail.value = details
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching service details", e)
                _serviceDetail.value = null
            }
        }
    }

    // --- Load user favourites ---
    fun loadUserFavourites() {
        viewModelScope.launch {
            try {
                val favResponse = repo.getUserFavourites(userToken)
                val favouriteIds = favResponse ?: emptyList()

                val updatedServices = allServices.map { svc ->
                    svc.copy(isFavourite = favouriteIds.contains(svc.id))
                }

                _services.value = updatedServices
            } catch (e: Exception) {
                Log.e(TAG, "Error loading favourites", e)
            }
        }
    }

    // --- Toggle favourite with optimistic update ---
    fun toggleFavourite(service: Service) {
        viewModelScope.launch {
            val currentList = _services.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == service.id }
            if (index == -1) return@launch

            val previous = currentList[index]
            val toggled = previous.copy(isFavourite = !previous.isFavourite)

            // Optimistic update
            currentList[index] = toggled
            _services.value = currentList

            try {
                val success = repo.toggleFavourite(userToken, service.id)

                if (!success) {
                    // Rollback on failure
                    currentList[index] = previous
                    _services.value = currentList
                } else {
                    // Update allServices as well
                    allServices = allServices.map { svc ->
                        if (svc.id == service.id) svc.copy(isFavourite = toggled.isFavourite) else svc
                    }
                }
            } catch (e: Exception) {
                // Rollback on exception
                currentList[index] = previous
                _services.value = currentList
                Log.e(TAG, "Error toggling favourite", e)
            }
        }
    }

    // --- Submit review ---
    fun addReview(serviceId: String, rating: Double, comment: String?) {
        viewModelScope.launch {
            try {
                val response = repo.submitReview(userToken, SubmitReviewRequest(serviceId, rating, comment))

                if (response?.success == true) {
                    val updatedList = _services.value.map { svc ->
                        if (svc.id == serviceId) {
                            val newRating = response.averageRating?.toFloatOrNull() ?: svc.rating.toFloat() ?: 0f
                            svc.copy(rating = newRating.toString())
                        } else svc
                    }
                    _services.value = updatedList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting review", e)
            }
        }
    }

    // --- Load reviews ---
    fun loadReviews(serviceId: String) {
        viewModelScope.launch {
            try {
                val result = repo.fetchReviews(userToken, serviceId)
                _reviews.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reviews", e)
                _reviews.value = emptyList()
            }
        }
    }

    // --- Get favourites locally ---
    fun getFavourites(): List<Service> = _services.value.filter { it.isFavourite }
}
