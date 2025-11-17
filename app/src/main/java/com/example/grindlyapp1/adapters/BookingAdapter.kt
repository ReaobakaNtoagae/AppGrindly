package com.example.grindlyapp1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.grindlyapp1.databinding.ItemBookingBinding
import com.example.grindlyapp1.network.Booking
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private val bookings = mutableListOf<Booking>()

    fun submitList(list: List<Booking>) {
        bookings.clear()
        bookings.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    inner class BookingViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvServiceName.text = booking.service?.title
                ?: booking.serviceTitle
                        ?: "Unknown Service"

            binding.tvDate.text = booking.date ?: "N/A"
            binding.tvStatus.text = booking.status

            binding.root.setOnClickListener {
                onBookingClick(booking)
            }
        }
    }
}
