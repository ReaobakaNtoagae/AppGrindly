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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.adapters.ServiceAdapter
import com.example.grindlyapp1.databinding.FragmentBrowseServicesBinding
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.repository.FavouritesRepository
import com.example.grindlyapp1.repository.ServiceRepository
import com.example.grindlyapp1.viewmodel.ServiceViewModel
import com.example.grindlyapp1.viewmodelfactory.ServiceViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BrowseServicesFragment : Fragment() {

    private var _binding: FragmentBrowseServicesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ServiceAdapter
    private val categories = mutableListOf("All Categories")

    private lateinit var serviceRepo: ServiceRepository
    private lateinit var favouritesRepo: FavouritesRepository
    private lateinit var currentUserToken: String

    private lateinit var viewModel: ServiceViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1️⃣ Load token
        currentUserToken = requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("TOKEN", "") ?: ""


        // 2️⃣ Create repositories
        val apiClient = RetrofitClient.getClient(requireContext())
        val serviceDao = AppDatabase.getDatabase(requireContext()).serviceDao()
        serviceRepo = ServiceRepository(apiClient,serviceDao)


        val dao = AppDatabase.getDatabase(requireContext()).favouriteDao()
        favouritesRepo = FavouritesRepository(dao)



        // 3️⃣ Create ViewModel with factory (⚠️ MUST happen AFTER repos are created)
        val factory = ServiceViewModelFactory(
            context = requireContext(),
            serviceRepo = serviceRepo,
            favouritesRepo = favouritesRepo,
            userToken = currentUserToken
        )

        viewModel = ViewModelProvider(this, factory)[ServiceViewModel::class.java]
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
        val unique = services.mapNotNull { it.category }.distinct()
        categories.apply {
            clear()
            add("All Categories")
            addAll(unique)
        }
        (binding.filterSpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }


    private fun setupSearch() {
        binding.searchInput.addTextChangedListener {
            adapter.filterBySearch(it.toString())
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

        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = categories[pos].takeIf { it != "All Categories" }
                adapter.filterByCategory(selected)
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
