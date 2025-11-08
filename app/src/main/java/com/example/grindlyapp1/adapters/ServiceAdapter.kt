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
    private var allServices: List<Service>,
    private val onClick: (Service) -> Unit,
    private val onFavouriteClicked: ((Service) -> Unit)? = null,
    private val onSubmitClicked: ((Review) -> Unit)? = null
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
        Log.d("ServiceAdapter", "Binding service at position $position: ${service.title}, isFavourite=${service.isFavourite}, workSampleURL=${service.workImageURL}")

        // Bind text
        holder.binding.serviceTitle.text = service.title ?: "Untitled Service"
        holder.binding.hustlerName.text = service.name ?: "Unknown Hustler"
        holder.binding.price.text =
            service.price?.let { "R$it · ${service.pricingModel}" } ?: "Price N/A"
        holder.binding.serviceLocation.text = service.location ?: "Location unknown"
        holder.binding.ratingBar.rating = service.rating?.toFloatOrNull() ?: 0f


        Glide.with(holder.itemView.context)
            .load(service.profilePictureURL.takeIf { !it.isNullOrBlank() } ?: R.drawable.ic_profile)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .centerCrop()
            .into(holder.binding.profilePic)

        Glide.with(holder.itemView.context)
            .load(service.workImageURL.takeIf { !it.isNullOrBlank() } ?: R.drawable.ic_profile)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.binding.thumbnail)



        holder.binding.btnFavourite.setImageResource(
            if (service.isFavourite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )


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

        holder.binding.btnSubmitReview.setOnClickListener{
            val userRating = holder.binding.userRatingBar.rating
            val userComment = holder.binding.userComment.text.toString().trim()

            if(userRating == 0f){
                Log.d("ServiceAdapter", "Rating not selected")
                return@setOnClickListener
            }

            if(userComment.isEmpty()){
                Log.d("ServiceAdapter", "Comment is empty")
                return@setOnClickListener
            }

            val review = Review(
                id = service.id ?: " ",
                rating = userRating.toDouble(),
                comment = userComment,
                reviewerName = " "

            )

            Log.d("ServiceAdapter", "Submitting review for ${service.title}: $review")

            onSubmitClicked?.invoke(review)

            holder.binding.userRatingBar.rating = 0f
            holder.binding.userComment.setText("")
        }

        holder.itemView.setOnClickListener {
            Log.d("ServiceAdapter", "Card clicked for ${service.title}")
            Log.d("ServiceAdapter", "Service Id: ${service.id}")
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

