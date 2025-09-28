package com.example.grindlyapp1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers

class FavouritesViewModel(private val repository: FavouritesRepository) : ViewModel() {

    fun toggleFavourite(token: String, serviceId: String) = liveData(Dispatchers.IO) {
        val response = repository.toggleFavourite(token, serviceId)
        emit(response)
    }

    fun getFavourites(token: String) = liveData(Dispatchers.IO) {
        val response = repository.getFavourites(token)
        emit(response)
    }
}
