package com.example.grindlyapp1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.adapters.BookingAdapter
import com.example.grindlyapp1.databinding.FragmentClientHomeBinding
import com.example.grindlyapp1.network.Booking
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.viewmodelfactory.BookingViewModelFactory
import com.example.grindlyapp1.viewmodels.BookingViewModel
import kotlinx.coroutines.flow.collectLatest

class ClientHomeFragment : Fragment() {

    private var _binding: FragmentClientHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var upcomingAdapter: BookingAdapter
    private lateinit var pastAdapter: BookingAdapter

    private val viewModel: BookingViewModel by viewModels {
        BookingViewModelFactory(
            RetrofitClient.getClient(requireContext())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        observeBookings()
        fetchBookings()
    }

    private fun setupAdapters() {
        upcomingAdapter = BookingAdapter { booking -> openTrackService(booking) }
        pastAdapter = BookingAdapter { booking -> openTrackService(booking) }

        binding.rvUpcomingBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingAdapter
        }

        binding.rvPastBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pastAdapter
        }
    }

    private fun observeBookings() {
        // Bookings
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.clientBookings.collectLatest { bookings ->

                val upcomingStatuses = listOf("Pending", "Accepted", "On the Way", "In Progress")
                val pastStatuses = listOf("Completed")

                val (upcoming, past) = bookings.partition { it.status in upcomingStatuses }
                val pastBookings = past + bookings.filter { it.status in pastStatuses && it.status !in upcomingStatuses }

                upcomingAdapter.submitList(upcoming)
                pastAdapter.submitList(pastBookings)
            }
        }

        // Errors
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Loading
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun fetchBookings() {
        val clientId = getClientId()
        val token = getToken()

        if (clientId.isNotEmpty() && token.isNotEmpty()) {
            viewModel.loadClientBookings(token, clientId)
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getClientId(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("USER_ID", "") ?: ""
    }

    private fun getToken(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("TOKEN", "") ?: ""
    }

    private fun openTrackService(booking: Booking) {
        val fragment = TrackServiceFragment().apply {
            arguments = Bundle().apply {
                putString("bookingId", booking.bookingId)
                putString("serviceId", booking.serviceId)
                putString("hustlerId", booking.hustlerId)
            }
        }


        binding.trackServiceContainer.visibility = View.VISIBLE
        parentFragmentManager.beginTransaction()
            .replace(R.id.trackServiceContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun closeTrackServiceFragment() {
        binding.trackServiceContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
