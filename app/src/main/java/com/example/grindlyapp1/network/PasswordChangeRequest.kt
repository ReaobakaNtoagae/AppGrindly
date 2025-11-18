package com.example.grindlyapp1.network


// Network models
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
    // No userId needed - it comes from token
)

data class ToggleRequest(
    val enable: Boolean
    // No userId needed - it comes from token
)

