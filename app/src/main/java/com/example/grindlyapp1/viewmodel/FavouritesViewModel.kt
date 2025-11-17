package com.example.grindlyapp1.viewmodels

import android.content.Context
import androidx.lifecycle.*
import com.example.grindlyapp1.network.FavouriteItem
import com.example.grindlyapp1.network.Service
import com.example.grindlyapp1.repository.FavouritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavouritesViewModel(
    private val serviceViewModel: ServiceViewModel
) : ViewModel() {

    private val repo = FavouritesRepository()

    private val _favourites = MediatorLiveData<List<Service>>()
    val favourites: LiveData<List<Service>> get() = _favourites

    private var cachedFavouriteIds: List<String> = emptyList()
    private var backendFavourites: List<Service> = emptyList()

    init {

        viewModelScope.launch {
        serviceViewModel.services.collect { services ->
            _favourites.postValue(mergeFavourites(services, cachedFavouriteIds, backendFavourites))
        }
    }

    }

    fun fetchFavourites(context: Context, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repo.getFavourites(token)
                val favItems: List<FavouriteItem> = response.body()?.favourites ?: emptyList()

                // Cache favourite IDs
                cachedFavouriteIds = favItems.mapNotNull { it.serviceId }

                // Cache backend service objects marked as favourite
                backendFavourites = favItems.mapNotNull { it.service?.copy(isFavourite = true) }

                withContext(Dispatchers.Main) {
                    val allServices = serviceViewModel.services.value ?: emptyList()
                    _favourites.value = mergeFavourites(allServices, cachedFavouriteIds, backendFavourites)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { _favourites.value = emptyList() }
            }
        }
    }

    fun toggleFavourite(context: Context, token: String, service: Service) {
        val currentList = _favourites.value?.toMutableList() ?: mutableListOf()
        val newStatus = !service.isFavourite

        if (newStatus) {
            currentList.add(service.copy(isFavourite = true))
        } else {
            currentList.removeAll { it.id == service.id }
        }

        _favourites.value = currentList.distinctBy { it.id }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.toggleFavourite(token, service.id)
                // Optionally refresh from backend
                fetchFavourites(context, token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun mergeFavourites(
        allServices: List<Service>,
        favouriteIds: List<String>,
        backendServices: List<Service>
    ): List<Service> {
        val favFromAll = allServices.map { service ->
            val isFav = favouriteIds.contains(service.id)
            service.copy(isFavourite = isFav)
        }.filter { it.isFavourite }

        return if (favFromAll.isNotEmpty()) favFromAll else backendServices
    }
}
