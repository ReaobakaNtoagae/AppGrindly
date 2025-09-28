package com.example.grindlyapp1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.databinding.FragmentBrowseServicesBinding
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.viewmodels.ServiceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowseServicesFragment : Fragment() {

    private var _binding: FragmentBrowseServicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServiceViewModel by viewModels()
    private lateinit var adapter: ServiceAdapter

    private val categories = mutableListOf("All Categories")
    private var currentUserToken: String = ""

    companion object {
        private const val TAG = "BrowseServicesFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentBrowseServicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        currentUserToken = getUserToken()

        setupRecyclerView()
        setupSearch()
        setupFilterSpinner()
        setupSortButtons()
        observeServices()

        Log.d(TAG, "Loading services list...")
        viewModel.loadServicesList()

        // Fetch user favourites
        fetchUserFavourites()
    }

    private fun getUserToken(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        return prefs.getString("TOKEN", "") ?: ""
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")

        adapter = ServiceAdapter(
            allServices = emptyList(),
            onClick = { service ->
                Log.d(TAG, "Service clicked: ${service.title}")
                val intent = Intent(requireContext(), ServiceProfile::class.java)
                intent.putExtra("serviceId", service.id)
                startActivity(intent)
            },
            userToken = currentUserToken
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            adapter.filterBySearch(query)
        }
    }

    private fun setupFilterSpinner() {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.filterSpinner.adapter = spinnerAdapter

        binding.filterSpinner.setSelection(0)
        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedCategory = if (position == 0) null else categories[position]
                adapter.filterByCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                adapter.filterByCategory(null)
            }
        }
    }

    private fun setupSortButtons() {
        binding.sortByRatingButton.setOnClickListener {
            adapter.sortBy(ServiceAdapter.SortType.RATING_HIGH_LOW)
        }
        binding.sortByPriceButton.setOnClickListener {
            adapter.sortBy(ServiceAdapter.SortType.PRICE_LOW_HIGH)
        }
    }

    private fun observeServices() {
        viewModel.services.observe(viewLifecycleOwner) { services ->
            adapter.updateList(services)
            updateCategories(services)
        }
    }

    private fun updateCategories(services: List<Service>) {
        val uniqueCategories = services.mapNotNull { it.category }.distinct()
        categories.clear()
        categories.add("All Categories")
        categories.addAll(uniqueCategories)
        (binding.filterSpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    // Fetch user's favourite services from API
    private fun fetchUserFavourites() {
        if (currentUserToken.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getFavourites("Bearer $currentUserToken")
                if (response.isSuccessful && response.body() != null) {
                    val favouriteIds = response.body()!!.favourites
                    withContext(Dispatchers.Main) {
                        // Mark each service in adapter as favourite if it's in the favourites list
                        val updatedServices = adapter.allServices.map { service ->
                            service.copy(isFavourite = favouriteIds.contains(service.id))
                        }
                        adapter.updateList(updatedServices)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch favourites: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
