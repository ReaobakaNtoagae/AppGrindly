package com.example.grindlyapp1.viewmodelfactory

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.grindlyapp1.network.ApiService
import com.example.grindlyapp1.viewmodel.SettingsViewModel

class SettingsVMFactory(
    private val prefs: SharedPreferences,
    private val api: ApiService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(prefs, api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
