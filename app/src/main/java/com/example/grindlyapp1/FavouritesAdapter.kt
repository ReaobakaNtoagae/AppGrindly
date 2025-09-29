package com.example.grindlyapp1

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.models.Service

class FavouritesAdapter(
    private var favourites: List<Service>,
    private val onClick: (Service) -> Unit,
    private val onFavouriteClicked: ((Service) -> Unit)? = null
) : RecyclerView.Adapter<FavouritesAdapter.FavouriteViewHolder>() {

    inner class FavouriteViewHolder(val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = ItemServiceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FavouriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        val service = favourites[position]

        // --- Bind text & images ---
        holder.binding.serviceTitle.text = service.title ?: "Untitled Service"
        holder.binding.hustlerName.text = service.name ?: "Unknown Hustler"
        holder.binding.price.text =
            service.price?.let { "R$it · ${service.pricingModel}" } ?: "Price N/A"
        holder.binding.serviceLocation.text = service.location ?: "Location unknown"
        holder.binding.ratingBar.rating = service.rating?.toFloatOrNull() ?: 0f

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

        // --- Heart icon ---
        holder.binding.btnFavourite.setImageResource(
            if (service.isFavourite) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )

        // --- Favourite toggle ---
        holder.binding.btnFavourite.setOnClickListener {
            Log.d("FavouritesAdapter", "Favourite clicked: ${service.title}, current state=${service.isFavourite}")
            val updatedService = service.copy(isFavourite = !service.isFavourite)
            onFavouriteClicked?.invoke(updatedService)
            // UI will update automatically via LiveData observer in fragment
        }

        // --- Card click ---
        holder.itemView.setOnClickListener { onClick(service) }
    }

    override fun getItemCount(): Int = favourites.size

    fun updateList(newList: List<Service>) {
        Log.d("FavouritesAdapter", "Updating favourites list: ${newList.size} items")
        favourites = newList
        notifyDataSetChanged()
    }
}
