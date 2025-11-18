package com.example.grindlyapp1.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grindlyapp1.FavouriteEntity
import com.example.grindlyapp1.network.ComboResponse
import com.example.grindlyapp1.network.Review
import com.example.grindlyapp1.network.Service
import com.example.grindlyapp1.network.SubmitReviewRequest
import com.example.grindlyapp1.repository.FavouritesRepository
import com.example.grindlyapp1.repository.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServiceViewModel(
    private val appContext: Context,
    private val serviceRepo: ServiceRepository,
    private val favouritesRepo: FavouritesRepository,
    private val userToken: String
) : ViewModel() {

    companion object {
        private const val TAG = "ServiceViewModel"
    }


    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> get() = _services

    private val _serviceDetail = MutableStateFlow<ComboResponse?>(null)
    val serviceDetail: StateFlow<ComboResponse?> get() = _serviceDetail

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> get() = _reviews

    private val _unsyncedFavourites = MutableStateFlow<List<FavouriteEntity>>(emptyList())
    val unsyncedFavourites: StateFlow<List<FavouriteEntity>> get() = _unsyncedFavourites

    private var allServices: List<Service> = emptyList()

    private val networkMonitor = NetworkMonitor(appContext)
    private val _isOnline = MutableStateFlow(checkInitialNetwork())
    val isOnline: StateFlow<Boolean> get() = _isOnline

    init {
        observeNetwork()
        refreshUnsynced()
        loadServicesList()
    }

    // -------------------------------
    // NETWORK MONITOR
    // -------------------------------
    private fun observeNetwork() {
        viewModelScope.launch(Dispatchers.IO) {
            networkMonitor.isOnline.collect { online ->
                val wasOnline = _isOnline.value
                _isOnline.value = online

                if (!wasOnline && online) {
                    Log.d(TAG, "Network returned — attempting sync.")
                    syncNow()
                }
            }
        }
    }

    private fun checkInitialNetwork(): Boolean = networkMonitor.isOnline.value

    // -------------------------------
    // SERVICES LOADING
    // -------------------------------

    fun loadServicesList() {
        viewModelScope.launch {
            try {

                val serviceList = withContext(Dispatchers.IO) {

                    serviceRepo.fetchServicesWithCache(userToken)

                }


                val localFavs = withContext(Dispatchers.IO) {

                    favouritesRepo.getLocalFavourites()

                }
                val favMap = localFavs.associate { it.serviceId to it.isFavourite }

                val mergedList = serviceList.map { svc ->
                    svc.copy(
                        isFavourite = favMap[svc.id] ?: svc.isFavourite,
                        rating = (svc.rating.toFloatOrNull() ?: 0f).toString(),
                        reviewCount = svc.reviewCount ?: 0
                    )
                }

                allServices = mergedList
                _services.value = mergedList
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching services", e)
                if (_services.value.isEmpty()) _services.value = emptyList()
            }
        }
    }

    fun loadServiceDetails(serviceId: String) {
        viewModelScope.launch {
            try {
                val details = withContext(Dispatchers.IO) {
                    serviceRepo.fetchServiceDetails(userToken, serviceId)
                }
                _serviceDetail.value = details
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching service details for $serviceId", e)
                _serviceDetail.value = null
            }
        }
    }

    fun loadReviews(serviceId: String) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    serviceRepo.fetchReviews(userToken, serviceId)
                }
                _reviews.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reviews for $serviceId", e)
                _reviews.value = emptyList()
            }
        }
    }

    fun loadUserFavourites() {
        viewModelScope.launch {
            try {
                val localFavourites = withContext(Dispatchers.IO) { favouritesRepo.getLocalFavourites() }
                val favouriteIds = localFavourites.filter { it.isFavourite }.map { it.serviceId }.toSet()
                _services.value = allServices.map { svc ->
                    svc.copy(isFavourite = favouriteIds.contains(svc.id))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user favourites", e)
            }
        }
    }



    fun addReview(serviceId: String, rating: Double, comment: String?) {
        viewModelScope.launch {
            try {
                val request = SubmitReviewRequest(serviceId, rating, comment)
                val response = withContext(Dispatchers.IO) { serviceRepo.submitReview(userToken, request) }

                // Use your actual ApiResponse type
                if (response != null && response.success) {  // adapt field names if needed
                    response.averageRating?.let { avg ->
                        _services.value = _services.value.map { svc ->
                            if (svc.id == serviceId) svc.copy(rating = avg.toString()) else svc
                        }
                    }
                    loadReviews(serviceId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting review for $serviceId", e)
            }
        }

}


    fun toggleFavourite(service: Service) {
        viewModelScope.launch {
            try {
                val online = _isOnline.value
                withContext(Dispatchers.IO) { favouritesRepo.toggleFavourite(userToken, service.id, online) }

                _services.value = _services.value.map { svc ->
                    if (svc.id == service.id) svc.copy(isFavourite = !svc.isFavourite) else svc
                }

                refreshUnsynced()
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favourite for ${service.id}", e)
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            if (!_isOnline.value) {
                Log.d(TAG, "Sync requested but currently offline — skipping.")
                return@launch
            }
            try {
                withContext(Dispatchers.IO) { favouritesRepo.syncUnsynced(userToken) }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing favourites", e)
            } finally {
                refreshUnsynced()
            }
        }
    }

    private fun refreshUnsynced() {
        viewModelScope.launch {
            try {
                val local = withContext(Dispatchers.IO) { favouritesRepo.getLocalFavourites() }
                _unsyncedFavourites.value = local.filter { !it.isSynced }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading local favourites for unsynced state", e)
                _unsyncedFavourites.value = emptyList()
            }
        }
    }

    // -------------------------------
    // UTILITY GETTERS
    // -------------------------------
    fun getFavourites(): List<Service> = _services.value.filter { it.isFavourite }
    fun getUnsyncedEntities(): List<FavouriteEntity> = _unsyncedFavourites.value

    // -------------------------------
    // NETWORK MONITOR INNER CLASS
    // -------------------------------
    class NetworkMonitor(context: Context) {
        private val connectivity = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        private val _isOnline = MutableStateFlow(checkOnline(connectivity))
        val isOnline: StateFlow<Boolean> get() = _isOnline

        init {
            try {
                val request = android.net.NetworkRequest.Builder().build()
                connectivity.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        _isOnline.value = true
                    }
                    override fun onLost(network: android.net.Network) {
                        _isOnline.value = checkOnline(connectivity)
                    }
                })
            } catch (e: Exception) {
                _isOnline.value = checkOnline(connectivity)
            }
        }

        private fun checkOnline(connectivity: ConnectivityManager): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivity.activeNetwork ?: return false
                    val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } else {
                    @Suppress("DEPRECATION")
                    val networkInfo = connectivity.activeNetworkInfo
                    networkInfo?.isConnectedOrConnecting == true
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
