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

class BrowseServicesFragment : Fragment() {

    private var _binding: FragmentBrowseServicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServiceViewModel by viewModels({ requireActivity() }) // shared ViewModel
    private lateinit var adapter: ServiceAdapter

    private val categories = mutableListOf("All Categories")
    private var currentUserToken: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseServicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserToken = getUserToken()
        Log.d("BrowseServicesFragment", "User token: $currentUserToken")

        setupRecyclerView()
        setupSearch()
        setupFilterSpinner()
        setupSortButtons()
        observeServices()

        // Load services and mark favourites in ViewModel
        viewModel.loadServicesList()
        viewModel.loadUserFavourites(currentUserToken)
    }

    private fun setupRecyclerView() {
        adapter = ServiceAdapter(
            allServices = emptyList(),
            viewModel = viewModel,          // Pass the ViewModel
            userToken = currentUserToken,   // Pass the user token
            onClick = { service ->
                Log.d("BrowseServicesFragment", "Card clicked: ${service.title}")
                val intent = Intent(requireContext(), ServiceProfile::class.java)
                intent.putExtra("serviceId", service.id)
                startActivity(intent)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }


    private fun observeServices() {
        viewModel.services.observe(viewLifecycleOwner) { services ->
            Log.d("BrowseServicesFragment", "Services observed: ${services.size}")
            adapter.updateList(services)
            updateCategories(services)
        }
    }

    private fun updateCategories(services: List<Service>) {
        val uniqueCategories = services.mapNotNull { it.category }.distinct()
        categories.clear()
        categories.add("All Categories")
        categories.addAll(uniqueCategories)
        Log.d("BrowseServicesFragment", "Categories updated: $categories")
        (binding.filterSpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            Log.d("BrowseServicesFragment", "Search query: $query")
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
                Log.d("BrowseServicesFragment", "Category selected: $selectedCategory")
                adapter.filterByCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("BrowseServicesFragment", "No category selected")
                adapter.filterByCategory(null)
            }
        }
    }

    private fun setupSortButtons() {
        binding.sortByRatingButton.setOnClickListener {
            Log.d("BrowseServicesFragment", "Sort by rating clicked")
            adapter.sortBy(ServiceAdapter.SortType.RATING_HIGH_LOW)
        }
        binding.sortByPriceButton.setOnClickListener {
            Log.d("BrowseServicesFragment", "Sort by price clicked")
            adapter.sortBy(ServiceAdapter.SortType.PRICE_LOW_HIGH)
        }
    }

    private fun getUserToken(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        return prefs.getString("TOKEN", "") ?: ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


