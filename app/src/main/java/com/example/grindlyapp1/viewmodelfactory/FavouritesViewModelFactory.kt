package com.example.grindlyapp1.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.grindlyapp1.viewmodels.FavouritesViewModel
import com.example.grindlyapp1.viewmodels.ServiceViewModel

class FavouritesViewModelFactory(
    private val serviceViewModel: ServiceViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavouritesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavouritesViewModel(serviceViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
