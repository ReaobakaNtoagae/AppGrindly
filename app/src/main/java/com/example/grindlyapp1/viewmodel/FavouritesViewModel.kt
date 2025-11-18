package com.example.grindlyapp1.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.*
import com.example.grindlyapp1.repository.FavouritesRepository
import kotlinx.coroutines.launch
import com.example.grindlyapp1.network.Service

class FavouritesViewModel(private val repository: FavouritesRepository) : ViewModel() {

    private val _favourites = MutableLiveData<List<Service>>()
    val favourites: LiveData<List<Service>> get() = _favourites

    fun loadFavourites(context: Context, userToken: String, allServices: List<Service>) {
        viewModelScope.launch {
            val isOnline = isOnline(context)

            if (isOnline) {
                repository.syncUnsynced(userToken)
            }

            val localFavourites = repository.getLocalFavourites()
            val updatedList = allServices.map { service ->
                service.copy(isFavourite = localFavourites.any { it.serviceId == service.id })
            }

            _favourites.postValue(updatedList)
        }
    }

    fun toggleFavourite(context: Context, token: String, serviceId: String) {
        viewModelScope.launch {
            val isOnline = isOnline(context)
            repository.toggleFavourite(token, serviceId, isOnline)

            val currentList = _favourites.value ?: emptyList()
            loadFavourites(context, token, currentList)
        }
    }


    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = cm.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }
}