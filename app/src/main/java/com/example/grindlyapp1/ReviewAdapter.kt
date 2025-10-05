package com.example.grindlyapp1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.grindlyapp1.databinding.ItemReviewBinding
import com.example.grindlyapp1.models.Review

class ReviewAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(val binding: ItemReviewBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.binding.txtReviewerName.text = review.reviewerName
        holder.binding.txtReviewText.text = review.comment
        holder.binding.ratingBar.numStars = review.rating.toInt()


    }

    override fun getItemCount() = reviews.size


}