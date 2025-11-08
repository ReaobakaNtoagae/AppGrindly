package com.example.grindlyapp1

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.grindlyapp1.adapters.BookingAdapter
import com.example.grindlyapp1.databinding.FragmentClientHomeBinding
import com.example.grindlyapp1.viewmodel.BookingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClientHomeFragment : Fragment() {

    private var _binding: FragmentClientHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by viewModels()
    private lateinit var adapter: BookingAdapter
    private lateinit var userId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClientHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("userId", "") ?: ""

        adapter = BookingAdapter(emptyList())
        binding.rvUpcomingBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUpcomingBookings.adapter = adapter

        viewModel.bookings.observe(viewLifecycleOwner) { list ->
            adapter = BookingAdapter(list)
            binding.recyclerBookings.adapter = adapter
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadBookingsForClient(userId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
