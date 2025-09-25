package com.example.grindlyapp1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.databinding.FragmentBrowseServicesBinding
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.viewmodels.ServiceViewModel
import androidx.core.widget.addTextChangedListener

class BrowseServicesFragment : Fragment() {

    private var _binding: FragmentBrowseServicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServiceViewModel by viewModels()
    private lateinit var adapter: ServiceAdapter

    private val categories = mutableListOf("All Categories")

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

        setupRecyclerView()
        setupSearch()
        setupFilterSpinner()
        setupSortButtons()
        observeServices()


        Log.d(TAG, "Loading services list...")
        viewModel.loadServicesList()
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        adapter = ServiceAdapter(emptyList()) { service ->
            Log.d(TAG, "Service clicked: ${service.title}")

            val intent = Intent(requireContext(), ServiceProfile::class.java)
            intent.putExtra("serviceId", service.id)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        Log.d(TAG, "Setting up Search")
        binding.searchInput.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            Log.d(TAG, "Search query: $query")
            adapter.filterBySearch(query)
        }
    }

    private fun setupFilterSpinner() {
        Log.d(TAG, "Setting up Filter Spinner")
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
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCategory = if (position == 0) null else categories[position]
                Log.d(TAG, "Filter selected: $selectedCategory")
                adapter.filterByCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "Filter cleared (Nothing selected)")
                adapter.filterByCategory(null)
            }
        }
    }

    private fun setupSortButtons() {
        Log.d(TAG, "Setting up Sort Buttons")
        binding.sortByRatingButton.setOnClickListener {
            Log.d(TAG, "Sort by rating clicked")
            adapter.sortBy(ServiceAdapter.SortType.RATING_HIGH_LOW)
        }
        binding.sortByPriceButton.setOnClickListener {
            Log.d(TAG, "Sort by price clicked")
            adapter.sortBy(ServiceAdapter.SortType.PRICE_LOW_HIGH)
        }
    }

    private fun observeServices() {
        Log.d(TAG, "Observing services LiveData")
        viewModel.services.observe(viewLifecycleOwner) { services ->
            Log.d(TAG, "Services received: ${services.size}")
            services.forEach { Log.d(TAG, "Service: ${it.title}, Category: ${it.category}") }

            adapter.updateList(services)
            updateCategories(services)
        }
    }

    private fun updateCategories(services: List<Service>) {
        Log.d(TAG, "Updating categories from services")
        val uniqueCategories = services.mapNotNull { it.category }.distinct()
        Log.d(TAG, "Unique categories found: $uniqueCategories")

        categories.clear()
        categories.add("All Categories")
        categories.addAll(uniqueCategories)
        (binding.filterSpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        _binding = null
    }
}
