package com.example.grindlyapp1.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.grindlyapp1.R
import com.example.grindlyapp1.databinding.ItemVerifydocsBinding
import com.example.grindlyapp1.network.HustlerProfile
import com.google.android.material.chip.Chip

class VerificationAdapter(
    private val context: Context,
    private val hustlers: MutableList<HustlerProfile>,
    private val onApprove: (HustlerProfile) -> Unit,
    private val onReject: (HustlerProfile) -> Unit
) : RecyclerView.Adapter<VerificationAdapter.VerificationViewHolder>() {

    inner class VerificationViewHolder(val binding: ItemVerifydocsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerificationViewHolder {
        val binding = ItemVerifydocsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VerificationViewHolder(binding)
    }

    override fun getItemCount(): Int = hustlers.size

    override fun onBindViewHolder(holder: VerificationViewHolder, position: Int) {
        val hustler = hustlers[position]
        val b = holder.binding

        // Hustler name + badge
        b.tvHustlerName.text = hustler.name.ifBlank { "Unknown Hustler" }
        if (hustler.verificationStatus != "unverified") {
            val badge = ContextCompat.getDrawable(context, R.drawable.verified_badge)
            b.tvHustlerName.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, badge, null)
            b.tvHustlerName.compoundDrawablePadding = 8
        } else {
            b.tvHustlerName.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }

        // Display document URLs as chips
        b.chipGroupDocs.removeAllViews()
        hustler.documentURLs?.forEach { url ->
            val chip = Chip(context).apply {
                text = url.substringAfterLast("/")
                isClickable = true
                isCheckable = false
                setOnClickListener { openDocument(url) }
            }
            b.chipGroupDocs.addView(chip)
        }


        b.approveBtn.setOnClickListener { onApprove(hustler) }
        b.disapproveBtn.setOnClickListener { onReject(hustler) }
    }


    private fun openDocument(url: String) {
        try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Open document"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Update adapter data
    fun updateData(newList: List<HustlerProfile>) {
        hustlers.clear()
        hustlers.addAll(newList)
        notifyDataSetChanged()
    }
}
