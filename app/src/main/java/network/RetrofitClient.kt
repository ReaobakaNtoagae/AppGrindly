package network

import network.ProfileApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5001/progapi-33199/us-central1/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}