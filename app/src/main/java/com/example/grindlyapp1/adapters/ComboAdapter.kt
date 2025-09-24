package com.example.grindlyapp1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.databinding.ItemHustlerBinding
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.models.HustlerProfile
import com.example.grindlyapp1.models.Service

class ComboAdapter(
    private var services: List<Service> = emptyList(),
    private var hustlers: List<HustlerProfile> = emptyList(),
    private val onServiceClick: (Service) -> Unit,
    private val onHustlerClick: (HustlerProfile) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SERVICE = 1
        private const val TYPE_HUSTLER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < services.size) TYPE_SERVICE else TYPE_HUSTLER
    }

    override fun getItemCount(): Int = services.size + hustlers.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SERVICE) {
            val binding = ItemServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ServiceViewHolder(binding)
        } else {
            val binding = ItemHustlerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HustlerViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ServiceViewHolder) {
            val service = services[position]
            holder.binding.serviceTitle.text = service.serviceTitle
            holder.binding.hustlerName.text = service.hustlerName
            holder.binding.price.text = "R${service.price}"
            holder.binding.rating.text = "⭐ ${service.rating}"
            Glide.with(holder.itemView.context).load(service.thumbnailUrl).into(holder.binding.thumbnail)
            holder.itemView.setOnClickListener { onServiceClick(service) }
        } else if (holder is HustlerViewHolder) {
            val index = position - services.size
            val hustler = hustlers[index]
            holder.binding.hustlerName.text = hustler.name
            holder.binding.serviceTitle.text = hustler.serviceTitle
            holder.binding.price.text = "R${hustler.price}"
            Glide.with(holder.itemView.context).load(hustler.profilePicUrl).into(holder.binding.thumbnail)
            holder.itemView.setOnClickListener { onHustlerClick(hustler) }
        }
    }

    fun updateData(newServices: List<Service>, newHustlers: List<HustlerProfile>) {
        services = newServices
        hustlers = newHustlers
        notifyDataSetChanged()
    }

    class ServiceViewHolder(val binding: ItemServiceBinding) : RecyclerView.ViewHolder(binding.root)
    class HustlerViewHolder(val binding: ItemHustlerBinding) : RecyclerView.ViewHolder(binding.root)
}
