package com.example.grindlyapp1.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.grindlyapp1.repository.ServiceRepository
import com.example.grindlyapp1.viewmodels.ServiceViewModel


class ServiceViewModelFactory(
    private val repository: ServiceRepository,
    private val userToken: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServiceViewModel::class.java)) {


            return ServiceViewModel(repository, userToken) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
