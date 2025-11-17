package com.example.grindlyapp1.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grindlyapp1.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsViewModel(
    private val prefs: SharedPreferences,
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val token = prefs.getString("TOKEN", null)
    private val userId = prefs.getString("USER_ID", null)

    init {
        loadSavedPrefs()
    }

    private fun loadSavedPrefs() {
        _uiState.update {
            it.copy(
                notificationsEnabled = prefs.getBoolean("NOTIFICATIONS_ENABLED", true),
                biometricsEnabled = prefs.getBoolean("BIOMETRICS_ENABLED", true),
                language = prefs.getString("LANGUAGE_PREF", "English") ?: "English"
            )
        }
    }

    // --- User actions ---

    fun updateLanguage(language: String) {
        prefs.edit { putString("LANGUAGE_PREF", language) }
        _uiState.update { it.copy(language = language) }
        showMessage("Language updated to $language")
    }

    fun toggleNotifications(enable: Boolean) {
        safeApiCall(
            call = { api.toggleNotifications("Bearer $token", mapOf("enable" to enable)) },
            onSuccess = {
                prefs.edit { putBoolean("NOTIFICATIONS_ENABLED", enable) }
                _uiState.update { it.copy(notificationsEnabled = enable) }
                showMessage("Notifications updated")
            },
            onError = { showMessage("Failed to update notifications") }
        )
    }

    fun toggleBiometrics(enable: Boolean) {
        safeApiCall(
            call = { api.toggleBiometrics("Bearer $token", mapOf("enable" to enable)) },
            onSuccess = {
                prefs.edit { putBoolean("BIOMETRICS_ENABLED", enable) }
                _uiState.update { it.copy(biometricsEnabled = enable) }
                showMessage("Biometrics updated")
            },
            onError = { showMessage("Failed to update biometrics") }
        )
    }

    fun changePassword(oldPass: String, newPass: String) {
        safeApiCall(
            call = { api.changePassword("Bearer $token", PasswordChangeRequest(userId!!, oldPass, newPass)) },
            onSuccess = {
                prefs.edit { clear() } // Log out after password change
                showMessage("Password updated. Please log in again.")
            },
            onError = { showMessage("Password update failed") }
        )
    }

    fun deleteAccount() {
        safeApiCall(
            call = { api.deleteAccount("Bearer $token", userId!!) },
            onSuccess = {
                prefs.edit { clear() }
                showMessage("Account deleted")
            },
            onError = { showMessage("Failed to delete account") }
        )
    }

    // --- Logout without deleting account ---
    fun logout(onComplete: () -> Unit) {
        token?.let { tkn ->
            api.logout("Bearer $tkn").enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    prefs.edit { clear() }
                    showMessage("Logged out successfully")
                    onComplete()
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    prefs.edit { clear() }
                    showMessage("Logged out (network error)")
                    onComplete()
                }
            })
        } ?: run {
            prefs.edit { clear() }
            showMessage("Logged out successfully")
            onComplete()
        }
    }

    // --- Helper functions ---

    private fun safeApiCall(
        call: () -> Call<GenericResponse>,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        _uiState.update { it.copy(loading = true) }

        call().enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                _uiState.update { it.copy(loading = false) }
                if (response.isSuccessful) onSuccess() else onError()
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                _uiState.update { it.copy(loading = false, message = "Network error: ${t.message}") }
                onError()
            }
        })
    }

    private fun showMessage(msg: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(message = msg) }
            delay(2000) // Keep message visible for 2 seconds
            _uiState.update { it.copy(message = null) }
        }
    }
}
