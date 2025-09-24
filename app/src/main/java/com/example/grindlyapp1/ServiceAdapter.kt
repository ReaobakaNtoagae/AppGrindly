package com.example.grindlyapp1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.databinding.ItemServiceBinding
import com.example.grindlyapp1.models.Service

class ServiceAdapter(
    private var services: List<Service>,
    private val onClick: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        holder.binding.serviceTitle.text = service.serviceTitle
        holder.binding.hustlerName.text = service.hustlerName
        holder.binding.price.text = "R${service.price}"
        holder.binding.rating.text = "⭐ ${service.rating}"

        Glide.with(holder.itemView.context)
            .load(service.thumbnailUrl)
            .into(holder.binding.thumbnail)

        holder.itemView.setOnClickListener { onClick(service) }
    }

    override fun getItemCount() = services.size

    fun updateList(newList: List<Service>) {
        services = newList
        notifyDataSetChanged()
    }
}
