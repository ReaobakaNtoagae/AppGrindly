package com.example.grindlyapp1.network


data class SettingsUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val notificationsEnabled: Boolean = true,
    val biometricsEnabled: Boolean = true,
    val language: String = "English",
    val showDeleteConfirmation: Boolean = false
)
