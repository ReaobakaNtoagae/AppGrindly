package com.example.grindlyapp1

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.models.Service

class ServiceAdapter(
    private var allServices: List<Service>,
    private val onClick: (Service) -> Unit,
    private val onFavouriteClicked: ((Service) -> Unit)? = null // ViewModel callback
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    private var displayedServices: List<Service> = allServices

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

        // --- LOG ---
        Log.d("ServiceAdapter", "Binding service at position $position: ${service.title}, isFavourite=${service.isFavourite}")

        // Bind text
        holder.binding.serviceTitle.text = service.title ?: "Untitled Service"
        holder.binding.hustlerName.text = service.name ?: "Unknown Hustler"
        holder.binding.price.text =
            service.price?.let { "R$it · ${service.pricingModel}" } ?: "Price N/A"
        holder.binding.serviceLocation.text = service.location ?: "Location unknown"
        holder.binding.ratingBar.rating = service.rating?.toFloatOrNull() ?: 0f

        // Load images
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

        // Set favourite icon initially
        holder.binding.btnFavourite.setImageResource(
            if (service.isFavourite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )

        // --- Favourite button click ---
        holder.binding.btnFavourite.setOnClickListener {
            Log.d("ServiceAdapter", "Favourite clicked for ${service.title}, current state: ${service.isFavourite}")

            // Toggle favourite state locally
            val updatedService = service.copy(isFavourite = !service.isFavourite)

            // Update UI immediately
            holder.binding.btnFavourite.setImageResource(
                if (updatedService.isFavourite) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )

            // Notify ViewModel / parent to persist change
            onFavouriteClicked?.invoke(updatedService)

            // Optimistic removal from displayed list if unfavourited
            if (!updatedService.isFavourite) {
                removeFromDisplayed(service)
            }
        }

        // Entire card click
        holder.itemView.setOnClickListener {
            Log.d("ServiceAdapter", "Card clicked for ${service.title}")
            onClick(service)
        }
    }

    override fun getItemCount(): Int = displayedServices.size

    fun updateList(newList: List<Service>) {
        Log.d("ServiceAdapter", "Updating list with ${newList.size} services")
        allServices = newList
        displayedServices = newList
        notifyDataSetChanged()
    }

    private fun removeFromDisplayed(service: Service) {
        Log.d("ServiceAdapter", "Removing ${service.title} from displayed list")
        displayedServices = displayedServices.filter { it.id != service.id }
        notifyDataSetChanged()
    }

    fun filterBySearch(query: String) {
        displayedServices = if (query.isBlank()) allServices
        else allServices.filter {
            (it.title?.contains(query, ignoreCase = true) ?: false) ||
                    (it.category?.contains(query, ignoreCase = true) ?: false) ||
                    (it.name?.contains(query, ignoreCase = true) ?: false)
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
            SortType.RATING_HIGH_LOW -> displayedServices.sortedByDescending { it.rating?.toFloatOrNull() ?: 0f }
        }
        notifyDataSetChanged()
    }
}



