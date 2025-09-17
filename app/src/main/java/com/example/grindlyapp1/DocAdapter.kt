package com.example.grindlyapp1

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DocAdapter(private val docs: List<Uri>) :
    RecyclerView.Adapter<DocAdapter.DocumentViewHolder>() {

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val docName: TextView = itemView.findViewById(R.id.docNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_documentpreviews, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val uri = docs[position]
        holder.docName.text = uri.lastPathSegment ?: "Document ${position + 1}"
    }

    override fun getItemCount(): Int = docs.size
}
