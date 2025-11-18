package com.example.grindlyapp1.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.grindlyapp1.R

class DocAdapter(private val documents: List<Uri>) :
    RecyclerView.Adapter<DocAdapter.DocViewHolder>() {

    inner class DocViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val docNameText: TextView = view.findViewById(R.id.docNameText)
        val docIcon: ImageView = view.findViewById(R.id.docIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_documentpreviews, parent, false)
        return DocViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocViewHolder, position: Int) {
        val uri = documents[position]
        val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "document"

        holder.docNameText.text = fileName


        Glide.with(holder.docIcon.context)
            .load(R.drawable.ic_document)
            .into(holder.docIcon)
    }

    override fun getItemCount() = documents.size
}
