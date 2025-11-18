package com.example.grindlyapp1.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grindlyapp1.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class SettingsViewModel(
    private val prefs: SharedPreferences,
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val token: String? = prefs.getString("TOKEN", null)

    init {
        loadSavedPreferences()
    }

    private fun loadSavedPreferences() {
        _uiState.update {
            it.copy(
                notificationsEnabled = prefs.getBoolean("NOTIFICATIONS_ENABLED", true),
                biometricsEnabled = prefs.getBoolean("BIOMETRICS_ENABLED", true),
                language = prefs.getString("LANGUAGE_PREF", "English") ?: "English"
            )
        }
    }

    // === Language Settings ===
    fun updateLanguage(language: String) {
        prefs.edit { putString("LANGUAGE_PREF", language) }
        _uiState.update { it.copy(language = language) }
        showTemporaryMessage("Language updated to $language")
    }

    // === Notifications Toggle ===
    fun toggleNotifications(enable: Boolean) {
        if (token.isNullOrEmpty()) {
            showTemporaryMessage("Authentication required")
            return
        }

        _uiState.update { it.copy(loading = true) }

        api.toggleNotifications("Bearer $token", ToggleRequest(enable))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    _uiState.update { it.copy(loading = false) }

                    if (response.isSuccessful && response.body() != null) {
                        prefs.edit { putBoolean("NOTIFICATIONS_ENABLED", enable) }
                        _uiState.update { it.copy(notificationsEnabled = enable) }
                        showTemporaryMessage("Notifications ${if (enable) "enabled" else "disabled"}")
                    } else {
                        val errorMsg = parseErrorMessage(response)
                        showTemporaryMessage("Failed to update notifications: $errorMsg")
                        // Revert UI state on failure
                        _uiState.update { it.copy(notificationsEnabled = !enable) }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    _uiState.update { it.copy(loading = false) }
                    showTemporaryMessage("Network error: ${t.message}")
                    // Revert UI state on failure
                    _uiState.update { it.copy(notificationsEnabled = !enable) }
                }
            })
    }

    // === Biometrics Toggle ===
    fun toggleBiometrics(enable: Boolean) {
        if (token.isNullOrEmpty()) {
            showTemporaryMessage("Authentication required")
            return
        }

        _uiState.update { it.copy(loading = true) }

        api.toggleBiometrics("Bearer $token", ToggleRequest(enable))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    _uiState.update { it.copy(loading = false) }

                    if (response.isSuccessful && response.body() != null) {
                        prefs.edit { putBoolean("BIOMETRICS_ENABLED", enable) }
                        _uiState.update { it.copy(biometricsEnabled = enable) }
                        showTemporaryMessage("Biometrics ${if (enable) "enabled" else "disabled"}")
                    } else {
                        val errorMsg = parseErrorMessage(response)
                        showTemporaryMessage("Failed to update biometrics: $errorMsg")
                        // Revert UI state on failure
                        _uiState.update { it.copy(biometricsEnabled = !enable) }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    _uiState.update { it.copy(loading = false) }
                    showTemporaryMessage("Network error: ${t.message}")
                    // Revert UI state on failure
                    _uiState.update { it.copy(biometricsEnabled = !enable) }
                }
            })
    }

    // === Change Password ===
    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit = {}) {
        if (token.isNullOrEmpty()) {
            showTemporaryMessage("Authentication required")
            return
        }

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            showTemporaryMessage("Please fill in all fields")
            return
        }

        _uiState.update { it.copy(loading = true) }

        api.changePassword("Bearer $token", ChangePasswordRequest(oldPassword, newPassword))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    _uiState.update { it.copy(loading = false) }

                    if (response.isSuccessful && response.body() != null) {
                        // Remove saved password from prefs (affects biometric login)
                        prefs.edit { remove("USER_PASSWORD") }
                        showTemporaryMessage("Password updated successfully")
                        onSuccess()
                    } else {
                        val errorMsg = parseErrorMessage(response)
                        showTemporaryMessage("Password change failed: $errorMsg")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    _uiState.update { it.copy(loading = false) }
                    showTemporaryMessage("Network error: ${t.message}")
                }
            })
    }

    // === Delete Account ===
    fun deleteAccount(onSuccess: () -> Unit) {
        if (token.isNullOrEmpty()) {
            showTemporaryMessage("Authentication required")
            return
        }

        _uiState.update { it.copy(loading = true, showDeleteConfirmation = false) }

        api.deleteAccount("Bearer $token")
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    _uiState.update { it.copy(loading = false) }

                    if (response.isSuccessful && response.body() != null) {
                        // Clear all user data from SharedPreferences
                        prefs.edit { clear() }
                        showTemporaryMessage("Account deleted successfully")
                        onSuccess()
                    } else {
                        val errorMsg = parseErrorMessage(response)
                        showTemporaryMessage("Account deletion failed: $errorMsg")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    _uiState.update { it.copy(loading = false) }
                    showTemporaryMessage("Network error: ${t.message}")
                }
            })
    }

    // === Logout ===
    fun logout(onComplete: () -> Unit) {
        if (token.isNullOrEmpty()) {
            // If no token, just clear prefs and complete
            prefs.edit { clear() }
            onComplete()
            return
        }

        _uiState.update { it.copy(loading = true) }

        api.logout("Bearer $token").enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                prefs.edit { clear() }
                _uiState.update { it.copy(loading = false) }
                showTemporaryMessage("Logged out successfully")
                onComplete()
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                // Still clear prefs even if network call fails
                prefs.edit { clear() }
                _uiState.update { it.copy(loading = false) }
                showTemporaryMessage("Logged out successfully")
                onComplete()
            }
        })
    }

    // === Delete Account Confirmation ===
    fun showDeleteConfirmation(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmation = show) }
    }

    // === Clear Message ===
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // === Private Helper Functions ===
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            response.errorBody()?.string() ?: "Unknown error occurred"
        } catch (e: IOException) {
            "Error parsing response"
        } catch (e: Exception) {
            "Unknown error"
        }
    }

    private fun showTemporaryMessage(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(message = message) }
            delay(3000) // Show message for 3 seconds
            _uiState.update { it.copy(message = null) }
        }
    }
}