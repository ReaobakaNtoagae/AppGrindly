package com.example.grindlyapp1.network

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val biometricsEnabled: Boolean = true,
    val language: String = "English",
    val loading: Boolean = false,
    val message: String? = null,
)

