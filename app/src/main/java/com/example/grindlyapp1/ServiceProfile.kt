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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.grindlyapp1.models.ComboResponse
import com.example.grindlyapp1.models.HustlerProfile
import com.example.grindlyapp1.models.Service
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
    private lateinit var recyclerReviews: RecyclerView

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
        callNowBtn = findViewById(R.id.btnCallNow)
        recyclerReviews = findViewById(R.id.recyclerReviews)

        // Setup RecyclerView for reviews
        recyclerReviews.layoutManager = LinearLayoutManager(this)

        // Get serviceId passed through Intent
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


                val serviceId = comboResponse.service?.id
                if (!serviceId.isNullOrEmpty()) {
                    viewModel.loadReviews(serviceId)
                }
            } else {
                finish()
            }
        }

        viewModel.reviews.observe(this) { reviews ->
            if (reviews.isNotEmpty()) {
                val average = reviews.map { it.rating }.average()
                ratingText.text = "%.1f (%d reviews)".format(average, reviews.size)
                recyclerReviews.adapter = ReviewAdapter(reviews)
            } else {
                ratingText.text = "No ratings yet"
                Toast.makeText(this, "No reviews yet", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun populateHustlerProfile() {
        hustlerProfile?.let { hustler ->

            // Basic details
            hustlerText.text = hustler.name.ifBlank { "Unknown Hustler" }
            titleText.text = hustler.title.ifBlank { "Service" }
            priceText.text = hustler.price.let {
                "R$it · ${hustler.pricingModel ?: "Per Session"}"
            }
            categoryText.text = hustler.category.ifBlank { "Category" }
            descriptionText.text = hustler.description.ifBlank { "No description available." }




            Glide.with(this)
                .load(hustler.profilePictureURL)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .circleCrop()
                .into(profilePic)


            val packages = hustler.servicePackages ?: emptyList()
            if (packages.isNotEmpty()) {
                val titles = packages.map {
                    "${it.title ?: "Package"} - R${it.price ?: 0}\n${it.services ?: ""}"
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
