package com.example.grindlyapp1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.R
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.network.Service
import com.example.grindlyapp1.viewmodel.ServiceViewModel

class FavouritesAdapter(
    private var favourites: List<Service>,
    private val viewModel: ServiceViewModel,
    private val onClick: (Service) -> Unit
) : RecyclerView.Adapter<FavouritesAdapter.FavouriteViewHolder>() {

    inner class FavouriteViewHolder(val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = ItemServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavouriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        val service = favourites[position]

        // Texts
        holder.binding.serviceTitle.text = service.title ?: "Untitled Service"
        holder.binding.hustlerName.text = service.name ?: "Unknown Hustler"
        holder.binding.price.text =
            service.price?.let { "R$it · ${service.pricingModel}" } ?: "Price N/A"
        holder.binding.serviceLocation.text = service.location ?: "Location unknown"
        holder.binding.ratingBar.rating = service.rating?.toFloat() ?: 0f
        holder.binding.reviewCount.text = "(${service.reviewCount ?: 0})"

        // Work image
        Glide.with(holder.itemView.context)
            .load(service.workImageURL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .centerCrop()
            .into(holder.binding.thumbnail)

        // Profile picture
        Glide.with(holder.itemView.context)
            .load(service.profilePictureURL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .centerCrop()
            .into(holder.binding.profilePic)

        // Heart icon toggle
        holder.binding.btnFavourite.setImageResource(
            if (service.isFavourite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        holder.binding.btnFavourite.setOnClickListener {
            // Offline-first toggle handled by ViewModel
            viewModel.toggleFavourite(service)
        }

        // Item click callback
        holder.itemView.setOnClickListener { onClick(service) }
    }

    override fun getItemCount(): Int = favourites.size

    /**
     * Updates the list with DiffUtil for smooth RecyclerView animations.
     */
    fun updateList(newList: List<Service>) {
        val diffCallback = ServiceDiffCallback(favourites, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        favourites = newList
        diffResult.dispatchUpdatesTo(this)
    }

    // -------------------------------
    // DiffUtil for smooth updates
    // -------------------------------
    class ServiceDiffCallback(
        private val oldList: List<Service>,
        private val newList: List<Service>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}
