package com.example.grindlyapp1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.grindlyapp1.databinding.FragmentTrackServiceBinding
import com.example.grindlyapp1.network.Booking
import com.example.grindlyapp1.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TrackServiceFragment : Fragment() {

    private var _binding: FragmentTrackServiceBinding? = null
    private val binding get() = _binding!!

    private var bookingId: String? = null
    private var booking: Booking? = null

    private lateinit var statusCircleMap: Map<String, View>
    private val statusDrawableMap = mapOf(
        "Pending" to R.drawable.status_circle_gray,
        "Accepted" to R.drawable.status_circle_blue,
        "On the Way" to R.drawable.status_circle_yellow,
        "In Progress" to R.drawable.status_circle_green,
        "Completed" to R.drawable.status_circle_purple
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize status circle references
        statusCircleMap = mapOf(
            "Pending" to binding.circlePending,
            "Accepted" to binding.circleAccepted,
            "On the Way" to binding.circleOnTheWay,
            "In Progress" to binding.circleInProgress,
            "Completed" to binding.circleCompleted
        )

        bookingId = arguments?.getString("bookingId")


        if (bookingId == null) {
            Toast.makeText(requireContext(), "No booking ID found", Toast.LENGTH_SHORT).show()
            return
        }

        fetchBookingDetails(bookingId!!)

        binding.btnContactHustler.setOnClickListener {
            val phoneNumber = booking?.hustler?.phoneNumber
            if (!phoneNumber.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Hustler phone number not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchBookingDetails(bookingId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getClient(requireContext())
                val token = requireContext().getSharedPreferences("app_prefs", 0)
                    .getString("TOKEN", "") ?: ""

                val response = api.getBookingById("Bearer $token", bookingId)

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext

                    if (response.isSuccessful && response.body() != null) {
                        booking = response.body()!!.booking
                        booking?.let { b ->
                            populateBookingInfo(b)
                            updateStatusUI(b.status)
                        }
                    } else {
                        Log.e("TrackServiceFragment", "Failed to load booking: ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "Failed to load booking", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("TrackServiceFragment", "Error fetching booking", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), "Error fetching booking details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun populateBookingInfo(b: Booking) {
        val serviceTitle = b.service?.title ?: b.hustler?.title ?: "Unknown Service"
        val hustlerName = b.hustler?.name ?: "Unknown"
        val hustlerPhone = b.hustler?.phoneNumber ?: "N/A"
        val rating = b.service?.rating ?: "N/A"



        binding.tvServiceTitle.text = "Service: $serviceTitle"
        binding.tvDateTime.text = "Last Updated: ${b.updatedAt}"
        binding.tvLocation.text = "Location: ${b.location ?: "TBD"}"
        binding.tvPaymentMethod.text = "Payment: ${b.paymentMethod ?: "TBD"}"
        binding.tvNotes.text = "Notes: ${b.notes ?: "TBD"}"
        binding.tvHustlerPhone.text = "Phone: $hustlerPhone"


    }

    private fun updateStatusUI(currentStatus: String?) {

        statusCircleMap.forEach { (_, view) ->
            view.setBackgroundResource(R.drawable.status_circle_gray)
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 0.5f
        }


        currentStatus?.let { status ->
            val circle = statusCircleMap[status]
            val drawableRes = statusDrawableMap[status] ?: R.drawable.status_circle_gray
            circle?.apply {
                setBackgroundResource(drawableRes)
                animate().scaleX(1.3f).scaleY(1.3f).alpha(1f).setDuration(300).start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
