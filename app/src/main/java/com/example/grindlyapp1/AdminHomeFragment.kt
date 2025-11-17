package com.example.grindlyapp1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.adapters.VerificationAdapter
import com.example.grindlyapp1.databinding.FragmentAdminHomeBinding
import com.example.grindlyapp1.viewmodel.AdminViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var adapter: VerificationAdapter
    private var token: String = "" // Fetch your user token from prefs or auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        token = getAuthToken() // implement this method to get saved token
        setupUI()
        setupRecyclerView()
        observeViewModel()
        fetchPendingHustlers()
    }

    private fun setupUI() {
        binding.swipeRefresh.setOnRefreshListener {
            fetchPendingHustlers()
        }

        binding.tvAdminGreeting.text = "Welcome, Admin 👋"
        binding.tvAdminSubtitle.text = "Here are all hustlers waiting for verification"
    }

    private fun setupRecyclerView() {
        adapter = VerificationAdapter(
            requireContext(),
            mutableListOf(),
            onApprove = { hustler -> viewModel.verifyHustler(requireContext(), token, hustler.hustlerId, "verify") },
            onReject = { hustler -> viewModel.verifyHustler(requireContext(), token, hustler.hustlerId, "reject") }
        )
        binding.recyclerVerifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerVerifications.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.hustlers.collectLatest { list ->
                adapter.updateData(list)
                binding.tvPendingCount.text = list.size.toString()
                binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.loading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }

        lifecycleScope.launch {
            viewModel.toastMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }
        }
    }

    private fun fetchPendingHustlers() {
        viewModel.fetchPendingHustlers(requireContext(), token)
    }

    private fun getAuthToken(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        return prefs.getString("AUTH_TOKEN", "") ?: ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
