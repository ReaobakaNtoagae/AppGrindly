package com.example.grindlyapp1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.grindlyapp1.R
import com.example.grindlyapp1.viewmodel.StatusTrackingViewModel
import com.google.android.material.card.MaterialCardView

@AndroidEntryPoint
class UpdateServiceStatus : Fragment() {

    private lateinit var binding: FragmentHustlerStatusUpdateBinding
    private val viewModel: StatusTrackingViewModel by viewModels()
    private var bookingId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHustlerStatusUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bookingId = arguments?.getString("bookingId")

        setupObservers()
        setupListeners()

        bookingId?.let {
            viewModel.fetchBookingStatus(it)
        }
    }

    private fun setupObservers() {
        viewModel.bookingStatus.observe(viewLifecycleOwner) { booking ->
            binding.tvClientName.text = booking.clientName
            binding.tvServiceType.text = booking.serviceType
            binding.tvLocation.text = booking.location
            binding.tvDate.text = booking.date
            binding.tvTime.text = booking.time
            binding.tvCurrentStatus.text = booking.status
            binding.tvEta.text = booking.eta ?: "N/A"

            // Visually update card selection
            highlightSelectedCard(booking.status)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    private fun setupListeners() {
        val cards = mapOf(
            binding.cardRequested to "Requested",
            binding.cardAccepted to "Accepted",
            binding.cardOnTheWay to "On the Way",
            binding.cardCompleted to "Completed"
        )

        cards.forEach { (card, status) ->
            card.setOnClickListener {
                bookingId?.let {
                    viewModel.updateStatus(it, status)
                    Toast.makeText(requireContext(), "Status updated to $status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun highlightSelectedCard(selectedStatus: String) {
        val allCards = listOf(
            binding.cardRequested,
            binding.cardAccepted,
            binding.cardOnTheWay,
            binding.cardCompleted
        )

        allCards.forEach { card ->
            card.strokeWidth = if (card.findViewById<TextView>(R.id.tvStatusText).text == selectedStatus) 4 else 2
        }
    }
}


