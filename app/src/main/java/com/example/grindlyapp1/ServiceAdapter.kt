package com.example.grindlyapp1

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.viewmodels.ServiceViewModel

class ServiceAdapter(
    private var allServices: List<Service>,
    private val viewModel: ServiceViewModel,
    private val userToken: String,
    private val onClick: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    private var displayedServices: List<Service> = allServices

    inner class ServiceViewHolder(val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = displayedServices[position]

        // --- Bind text ---
        holder.binding.serviceTitle.text = service.title ?: "Untitled Service"
        holder.binding.hustlerName.text = service.name ?: "Unknown Hustler"
        holder.binding.price.text = service.price?.let { "R$it · ${service.pricingModel}" } ?: "Price N/A"
        holder.binding.serviceLocation.text = service.location ?: "Location unknown"
        holder.binding.ratingBar.rating = service.rating ?: 0f
        holder.binding.reviewCount.text = "(${service.reviewCount ?: 0})"

        // --- Load images ---
        Glide.with(holder.itemView.context)
            .load(service.workSampleURL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .centerCrop()
            .into(holder.binding.thumbnail)

        Glide.with(holder.itemView.context)
            .load(service.profilePicURL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .centerCrop()
            .into(holder.binding.profilePic)

        // --- Favourite button ---
        holder.binding.btnFavourite.setImageResource(
            if (service.isFavourite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        holder.binding.btnFavourite.setOnClickListener {
            val updatedService = service.copy(isFavourite = !service.isFavourite)
            viewModel.toggleFavourite(updatedService, userToken)
        }

        // --- Submit review ---
        holder.binding.btnSubmitReview.setOnClickListener {
            val rating = holder.binding.userRatingBar.rating.toInt()
            val comment = holder.binding.userComment.text.toString().takeIf { it.isNotBlank() }

            if (rating == 0) {
                Toast.makeText(holder.itemView.context, "Please provide a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addReview(service.id, rating, comment, userToken)

            holder.binding.userComment.text.clear()
            holder.binding.userRatingBar.rating = 0f
            Toast.makeText(holder.itemView.context, "Review submitted", Toast.LENGTH_SHORT).show()
        }

        // --- Card click ---
        holder.itemView.setOnClickListener { onClick(service) }
    }

    override fun getItemCount(): Int = displayedServices.size

    fun updateList(newList: List<Service>) {
        allServices = newList
        displayedServices = newList
        notifyDataSetChanged()
    }

    fun filterBySearch(query: String) {
        displayedServices = if (query.isBlank()) allServices
        else allServices.filter {
            (it.title?.contains(query, true) ?: false) ||
                    (it.category?.contains(query, true) ?: false) ||
                    (it.name?.contains(query, true) ?: false)
        }
        notifyDataSetChanged()
    }

    fun filterByCategory(category: String?) {
        displayedServices = if (category.isNullOrEmpty()) allServices
        else allServices.filter { it.category == category }
        notifyDataSetChanged()
    }

    enum class SortType { PRICE_LOW_HIGH, PRICE_HIGH_LOW, RATING_HIGH_LOW }
    fun sortBy(type: SortType) {
        displayedServices = when (type) {
            SortType.PRICE_LOW_HIGH -> displayedServices.sortedBy { it.price }
            SortType.PRICE_HIGH_LOW -> displayedServices.sortedByDescending { it.price }
            SortType.RATING_HIGH_LOW -> displayedServices.sortedByDescending { it.rating ?: 0f }
        }
        notifyDataSetChanged()
    }
}




