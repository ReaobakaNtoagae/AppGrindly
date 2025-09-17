package com.example.grindlyapp1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: ShapeableImageView
    private lateinit var editServiceTitle: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var editPhoneNumber: EditText
    private lateinit var editPrice: EditText
    private lateinit var pricingSpinner: Spinner
    private lateinit var editDescription: EditText
    private lateinit var btnUploadImages: Button
    private lateinit var btnUploadDocs: Button
    private lateinit var btnSubmit: Button
    private lateinit var imagesRecycler: RecyclerView
    private lateinit var docsRecycler: RecyclerView

    private val imageUris = mutableListOf<Uri>()
    private val docUris = mutableListOf<Uri>()
    private val client = OkHttpClient()

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
        private const val PICK_DOC_REQUEST = 101
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Bind views
        profileImageView = view.findViewById(R.id.profileImageView)
        editServiceTitle = view.findViewById(R.id.editServiceTitle)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        editPhoneNumber = view.findViewById(R.id.editPhoneNumber)
        editPrice = view.findViewById(R.id.editPrice)
        pricingSpinner = view.findViewById(R.id.pricingSpinner)
        editDescription = view.findViewById(R.id.editDescription)
        btnUploadImages = view.findViewById(R.id.btnUploadImages)
        btnUploadDocs = view.findViewById(R.id.btnUploadDocs)
        imagesRecycler = view.findViewById(R.id.imagesRecycler)
        docsRecycler = view.findViewById(R.id.docsRecycler)
        btnSubmit = view.findViewById(R.id.btnSubmitProfile)

        setupSpinners()

        btnUploadImages.setOnClickListener { pickImages() }
        btnUploadDocs.setOnClickListener { pickDocuments() }
        btnSubmit.setOnClickListener { submitProfile() }

        return view
    }

    private fun setupSpinners() {
        val categories = listOf("Cleaning", "Tutoring", "Plumbing", "Other")
        categorySpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)

        val pricingTypes = listOf("Per Hour", "Per Day", "Per Job")
        pricingSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, pricingTypes)
    }

    private fun pickImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST)
    }

    private fun pickDocuments() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Select Documents"), PICK_DOC_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    data?.let {
                        if (it.clipData != null) {
                            for (i in 0 until it.clipData!!.itemCount) {
                                imageUris.add(it.clipData!!.getItemAt(i).uri)
                            }
                        } else {
                            it.data?.let { uri -> imageUris.add(uri) }
                        }
                    }
                    // TODO: Update images RecyclerView adapter here
                }
                PICK_DOC_REQUEST -> {
                    data?.let {
                        if (it.clipData != null) {
                            for (i in 0 until it.clipData!!.itemCount) {
                                docUris.add(it.clipData!!.getItemAt(i).uri)
                            }
                        } else {
                            it.data?.let { uri -> docUris.add(uri) }
                        }
                    }
                    // TODO: Update docs RecyclerView adapter here
                }
            }
        }
    }

    private fun submitProfile() {
        val serviceProfile = JSONObject().apply {
            put("title", editServiceTitle.text.toString())
            put("category", categorySpinner.selectedItem.toString())
            put("phone", editPhoneNumber.text.toString())
            put("price", editPrice.text.toString())
            put("pricingType", pricingSpinner.selectedItem.toString())
            put("description", editDescription.text.toString())
        }

        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        bodyBuilder.addFormDataPart("data", serviceProfile.toString())

        imageUris.forEach { uri ->
            val file = File(getRealPathFromURI(uri))
            bodyBuilder.addFormDataPart(
                "images",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        docUris.forEach { uri ->
            val file = File(getRealPathFromURI(uri))
            bodyBuilder.addFormDataPart(
                "documents",
                file.name,
                file.asRequestBody("*/*".toMediaTypeOrNull())
            )
        }

        val request = Request.Builder()
            .url("http://10.0.2.2:5001/progapi-33199/us-central1/api/serviceprofile")
            .post(bodyBuilder.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Profile submitted successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun getRealPathFromURI(contentUri: Uri): String {
        var filePath = ""
        val cursor = activity?.contentResolver?.query(contentUri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                if (idx >= 0) filePath = it.getString(idx)
            }
            it.close()
        }
        return filePath
    }
}

