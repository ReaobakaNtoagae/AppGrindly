package com.example.grindlyapp1.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grindlyapp1.network.HustlerProfile
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.network.VerifyHustlerRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    // ---------- UI State ----------
    private val _hustlers = MutableStateFlow<List<HustlerProfile>>(emptyList())
    val hustlers: StateFlow<List<HustlerProfile>> get() = _hustlers

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> get() = _toastMessage

    // ---------- Fetch Pending Hustlers ----------
    fun fetchPendingHustlers(context: Context, token: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val api = RetrofitClient.getClient(context)
                val response = api.getPendingHustlers("Bearer $token")

                Log.d("AdminViewModel", "Response code: ${response.code()}")
                Log.d("AdminViewModel", "Response body: ${response.body()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    _hustlers.value = response.body()!!.hustlers
                    Log.d("AdminViewModel", "Fetched ${_hustlers.value.size} hustlers")
                } else {
                    _toastMessage.value = "Failed to fetch pending hustlers"
                }

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Exception during fetch", e)
                _toastMessage.value = "Error: ${e.localizedMessage ?: e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // ---------- Verify or Reject Hustler ----------
    fun verifyHustler(context: Context, token: String, hustlerId: String, action: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val api = RetrofitClient.getClient(context)
                val response = api.verifyHustler("Bearer $token", VerifyHustlerRequest(hustlerId, action))

                if (response.isSuccessful && response.body()?.success == true) {
                    _toastMessage.value = "Hustler ${action.uppercase()} successful"
                    _hustlers.value = _hustlers.value.filterNot { it.hustlerId == hustlerId }
                } else {
                    _toastMessage.value = "Failed to ${action.lowercase()} hustler"
                }

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error during verifyHustler", e)
                _toastMessage.value = "Error: ${e.localizedMessage ?: e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // ---------- Clear toast message ----------
    fun clearToast() {
        _toastMessage.value = null
    }
}
