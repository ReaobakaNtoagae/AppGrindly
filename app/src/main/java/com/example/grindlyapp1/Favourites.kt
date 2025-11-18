package com.example.grindlyapp1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.adapters.FavouritesAdapter
import com.example.grindlyapp1.databinding.FragmentFavouritesBinding
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.repository.FavouritesRepository
import com.example.grindlyapp1.repository.ServiceRepository
import com.example.grindlyapp1.viewmodelfactory.ServiceViewModelFactory
import com.example.grindlyapp1.viewmodel.ServiceViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Favourites : Fragment() {

    private var _binding: FragmentFavouritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ServiceViewModel
    private lateinit var adapter: FavouritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = FragmentFavouritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userToken = getUserToken()

        // Setup repositories
        val apiClient = RetrofitClient.getClient(requireContext())
        val dao = AppDatabase.getDatabase(requireContext()).favouriteDao()
        val serviceDAO = AppDatabase.getDatabase(requireContext()).serviceDao()
        val serviceRepo = ServiceRepository(apiClient,serviceDAO)
        val favouritesRepo = FavouritesRepository(dao)

        // Setup ViewModel
        val factory = ServiceViewModelFactory(
            context = requireContext(),
            serviceRepo = serviceRepo,
            favouritesRepo = favouritesRepo,
            userToken = userToken
        )
        viewModel = ViewModelProvider(this, factory)[ServiceViewModel::class.java]

        setupRecyclerView()
        observeState()
        setupSyncButton()
    }

    private fun setupRecyclerView() {
        adapter = FavouritesAdapter(
            favourites = emptyList(),
            viewModel = viewModel,
            onClick = { service ->
                // TODO: Handle item click, e.g., open service details
            }
        )


        binding.favouritesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.favouritesRecyclerView.adapter = adapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.services.collectLatest { services ->
                        val favourites = services.filter { it.isFavourite }
                        adapter.updateList(favourites)
                    }
                }

                launch {
                    viewModel.isOnline.collectLatest { online ->
                        binding.offlineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.unsyncedFavourites.collectLatest { unsynced ->
                        val hasUnsynced = unsynced.isNotEmpty()
                        binding.unsyncedBadge.visibility = if (hasUnsynced) View.VISIBLE else View.GONE
                        binding.btnRetrySync.visibility = if (hasUnsynced) View.VISIBLE else View.GONE
                        binding.syncStatusText.apply {
                            visibility = if (hasUnsynced) View.VISIBLE else View.GONE
                            text = "Waiting to sync ${unsynced.size} item(s)…"
                        }
                    }
                }
            }
        }
    }


    private fun setupSyncButton() {
        binding.btnRetrySync.setOnClickListener {
            viewModel.syncNow()
        }
    }



    private fun getUserToken(): String =
        requireContext().getSharedPreferences("app_prefs", 0)
            .getString("TOKEN", "").orEmpty()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
