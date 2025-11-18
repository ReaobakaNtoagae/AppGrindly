package com.example.grindlyapp1.viewmodelfactory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.grindlyapp1.repository.FavouritesRepository
import com.example.grindlyapp1.repository.ServiceRepository
import com.example.grindlyapp1.viewmodel.FavouritesViewModel
import com.example.grindlyapp1.viewmodel.ServiceViewModel

/**
 * Factory to provide FavouritesViewModel and ServiceViewModel with required dependencies.
 *
 * Assumes you have already created ServiceRepository and FavouritesRepository instances.
 */
class FavouritesViewModelFactory(
    private val favouritesRepo: FavouritesRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavouritesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavouritesViewModel(favouritesRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

