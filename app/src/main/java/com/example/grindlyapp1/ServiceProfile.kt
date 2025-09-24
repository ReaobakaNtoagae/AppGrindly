package com.example.grindlyapp1

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.grindlyapp1.models.HustlerProfile
import com.example.grindlyapp1.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ServiceProfile : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var hustlerText: TextView
    private lateinit var priceText: TextView
    private lateinit var ratingText: TextView
    private lateinit var thumbnail: ImageView
    private lateinit var descriptionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_serviceprofile)

        // Correct IDs from XML
        hustlerText = findViewById(R.id.tvHustlerName)
        titleText = findViewById(R.id.tvServiceTitle)
        priceText = findViewById(R.id.tvServicePrice)
        ratingText = findViewById(R.id.tvAverageRating)
        thumbnail = findViewById(R.id.profileImage)
        descriptionText = findViewById(R.id.tvServiceDescription)

        val serviceId = intent.getStringExtra("SERVICE_ID")
        val hustlerId = intent.getStringExtra("HUSTLER_ID")

        if (serviceId != null) loadService(serviceId)
        else if (hustlerId != null) loadHustler(hustlerId)
    }

    private fun loadService(id: String) {
        RetrofitClient.api.getServiceDetails(id).enqueue(object : Callback<HustlerProfile> {
            override fun onResponse(call: Call<HustlerProfile>, response: Response<HustlerProfile>) {
                response.body()?.let { populateHustlerProfile(it) }
            }

            override fun onFailure(call: Call<HustlerProfile>, t: Throwable) {}
        })
    }

    private fun loadHustler(id: String) {
        RetrofitClient.api.getServiceDetails(id).enqueue(object : Callback<HustlerProfile> {
            override fun onResponse(call: Call<HustlerProfile>, response: Response<HustlerProfile>) {
                response.body()?.let { populateHustlerProfile(it) }
            }

            override fun onFailure(call: Call<HustlerProfile>, t: Throwable) {}
        })
    }

    private fun populateHustlerProfile(hustler: HustlerProfile) {
        hustlerText.text = hustler.name
        titleText.text = hustler.serviceTitle
        priceText.text = "R${hustler.price}"
        descriptionText.text = hustler.description
        ratingText.text = "⭐ ${hustler.reviews.map { it.rating }.average().toFloat()}"
        Glide.with(this).load(hustler.profilePicUrl).into(thumbnail)
    }
}
