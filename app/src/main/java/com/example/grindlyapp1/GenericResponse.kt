package com.example.grindlyapp1

data class GenericResponse(
    val message: String? = null,
    val error: String? = null,
    val success: Boolean
)