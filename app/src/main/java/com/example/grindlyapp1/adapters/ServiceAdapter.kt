package com.example.grindlyapp1.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.R
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.network.Review
import com.example.grindlyapp1.network.Service

class ServiceAdapter(
    services: List<Service>,
    private val onClick: (Service) -> Unit,
    private val onFavouriteClicked: ((Service) -> Unit)? = null,
    private val onSubmitClicked: ((Review) -> Unit)? = null
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    private var allServices: List<Service> = services
    private var displayedServices: List<Service> = services

    inner class ServiceViewHolder(val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServiceViewHolder(binding)
    }

    override fun getItemCount(): Int = displayedServices.size

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = displayedServices[position]


        // --- Bind Text ---
        holder.binding.apply {
            serviceTitle.text = service.title ?: "Untitled Service"
            hustlerName.text = service.name ?: "Unknown Hustler"
            price.text = service.price?.let { "R$it · ${service.pricingModel}" } ?: "Price N/A"
            serviceLocation.text = service.location ?: "Location unknown"
            holder.binding.ratingBar.rating = service.rating.toFloat()


            Glide.with(root.context)
                .load(service.profilePictureURL.takeIf { !it.isNullOrBlank() } ?: R.drawable.ic_profile)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .centerCrop()
                .into(profilePic)

            Glide.with(root.context)
                .load(service.workImageURL.takeIf { !it.isNullOrBlank() } ?: R.drawable.ic_profile)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(thumbnail)

            btnFavourite.setImageResource(
                if (service.isFavourite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )


            // --- Favourite toggle ---
            btnFavourite.setOnClickListener {
                val updated = service.copy(isFavourite = !service.isFavourite)
                btnFavourite.setImageResource(
                    if (updated.isFavourite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
                onFavouriteClicked?.invoke(updated)
            }


            // --- Submit review ---
            btnSubmitReview.setOnClickListener {
                val rating = userRatingBar.rating
                val comment = userComment.text.toString().trim()
                if (rating > 0f && comment.isNotEmpty()) {
                    val review = Review(
                        id = service.id ?: "",
                        rating = rating.toDouble(),
                        comment = comment,
                        reviewerName = ""
                    )
                    onSubmitClicked?.invoke(review)
                    userRatingBar.rating = 0f
                    userComment.setText("")
                }
            }

            root.setOnClickListener { onClick(service) }
        }
    }

    // --- Update entire list ---
    fun updateList(newList: List<Service>) {
        allServices = newList
        displayedServices = newList
        notifyDataSetChanged()
    }

    // --- Filtering ---
    fun filterBySearch(query: String) {
        displayedServices = if (query.isBlank()) allServices
        else allServices.filter {
            it.title.orEmpty().contains(query, true)
                    || it.name.orEmpty().contains(query, true)
                    || it.category.orEmpty().contains(query, true)
        }
        notifyDataSetChanged()
    }


    fun filterByCategory(category: String?) {
        displayedServices = if (category.isNullOrBlank()) allServices
        else allServices.filter { it.category == category }
        notifyDataSetChanged()
    }


    // --- Sorting ---
    enum class SortType { PRICE_LOW_HIGH, PRICE_HIGH_LOW, RATING_HIGH_LOW }

    fun sortBy(type: SortType) {
        displayedServices = when (type) {
            SortType.PRICE_LOW_HIGH -> displayedServices.sortedBy { it.price }
            SortType.PRICE_HIGH_LOW -> displayedServices.sortedByDescending { it.price }
            SortType.RATING_HIGH_LOW -> displayedServices.sortedByDescending { it.rating }
        }
        notifyDataSetChanged()
    }


}
