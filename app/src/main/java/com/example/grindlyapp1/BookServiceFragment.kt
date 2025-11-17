package com.example.grindlyapp1

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.grindlyapp1.databinding.FragmentBookServiceBinding
import com.example.grindlyapp1.network.BookingRequest
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.viewmodelfactory.BookingViewModelFactory
import com.example.grindlyapp1.viewmodels.BookingViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class BookServiceFragment : Fragment() {

    private var _binding: FragmentBookServiceBinding? = null
    private val binding get() = _binding!!


    private val bookingViewModel: BookingViewModel by viewModels {
        BookingViewModelFactory(
            RetrofitClient.getClient(requireContext())
        )
    }


    private var hustlerId = ""
    private var serviceId = ""
    private var serviceTitle = ""
    private var location = ""
    private var price = 100.0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hustlerId = arguments?.getString("hustlerId") ?: ""
        serviceId = arguments?.getString("serviceId") ?: ""
        serviceTitle = arguments?.getString("serviceTitle") ?: ""
        location = arguments?.getString("location") ?: ""
        price = arguments?.getDouble("price") ?: 100.0

        if (hustlerId.isEmpty() || serviceId.isEmpty()) {
            Toast.makeText(requireContext(), "Missing booking details", Toast.LENGTH_SHORT).show()
            return
        }

        binding.etServiceName.setText(serviceTitle)
        binding.etServiceName.isEnabled = false

        binding.etLocation.setText(location)
        binding.etLocation.isEnabled = false

        setupPaymentDropdown()
        setupObservers()
        setupListeners()
    }

    private fun setupPaymentDropdown() {
        val paymentOptions = resources.getStringArray(R.array.payment_methods)
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item_dropdown, paymentOptions)
        binding.etPayment.setAdapter(adapter)
    }

    private fun setupObservers() {

        // Loading state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            bookingViewModel.isLoading.collectLatest { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                binding.btnBookService.isEnabled = !loading
            }
        }

        // Error state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            bookingViewModel.error.collectLatest { errorMsg ->
                if (errorMsg != null) {
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etTime.setOnClickListener { showTimePicker() }
        binding.btnBookService.setOnClickListener { bookService() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                binding.etDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                binding.etTime.setText(String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun bookService() {

        val date = binding.etDate.text.toString().trim()
        val time = binding.etTime.text.toString().trim()
        val paymentMethod = binding.etPayment.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        if (date.isEmpty() || time.isEmpty() || paymentMethod.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate date is not in the past
        val selectedDateTime = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse("$date $time")
        } catch (e: Exception) { null }

        if (selectedDateTime == null || selectedDateTime.before(Date())) {
            Toast.makeText(requireContext(), "Select a valid future date/time", Toast.LENGTH_SHORT).show()
            return
        }

        val clientId = getClientId()
        val token = getToken()

        if (clientId.isEmpty() || token.isEmpty()) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val request = BookingRequest(
            clientId = clientId,
            hustlerId = hustlerId,
            serviceId = serviceId,
            serviceTitle = serviceTitle,
            date = "$date $time",
            price = price,
            location = location,
            paymentMethod = paymentMethod,
            notes = notes
        )

        Log.d("BookService", "Request → $request")

        bookingViewModel.createBooking(token, request) {
            // On success →
            Toast.makeText(requireContext(), "Booking Created Successfully", Toast.LENGTH_SHORT).show()
            navigateToTrackService(serviceId)  // use bookingId from backend if needed
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

    private fun navigateToTrackService(bookingId: String) {
        val trackFragment = TrackServiceFragment().apply {
            arguments = Bundle().apply {
                putString("bookingId", bookingId)
            }
        }

        binding.trackServiceContainer.visibility = View.VISIBLE

        parentFragmentManager.beginTransaction()
            .replace(R.id.trackServiceContainer, trackFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
