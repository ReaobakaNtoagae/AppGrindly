package com.example.grindlyapp1.models

import com.example.grindlyapp1.network.Review

data class ReviewResponse(
    val success: Boolean,
    val reviews: List<Review>
)

