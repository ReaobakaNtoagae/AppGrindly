package com.example.grindlyapp1.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grindlyapp1.models.*
import com.example.grindlyapp1.network.ApiService
import com.example.grindlyapp1.network.Booking
import com.example.grindlyapp1.network.BookingRequest
import com.example.grindlyapp1.network.BookingStatusUpdateRequest
import com.example.grindlyapp1.network.BookingStatusUpdateResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookingViewModel(
    private val apiService: ApiService
) : ViewModel() {

    // ---------- UI State ----------
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    // Booking lists
    private val _clientBookings = MutableStateFlow<List<Booking>>(emptyList())
    val clientBookings: StateFlow<List<Booking>> get() = _clientBookings

    private val _hustlerBookings = MutableStateFlow<List<Booking>>(emptyList())
    val hustlerBookings: StateFlow<List<Booking>> get() = _hustlerBookings

    // Single booking
    private val _bookingDetails = MutableStateFlow<Booking?>(null)
    val bookingDetails: StateFlow<Booking?> get() = _bookingDetails

    private val _statusUpdateResponse = MutableStateFlow<BookingStatusUpdateResponse?>(null)
    val statusUpdateResponse: StateFlow<BookingStatusUpdateResponse?> get() = _statusUpdateResponse


    // ---------- Fetch Client Bookings ----------
    fun loadClientBookings(token: String, clientId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getClientBookings("Bearer $token", clientId)

                if (response.isSuccessful) {
                    _clientBookings.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load client bookings"
                }

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }


    // ---------- Fetch Hustler Bookings ----------
    fun loadHustlerBookings(token: String, hustlerId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getHustlerBookings("Bearer $token", hustlerId)

                if (response.isSuccessful) {
                    _hustlerBookings.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load hustler bookings"
                }

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun getBookingById(token: String, bookingId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getBookingById("Bearer $token", bookingId)

                if (response.isSuccessful) {
                    _bookingDetails.value = response.body()?.booking
                } else {
                    _error.value = "Failed to load booking details"
                }

            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun updateBookingStatus(token: String, bookingId: String, status: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val request = BookingStatusUpdateRequest(status)
                val response = apiService.updateBookingStatus("Bearer $token", bookingId, request)

                if (response.isSuccessful) {
                    _statusUpdateResponse.value = response.body()
                } else {
                    _error.value = "Failed to update status"
                }

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun createBooking(token: String, bookingRequest: BookingRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.createBooking("Bearer $token", bookingRequest)

                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    _error.value = "Failed to create booking"
                }

            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

}
