package com.example.grindlyapp1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.adapters.FavouritesAdapter
import com.example.grindlyapp1.databinding.FragmentFavouritesBinding
import com.example.grindlyapp1.viewmodels.ServiceViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Favourites : Fragment() {

    private var _binding: FragmentFavouritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServiceViewModel by viewModels({ requireActivity() })
    private lateinit var adapter: FavouritesAdapter
    private var currentUserToken: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavouritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentUserToken = getUserToken()

        setupRecyclerView()
        observeFavouritesFlow()
    }

    private fun setupRecyclerView() {
        adapter = FavouritesAdapter(
            favourites = emptyList(),
            viewModel = viewModel,
            userToken = currentUserToken,
            onClick = { service ->
                // Handle click if needed
            }
        )

        binding.favouritesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.favouritesRecyclerView.adapter = adapter
    }

    private fun observeFavouritesFlow() {
        // Use StateFlow from ViewModel to reactively update favourites
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.services.collectLatest { services ->
                val favourites = services.filter { it.isFavourite }
                adapter.updateList(favourites)
            }
        }
    }

    private fun getUserToken(): String =
        requireContext().getSharedPreferences("app_prefs", 0).getString("TOKEN", "").orEmpty()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
