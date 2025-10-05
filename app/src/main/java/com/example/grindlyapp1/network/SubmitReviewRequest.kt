package com.example.grindlyapp1.network


data class SubmitReviewRequest(
    val serviceId: String,
    val rating: Int,
    val comment: String? = null
)