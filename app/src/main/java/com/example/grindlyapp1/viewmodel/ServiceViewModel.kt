package com.example.grindlyapp1.viewmodels


import android.R.attr.data
import android.util.Log
import androidx.lifecycle.*
import com.example.grindlyapp1.models.ComboResponse
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.models.HustlerProfile
import com.example.grindlyapp1.repository.ServiceRepository
import kotlinx.coroutines.launch

class ServiceViewModel : ViewModel() {

    private val repo = ServiceRepository()

    private val _services = MutableLiveData<List<Service>>()
    val services: LiveData<List<Service>> get() = _services

    private val _hustlers = MutableLiveData<List<HustlerProfile>>()
    val hustlers: LiveData<List<HustlerProfile>> get() = _hustlers

    private val _serviceDetail = MutableLiveData<ComboResponse?>()
    val serviceDetail: LiveData<ComboResponse?> get() = _serviceDetail


    fun loadServicesList() {
        viewModelScope.launch {
            try {
                Log.d("ServiceViewModel", "Fetching services...")
                val serviceList = repo.fetchServices()
                Log.d("ServiceViewModel", "Services fetched: ${serviceList.size}")
                serviceList.forEach { Log.d("ServiceViewModel", "Service: ${it.title}, Category: ${it.category}") }

                _services.postValue(serviceList)

            } catch (e: Exception) {
                Log.e("ServiceViewModel", "Error fetching services", e)
                _services.postValue(emptyList())
            }
        }
    }


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
}
