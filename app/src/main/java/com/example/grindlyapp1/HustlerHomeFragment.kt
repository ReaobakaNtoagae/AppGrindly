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
import com.example.grindlyapp1.adapters.HustlerBookingAdapter
import com.example.grindlyapp1.databinding.FragmentHustlerHomeBinding
import com.example.grindlyapp1.network.Booking
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.viewmodelfactory.BookingViewModelFactory
import com.example.grindlyapp1.viewmodels.BookingViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HustlerHomeFragment : Fragment() {

    private var _binding: FragmentHustlerHomeBinding? = null
    private val binding get() = _binding!!

    private val bookingViewModel: BookingViewModel by viewModels {
        BookingViewModelFactory(
            RetrofitClient.getClient(requireContext())
        )
    }


    private val upcomingAdapter by lazy {
        HustlerBookingAdapter(emptyList(), ::navigateUpdateService)
    }
    private val pastAdapter by lazy {
        HustlerBookingAdapter(emptyList(), ::navigateUpdateService)
    }

    private var hustlerId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHustlerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hustlerId = getHustlerId()

        setupRecyclerViews()
        observeViewModel()

        if (hustlerId.isNotEmpty()) {
            bookingViewModel.loadHustlerBookings(requireContext().toString(), hustlerId)
        }
    }

    private fun setupRecyclerViews() = with(binding) {
        rvUpcomingAppointments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingAdapter
        }
        rvPastAppointments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pastAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            bookingViewModel.hustlerBookings.collectLatest { bookings ->
                val upcomingStatuses = listOf("Pending", "Accepted", "On the Way", "In Progress")
                val pastStatuses = listOf("Completed")

                upcomingAdapter.updateList(bookings.filter { it.status in upcomingStatuses })
                pastAdapter.updateList(bookings.filter { it.status in pastStatuses })
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            bookingViewModel.error.collectLatest { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun getHustlerId(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("USER_ID", "") ?: ""
    }

    private fun navigateUpdateService(booking: Booking) {
        binding.fragmentContainer.visibility = View.VISIBLE

        val fragment = UpdateServiceStatus().apply {
            arguments = Bundle().apply {
                putString("bookingId", booking.bookingId)
                putString("serviceId", booking.serviceId)
                putString("hustlerId", booking.hustlerId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
