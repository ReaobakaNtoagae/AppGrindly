package com.example.grindlyapp1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.databinding.ItemWorkSampleBinding

class WorkSamplesAdapter(private val images: List<String>) :
    RecyclerView.Adapter<WorkSamplesAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemWorkSampleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkSampleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(images[position])
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .centerCrop()
            .into(holder.binding.workSampleImage)
    }

    override fun getItemCount() = images.size
}
