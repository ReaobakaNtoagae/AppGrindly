package com.example.grindlyapp1.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiService {
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>
}
