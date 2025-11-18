package com.example.grindlyapp1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.grindlyapp1.databinding.FragmentUpdateServiceStatusBinding
import com.example.grindlyapp1.network.Booking
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.viewmodelfactory.BookingViewModelFactory
import com.example.grindlyapp1.viewmodels.BookingViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UpdateServiceStatus : Fragment() {

    private var _binding: FragmentUpdateServiceStatusBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingViewModel by viewModels {
        BookingViewModelFactory(
            RetrofitClient.getClient(requireContext())
        )
    }

    private var booking: Booking? = null
    private var bookingId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateServiceStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bookingId = arguments?.getString("bookingId") ?: booking?.bookingId

        setupObservers()
        setupListeners()

        bookingId?.let { id ->
            val token = getToken()
            viewModel.getBookingById(token, id)
        }

        binding.cardBookingInfo.setOnClickListener {
            toggleBookingInfo()
        }

    }

    private fun setupObservers() {
        // Observe booking details
        lifecycleScope.launch {
            viewModel.bookingDetails.collectLatest { bookingDetails ->
                bookingDetails?.let {
                    booking = it
                    populateBookingUI(it)
                    highlightSelectedCard(it.status)
                }
            }
        }



        // Observe errors
        lifecycleScope.launch {
            viewModel.error.collectLatest { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        val statusCards = mapOf(
            binding.cardAccepted to "Accepted",
            binding.cardOnTheWay to "On the Way",
            binding.cardInProgress to "In Progress",
            binding.cardCompleted to "Completed"
        )

        statusCards.forEach { (card, status) ->
            card.setOnClickListener {
                bookingId?.let { id ->
                    val token = getToken()
                    viewModel.updateBookingStatus(token, id, status)
                    Toast.makeText(requireContext(), "Status updated to $status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateBookingUI(b: Booking) {

        binding.tvServiceTitle.text ="Service Title: ${b.service?.title ?: "Unknown Service"} "
        binding.tvLocation.text = "Location: ${b.location ?: "TBD"} "
        binding.tvDateTime.text = "Date and Time: ${b.date ?: "TBD"}"
        binding.tvCurrentStatus.text = "Current Status: ${b.status}"
        binding.tvPaymentMethod.text = "Payment Method:  ${b.paymentMethod}"
        binding.tvNotes.text = "Ntes: ${b.notes}"
    }

    private fun highlightSelectedCard(selectedStatus: String) {
        val statusCards = mapOf(
            binding.cardAccepted to "Accepted",
            binding.cardOnTheWay to "On the Way",
            binding.cardInProgress to "In Progress",
            binding.cardCompleted to "Completed"
        )

        statusCards.forEach { (card, status) ->
            card.strokeWidth = if (status == selectedStatus) 4 else 2
        }
    }

    private fun getToken(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        return prefs.getString("TOKEN", "") ?: ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toggleBookingInfo() {
        val container = binding.bookingDetailsContainer

        if (container.isVisible) {

            container.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    container.visibility = View.GONE
                }
                .start()

            binding.tvBookingInfoTitle.text = "Click to view booking info"
        } else {
            // Expand animation
            container.alpha = 0f
            container.visibility = View.VISIBLE
            container.animate()
                .alpha(1f)
                .setDuration(200)
                .start()

            binding.tvBookingInfoTitle.text = "Booking Information"
        }
    }

}
