package com.example.grindlyapp1

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.grindlyapp1.viewmodel.StatusTrackingViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TrackServiceFragment : Fragment() {

    private lateinit var binding: FragmentClientStatusTrackingBinding
    private val viewModel: StatusTrackingViewModel by viewModels()
    private var bookingId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClientStatusTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bookingId = arguments?.getString("bookingId")

        setupObservers()
        startAutoRefresh()

        bookingId?.let {
            viewModel.fetchBookingStatus(it)
        }
    }

    private fun setupObservers() {
        viewModel.bookingStatus.observe(viewLifecycleOwner) { booking ->
            binding.tvHustlerName.text = booking.hustlerName
            binding.tvServiceType.text = booking.serviceType
            binding.tvLocation.text = booking.location
            binding.tvDate.text = booking.date
            binding.tvTime.text = booking.time
            binding.tvEta.text = booking.eta ?: "Calculating..."

            updateRoadmap(booking.status)
        }
    }

    private fun startAutoRefresh() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                bookingId?.let { viewModel.fetchBookingStatus(it) }
                handler.postDelayed(this, 10000) // every 10 seconds
            }
        }, 10000)
    }

    private fun updateRoadmap(status: String) {
        val stages = listOf(
            binding.stageRequested,
            binding.stageAccepted,
            binding.stageOnTheWay,
            binding.stageCompleted
        )

        val statusIndex = when (status) {
            "Requested" -> 0
            "Accepted" -> 1
            "On the Way" -> 2
            "Completed" -> 3
            else -> -1
        }

        stages.forEachIndexed { index, stage ->
            stage.alpha = if (index <= statusIndex) 1f else 0.4f
            stage.setBackgroundResource(
                if (index <= statusIndex) R.drawable.roadmap_circle_active
                else R.drawable.roadmap_circle_inactive
            )
        }
    }
}

