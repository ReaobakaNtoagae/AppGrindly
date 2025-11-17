package com.example.grindlyapp1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.grindlyapp1.databinding.ItemBookingBinding
import com.example.grindlyapp1.network.Booking

class HustlerBookingAdapter(
    private var bookings: List<Booking>,
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<HustlerBookingAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: Booking) {
            binding.tvServiceName.text = booking.serviceTitle
            binding.tvDate.text = "Date: ${booking.date}"
            binding.tvStatus.text = "Status: ${booking.status}"

            // Click listener for the card
            binding.root.setOnClickListener {
                onBookingClick(booking)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    /** Update adapter list */
    fun updateList(newList: List<Booking>) {
        bookings = newList
        notifyDataSetChanged()
    }
}
