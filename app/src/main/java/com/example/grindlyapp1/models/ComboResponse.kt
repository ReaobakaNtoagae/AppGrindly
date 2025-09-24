package com.example.grindlyapp1.models

data class ComboResponse(
    val services: List<Service> = emptyList(),
    val hustlers: List<HustlerProfile> = emptyList()
)
