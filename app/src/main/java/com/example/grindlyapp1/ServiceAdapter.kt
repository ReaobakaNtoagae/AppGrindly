package com.example.grindlyapp1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.models.Service

class ServiceAdapter(
    private var allServices: List<Service>,
    private val onClick: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    private var displayedServices: List<Service> = allServices

    private fun parseRating(rating: String?): Float {
        return rating?.toFloatOrNull() ?: 0f
    }

    inner class ServiceViewHolder(val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = displayedServices[position]

        holder.binding.serviceTitle.text = service.title ?: "Untitled Service"
        holder.binding.hustlerName.text = service.name ?: "Unknown Hustler"
        holder.binding.price.text =
            service.price?.let { "R$it · ${service.pricingModel}" } ?: "Price N/A"
        holder.binding.serviceLocation.text = service.location ?: "Location unknown"

        holder.binding.ratingBar.rating = parseRating(service.rating)

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

        holder.itemView.setOnClickListener { onClick(service) }
    }

    override fun getItemCount() = displayedServices.size

    fun updateList(newList: List<Service>) {
        allServices = newList
        displayedServices = newList
        notifyDataSetChanged()
    }


    fun filterBySearch(query: String) {
        displayedServices = if (query.isBlank()) {
            allServices
        } else {
            val lower = query.lowercase()
            allServices.filter {
                (it.title?.lowercase()?.contains(lower) == true) ||
                        (it.category?.lowercase()?.contains(lower) == true) ||
                        (it.name?.lowercase()?.contains(lower) == true)
            }
        }
        notifyDataSetChanged()
    }


    fun filterByCategory(category: String?) {
        displayedServices = if (category.isNullOrEmpty()) {
            allServices
        } else {
            allServices.filter { it.category == category }
        }
        notifyDataSetChanged()
    }


    enum class SortType { PRICE_LOW_HIGH, PRICE_HIGH_LOW, RATING_HIGH_LOW }

    fun sortBy(type: SortType) {
        displayedServices = when (type) {
            SortType.PRICE_LOW_HIGH -> displayedServices.sortedBy { it.price ?: 0.0 }
            SortType.PRICE_HIGH_LOW -> displayedServices.sortedByDescending { it.price ?: 0.0 }
            SortType.RATING_HIGH_LOW -> displayedServices.sortedByDescending { parseRating(it.rating).toDouble() }
        }
        notifyDataSetChanged()
    }


}
