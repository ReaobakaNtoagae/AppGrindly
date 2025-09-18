package com.example.grindlyapp1

data class PasswordChangeRequest(
    val userId: String,
    val oldPassword: String,
    val newPassword: String
)
