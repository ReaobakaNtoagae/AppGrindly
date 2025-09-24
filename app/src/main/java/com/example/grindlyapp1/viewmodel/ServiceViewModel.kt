package com.example.grindlyapp1.viewmodels

import androidx.lifecycle.*
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.models.HustlerProfile
import com.example.grindlyapp1.models.ComboResponse
import com.example.grindlyapp1.repository.ServiceRepository
import kotlinx.coroutines.launch

class ServiceViewModel : ViewModel() {

    private val repo = ServiceRepository()

    private val _services = MutableLiveData<List<Service>>()
    val services: LiveData<List<Service>> get() = _services

    private val _hustlers = MutableLiveData<List<HustlerProfile>>()
    val hustlers: LiveData<List<HustlerProfile>> get() = _hustlers

    private val _serviceDetails = MutableLiveData<HustlerProfile?>()
    val serviceDetails: LiveData<HustlerProfile?> get() = _serviceDetails

    fun loadCombo() {
        viewModelScope.launch {
            try {
                val combo = repo.fetchCombo()
                _services.postValue(combo?.services ?: emptyList())
                _hustlers.postValue(combo?.hustlers ?: emptyList())
            } catch (e: Exception) {
                e.printStackTrace()
                _services.postValue(emptyList())
                _hustlers.postValue(emptyList())
            }
        }
    }

    fun loadServiceDetails(id: String) {
        viewModelScope.launch {
            try {
                val details = repo.fetchServiceDetails(id)
                _serviceDetails.postValue(details)
            } catch (e: Exception) {
                e.printStackTrace()
                _serviceDetails.postValue(null)
            }
        }
    }
}
