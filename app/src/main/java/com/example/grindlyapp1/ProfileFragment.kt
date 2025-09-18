package com.example.grindlyapp1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grindlyapp1.network.UserProfileUpdateRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private lateinit var titleInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var locationInput: EditText
    private lateinit var priceInput: EditText
    private lateinit var pricingSpinner: Spinner
    private lateinit var descriptionInput: EditText
    private lateinit var uploadImagesButton: Button
    private lateinit var uploadDocsButton: Button
    private lateinit var saveButton: Button

    private lateinit var imagesRecycler: RecyclerView
    private lateinit var docsRecycler: RecyclerView

    private var imageUris = mutableListOf<String>() // placeholder URLs
    private var docUris = mutableListOf<String>()   // placeholder URLs
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Bind views
        titleInput = view.findViewById(R.id.editServiceTitle)
        categorySpinner = view.findViewById(R.id.editCategory)
        locationInput = view.findViewById(R.id.editLocation)
        priceInput = view.findViewById(R.id.editPrice)
        pricingSpinner = view.findViewById(R.id.editPricingModel)
        descriptionInput = view.findViewById(R.id.editDescription)
        uploadImagesButton = view.findViewById(R.id.btnUploadImages)
        uploadDocsButton = view.findViewById(R.id.btnUploadDocs)
        saveButton = view.findViewById(R.id.btnSubmit)
        imagesRecycler = view.findViewById(R.id.imagesRecycler)
        docsRecycler = view.findViewById(R.id.docRecycler)

        imagesRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        docsRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)


        return view
    }


}
