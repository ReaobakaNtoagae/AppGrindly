package com.example.grindlyapp1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.grindlyapp1.models.ComboResponse
import com.example.grindlyapp1.models.HustlerProfile
import com.example.grindlyapp1.viewmodels.ServiceViewModel
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class ServiceProfile : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var hustlerText: TextView
    private lateinit var priceText: TextView
    private lateinit var ratingText: TextView
    private lateinit var profilePic: ImageView
    private lateinit var descriptionText: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var servicePackagesListView: ListView
    private lateinit var categoryText: TextView
    private lateinit var callNowBtn: Button

    private val viewModel: ServiceViewModel by viewModels()
    private var hustlerProfile: HustlerProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_serviceprofile)

        // Initialize views
        hustlerText = findViewById(R.id.tvHustlerName)
        titleText = findViewById(R.id.tvServiceTitle)
        categoryText = findViewById(R.id.tvServiceCategory)
        priceText = findViewById(R.id.tvServicePrice)
        ratingText = findViewById(R.id.tvAverageRating)
        profilePic = findViewById(R.id.profileImage)
        descriptionText = findViewById(R.id.tvServiceDescription)
        viewPager = findViewById(R.id.viewPagerWorkSamples)
        servicePackagesListView = findViewById(R.id.servicePackagesContainer)

        dotsIndicator = findViewById(R.id.dotsIndicator)
        callNowBtn = findViewById<Button>(R.id.btnCallNow)


        val serviceId = intent.getStringExtra("serviceId")
        if (serviceId != null) {
            observeServiceDetail()
            viewModel.loadServiceDetails(serviceId)
        } else {
            finish()
        }




    }


    private fun observeServiceDetail() {
        viewModel.serviceDetail.observe(this) { comboResponse: ComboResponse? ->
            if (comboResponse != null) {
                hustlerProfile = comboResponse.hustler
                populateHustlerProfile()
            } else {
                finish()
            }
        }
    }


    private fun populateHustlerProfile() {
        hustlerProfile?.let { hustler ->

            hustlerText.text = hustler.name ?: "Unknown Hustler"


            titleText.text = hustler.title ?: "Service"
            priceText.text =
                hustler.price?.let { "R$it · ${hustler.pricingModel ?: "Per Session"}" }
                    ?: "Price N/A"

            categoryText.text = hustler.category ?: "Category"
            descriptionText.text = hustler.description ?: "No description available."

            val averageRating = hustler.reviews?.mapNotNull { it.rating }?.average() ?: 0.0
            ratingText.text =
                if (averageRating > 0) "⭐ ${"%.1f".format(averageRating)}" else "No ratings yet"


            Glide.with(this)
                .load(hustler.profilePicURL)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .circleCrop()
                .into(profilePic)


            val packages = hustler.servicePackages ?: emptyList()
            if (packages.isNotEmpty()) {
                val titles = packages.map {
                    "${it.title ?: "Package"} - ${it.price ?: 0}\n${it.services ?: ""}"
                }
                val adapter = android.widget.ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    titles
                )
                servicePackagesListView.adapter = adapter
            } else {
                val adapter = android.widget.ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    listOf("No service packages available")
                )
                servicePackagesListView.adapter = adapter
            }

            setupWorkSamplesPager()

            callNowBtn.setOnClickListener {
                val phoneNumber = hustler.phoneNumber
                if (!phoneNumber.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                    }
                    startActivity(intent)

                } else {
                    Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun setupWorkSamplesPager() {
        hustlerProfile?.workImageURLs?.let { samples ->
            val adapter = WorkSamplesAdapter()
            viewPager.adapter = adapter
            adapter.submitList(samples)
            dotsIndicator.setViewPager2(viewPager)
        }
    }
}
