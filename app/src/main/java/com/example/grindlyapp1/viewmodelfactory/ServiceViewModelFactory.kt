package com.example.grindlyapp1.viewmodelfactory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.grindlyapp1.repository.FavouritesRepository
import com.example.grindlyapp1.repository.ServiceRepository
import com.example.grindlyapp1.viewmodel.ServiceViewModel

class ServiceViewModelFactory(
    private val context: Context,
    private val serviceRepo: ServiceRepository,
    private val favouritesRepo: FavouritesRepository,
    private val userToken: String
) : ViewModelProvider.Factory
{

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServiceViewModel::class.java)) {
            return ServiceViewModel(context, serviceRepo, favouritesRepo, userToken) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

}
