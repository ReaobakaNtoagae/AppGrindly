package com.example.grindlyapp1.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.grindlyapp1.FavouriteRequest
import com.example.grindlyapp1.RetrofitInstance
import com.example.grindlyapp1.models.ComboResponse
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.models.HustlerProfile
import com.example.grindlyapp1.repository.ServiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServiceViewModel : ViewModel() {

    private val repo = ServiceRepository()

    // --- Services ---
    private val _services = MutableLiveData<List<Service>>(emptyList())
    val services: LiveData<List<Service>> get() = _services

    private val _serviceDetail = MutableLiveData<ComboResponse?>()
    val serviceDetail: LiveData<ComboResponse?> get() = _serviceDetail

    private val _hustlers = MutableLiveData<List<HustlerProfile>>()
    val hustlers: LiveData<List<HustlerProfile>> get() = _hustlers

    private var allServices: List<Service> = emptyList()

    // --- Load all services ---
    fun loadServicesList() {
        viewModelScope.launch {
            try {
                Log.d("ServiceViewModel", "Fetching services...")
                val serviceList = repo.fetchServices()

                // Merge favourites from current LiveData
                val currentFavourites = _services.value?.associateBy({ it.id }, { it.isFavourite }) ?: emptyMap()
                val mergedList = serviceList.map { svc ->
                    val isFav = currentFavourites[svc.id] ?: svc.isFavourite
                    svc.copy(isFavourite = isFav)
                }

                Log.d("ServiceViewModel", "Services fetched: ${mergedList.size}")
                mergedList.forEach { Log.d("ServiceViewModel", "Service: ${it.title}, isFavourite=${it.isFavourite}") }

                allServices = mergedList
                _services.postValue(mergedList)
            } catch (e: Exception) {
                Log.e("ServiceViewModel", "Error fetching services", e)
                _services.postValue(emptyList())
            }
        }
    }


    // --- Load service details ---
    fun loadServiceDetails(id: String) {
        viewModelScope.launch {
            try {
                val details = repo.fetchServiceDetails(id)
                _serviceDetail.postValue(details)
            } catch (e: Exception) {
                e.printStackTrace()
                _serviceDetail.postValue(null)
            }
        }
    }

    // --- Favourites ---
    fun loadUserFavourites(userToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = "Bearer $userToken"
                val favResponse = RetrofitInstance.api.getFavourites(token)
                val favouriteIds = if (favResponse.isSuccessful) {
                    favResponse.body()?.favourites ?: emptyList()
                } else emptyList()

                // Merge favourites with current allServices list
                val updatedServices = allServices.map { service ->
                    service.copy(isFavourite = favouriteIds.contains(service.id))
                }

                // Update LiveData safely
                withContext(Dispatchers.Main) {
                    Log.d("ServiceViewModel", "User favourites loaded: $favouriteIds")
                    _services.value = updatedServices
                }
            } catch (e: Exception) {
                Log.e("ServiceViewModel", "Error loading favourites", e)
            }
        }
    }


    /**
     * Toggle favourite for a service.
     * - Performs optimistic update on LiveData first.
     * - Calls API to persist the change.
     * - Rolls back if API fails or throws.
     */
    fun toggleFavourite(service: Service, userToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = _services.value?.toMutableList() ?: return@launch
                val index = currentList.indexOfFirst { it.id == service.id }
                if (index == -1) {
                    Log.w("ServiceViewModel", "Service not found in list: ${service.id}")
                    return@launch
                }

                val previous = currentList[index]
                val toggled = previous.copy(isFavourite = !previous.isFavourite)
                Log.d("ServiceViewModel", "Toggling favourite for ${service.title}: ${previous.isFavourite} -> ${toggled.isFavourite}")

                // Optimistic update
                currentList[index] = toggled
                withContext(Dispatchers.Main) {
                    _services.value = currentList
                    Log.d("ServiceViewModel", "LiveData updated optimistically for ${service.title}")
                }

                // API call
                val token = "Bearer $userToken"
                val request = FavouriteRequest(serviceId = service.id)
                val response = RetrofitInstance.api.toggleFavourite(token, request)

                if (!response.isSuccessful) {
                    Log.e("ServiceViewModel", "API toggleFavourite failed for ${service.title}, rolling back")
                    withContext(Dispatchers.Main) {
                        val rollback = _services.value?.toMutableList() ?: return@withContext
                        rollback[index] = previous
                        _services.value = rollback
                    }
                } else {
                    Log.d("ServiceViewModel", "API toggleFavourite successful for ${service.title}")
                }
            } catch (e: Exception) {
                Log.e("ServiceViewModel", "Exception toggling favourite for ${service.title}, rolling back", e)
                withContext(Dispatchers.Main) {
                    val rollbackList = _services.value?.toMutableList() ?: return@withContext
                    val index = rollbackList.indexOfFirst { it.id == service.id }
                    if (index != -1) {
                        val prev = allServices.find { it.id == service.id } ?: rollbackList[index]
                        rollbackList[index] = prev
                        _services.value = rollbackList
                    }
                }
            }
        }
    }

    fun updateFavouriteState(updatedService: Service) {
        Log.d("ServiceViewModel", "Updating favourite state for ${updatedService.title} to ${updatedService.isFavourite}")
        val currentList = _services.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == updatedService.id }
        if (index == -1) return
        currentList[index] = updatedService
        _services.value = currentList
    }

    fun getFavourites(): List<Service> {
        return _services.value?.filter { it.isFavourite } ?: emptyList()
    }
}


