package com.example.grindlyapp1

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.grindlyapp1.adapters.ReviewAdapter
import com.example.grindlyapp1.adapters.WorkSamplesAdapter
import com.example.grindlyapp1.network.ComboResponse
import com.example.grindlyapp1.network.HustlerProfile
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.network.Service
import com.example.grindlyapp1.repository.FavouritesRepository
import com.example.grindlyapp1.repository.ServiceRepository
import com.example.grindlyapp1.viewmodel.ServiceViewModel
import com.example.grindlyapp1.viewmodelfactory.ServiceViewModelFactory
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import kotlinx.coroutines.launch

class ServiceProfile : AppCompatActivity() {

    private lateinit var viewModel: ServiceViewModel
    private lateinit var userToken: String
    private var serviceId: String? = null
    private lateinit var favouritesRepo: FavouritesRepository

    // Views
    private lateinit var hustlerText: TextView
    private lateinit var titleText: TextView
    private lateinit var categoryText: TextView
    private lateinit var priceText: TextView
    private lateinit var ratingText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var profilePic: ImageView
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var servicePackagesListView: ListView
    private lateinit var recyclerReviews: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var contentContainer: ScrollView
    private lateinit var bookNowButton: Button
    private lateinit var bookingContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_serviceprofile)

        // Bind Views
        hustlerText = findViewById(R.id.tvHustlerName)
        titleText = findViewById(R.id.tvServiceTitle)
        categoryText = findViewById(R.id.tvServiceCategory)
        priceText = findViewById(R.id.tvServicePrice)
        ratingText = findViewById(R.id.tvAverageRating)
        descriptionText = findViewById(R.id.tvServiceDescription)
        profilePic = findViewById(R.id.profileImage)
        viewPager = findViewById(R.id.viewPagerWorkSamples)
        dotsIndicator = findViewById(R.id.dotsIndicator)
        servicePackagesListView = findViewById(R.id.servicePackagesContainer)
        recyclerReviews = findViewById(R.id.recyclerReviews)
        progressBar = findViewById(R.id.progressBar)
        contentContainer = findViewById(R.id.serviceProfileScroll)
        bookNowButton = findViewById(R.id.btnCallNow)
        bookingContainer = findViewById(R.id.bookingFragmentContainer)

        recyclerReviews.layoutManager = LinearLayoutManager(this)

        // Token
        userToken = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("TOKEN", "") ?: ""

        // Service ID
        serviceId = intent.getStringExtra("serviceId")
        if (serviceId == null) {
            Toast.makeText(this, "Invalid service", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Repositories
        val db = AppDatabase.getDatabase(this)
        val serviceDao = db.serviceDao()
        val favouritesDao = db.favouriteDao()

        val serviceRepo = ServiceRepository(RetrofitClient.getClient(this), serviceDao)
        favouritesRepo = FavouritesRepository(favouritesDao)

        // ViewModel
        viewModel = ViewModelProvider(
            this,
            ServiceViewModelFactory(
                context = this,
                serviceRepo = serviceRepo,
                favouritesRepo = favouritesRepo,
                userToken = userToken
            )
        )[ServiceViewModel::class.java]

        observeViewModel()

        progressBar.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE

        viewModel.loadServiceDetails(serviceId!!)
        viewModel.loadUserFavourites()

    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.serviceDetail.collect { combo ->
                combo?.let {
                    populateUI(it)
                    progressBar.visibility = View.GONE
                    contentContainer.visibility = View.VISIBLE
                    viewModel.loadReviews(serviceId!!)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.reviews.collect { reviews ->
                if (reviews.isNotEmpty()) {
                    ratingText.text = "%.1f (%d reviews)"
                        .format(reviews.map { it.rating }.average(), reviews.size)

                    recyclerReviews.adapter = ReviewAdapter(reviews)
                }
            }
        }
    }

    private fun populateUI(combo: ComboResponse) {
        val hustler = combo.hustler ?: return

        hustlerText.text = hustler.name
        titleText.text = hustler.title
        categoryText.text = hustler.category
        priceText.text = "R${hustler.price}"
        descriptionText.text = hustler.description

        Glide.with(this)
            .load(hustler.profilePictureURL)
            .circleCrop()
            .into(profilePic)

        setupPackages(hustler)
        setupWorkSamples(hustler)
        setupBookNow(hustler, combo.service)
    }

    private fun setupPackages(hustler: HustlerProfile) {
        val list = hustler.servicePackages?.map {
            "${it.title} - R${it.price}\n${it.services}"
        } ?: listOf("No packages")

        servicePackagesListView.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
    }

    private fun setupWorkSamples(hustler: HustlerProfile) {
        val samples = hustler.workImageURLs ?: emptyList()

        if (samples.isNotEmpty()) {
            val adapter = WorkSamplesAdapter()
            viewPager.adapter = adapter
            adapter.submitList(samples)
            dotsIndicator.setViewPager2(viewPager)
        } else {
            viewPager.visibility = View.GONE
            dotsIndicator.visibility = View.GONE
        }
    }

    private fun setupBookNow(hustler: HustlerProfile, service: Service?) {
        bookNowButton.setOnClickListener {
            val frag = BookServiceFragment().apply {
                arguments = Bundle().apply {
                    putString("hustlerId", hustler.hustlerId)
                    putString("serviceId", service?.id)
                    putString("serviceTitle", service?.title)
                    putString("location", service?.location)
                    putDouble("price", service?.price ?: 0.00)
                }
            }

            frag.show(supportFragmentManager, "BookServiceBottomSheet")
        }
    }

}