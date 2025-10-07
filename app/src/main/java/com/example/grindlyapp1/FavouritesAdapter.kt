package com.example.grindlyapp1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.models.Service
import com.example.grindlyapp1.viewmodels.ServiceViewModel

class FavouritesAdapter(
    private var favourites: List<Service>,
    private val viewModel: ServiceViewModel,
    private val userToken: String,
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

        holder.binding.serviceTitle.text = service.title ?: "Untitled Service"
        holder.binding.hustlerName.text = service.name ?: "Unknown Hustler"
        holder.binding.price.text = service.price?.let { "R$it · ${service.pricingModel}" } ?: "Price N/A"
        holder.binding.serviceLocation.text = service.location ?: "Location unknown"
        holder.binding.ratingBar.rating = service.rating?.toFloatOrNull() ?: 0f
        holder.binding.reviewCount.text = "(${service.reviewCount ?: 0})"

        Glide.with(holder.itemView.context)
            .load(service.workImageURL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.binding.thumbnail)

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
            val updatedService = service.copy(isFavourite = !service.isFavourite)
            viewModel.toggleFavourite(updatedService, userToken)
        }

        holder.itemView.setOnClickListener { onClick(service) }
    }

    override fun getItemCount(): Int = favourites.size

    fun updateList(newList: List<Service>) {
        favourites = newList
        notifyDataSetChanged()
    }
}
