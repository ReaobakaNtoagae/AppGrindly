package com.example.grindlyapp1

import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.adapters.ServiceAdapter
import com.example.grindlyapp1.databinding.FragmentBrowseServicesBinding
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.repository.ServiceRepository
import com.example.grindlyapp1.viewmodels.ServiceViewModel
import com.example.grindlyapp1.viewmodelfactory.ServiceViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BrowseServicesFragment : Fragment() {

    private var _binding: FragmentBrowseServicesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ServiceAdapter
    private val categories = mutableListOf("All Categories")

    private lateinit var repository: ServiceRepository
    private lateinit var currentUserToken: String

    // Initialize ViewModel with factory
    private val viewModel: ServiceViewModel by viewModels {
        ServiceViewModelFactory(repository, currentUserToken)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1️⃣ Load token safely
        currentUserToken = requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("TOKEN", "") ?: ""

        // 2️⃣ Initialize repository
        val apiClient = RetrofitClient.getClient(requireContext())
        repository = ServiceRepository(apiClient)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseServicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "User token: $currentUserToken")

        setupRecyclerView()
        setupSearch()
        setupFilterSpinner()
        setupSortButtons()
        observeServicesFlow()

        viewModel.loadServicesList()
        viewModel.loadUserFavourites()
    }

    private fun setupRecyclerView() {
        adapter = ServiceAdapter(
            services = emptyList(),
            onClick = { service ->
                val intent = Intent(requireContext(), ServiceProfile::class.java)
                intent.putExtra("serviceId", service.id)
                startActivity(intent)
            },
            onFavouriteClicked = { service ->
                viewModel.toggleFavourite(service)
            },
            onSubmitClicked = { review ->
                viewModel.addReview(review.id, review.rating, review.comment)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }


    private fun observeServicesFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.services.collectLatest { services ->
                adapter.updateList(services)
                updateCategories(services)
            }
        }
    }


    private fun updateCategories(services: List<com.example.grindlyapp1.network.Service>) {
        val uniqueCategories = services.mapNotNull { it.category }.distinct()
        categories.clear()
        categories.add("All Categories")
        categories.addAll(uniqueCategories)
        (binding.filterSpinner.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener { editable ->
            val query = editable?.toString().orEmpty()
            adapter.filterBySearch(query)
        }
    }

    private fun setupFilterSpinner() {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.filterSpinner.adapter = spinnerAdapter
        binding.filterSpinner.setSelection(0)

        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedCategory = categories.getOrNull(position)?.takeIf { it != "All Categories" }
                adapter.filterByCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "BrowseServicesFragment"
    }
}

// Extension function for Spinner listener
private fun androidx.appcompat.widget.AppCompatSpinner.setOnItemSelectedListener(
    onItemSelected: (position: Int) -> Unit
) {
    this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long
        ) = onItemSelected(position)

        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }
}
