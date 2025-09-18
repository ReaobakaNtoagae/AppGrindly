package com.example.grindlyapp1

import android.app.Activity
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
import com.example.grindlyapp1.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var titleInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var locationInput: EditText
    private lateinit var priceInput: EditText
    private lateinit var pricingModelSpinner: Spinner
    private lateinit var descriptionInput: EditText
    private lateinit var submitButton: Button
    private lateinit var btnUploadImages: Button
    private lateinit var btnUploadDocs: Button
    private lateinit var imageRecycler: RecyclerView
    private lateinit var docRecycler: RecyclerView

    private val imageUris = mutableListOf<Uri>()
    private val docUris = mutableListOf<Uri>()
    private var profilePicUri: Uri? = null

    private lateinit var imageAdapter: ImageAdapter
    private lateinit var docAdapter: DocAdapter

    private var userId: String = "" // Set this from SharedPreferences or arguments

    companion object {
        private const val PICK_PROFILE_PIC = 50
        private const val PICK_IMAGES = 100
        private const val PICK_DOCS = 200
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImageView = view.findViewById(R.id.editProfile)
        titleInput = view.findViewById(R.id.editServiceTitle)
        categorySpinner = view.findViewById(R.id.editCategory)
        locationInput = view.findViewById(R.id.editLocation)
        priceInput = view.findViewById(R.id.editPrice)
        pricingModelSpinner = view.findViewById(R.id.editPricingModel)
        descriptionInput = view.findViewById(R.id.editDescription)
        submitButton = view.findViewById(R.id.btnSubmit)
        btnUploadImages = view.findViewById(R.id.btnUploadImages)
        btnUploadDocs = view.findViewById(R.id.btnUploadDocs)
        imageRecycler = view.findViewById(R.id.imagesRecycler)
        docRecycler = view.findViewById(R.id.docRecycler)

        imageAdapter = ImageAdapter(imageUris)
        docAdapter = DocAdapter(docUris)
        imageRecycler.adapter = imageAdapter
        docRecycler.adapter = docAdapter
        imageRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        docRecycler.layoutManager = LinearLayoutManager(requireContext())

        profileImageView.setOnClickListener { openProfilePicPicker() }
        btnUploadImages.setOnClickListener { openImagePicker() }
        btnUploadDocs.setOnClickListener { openDocPicker() }
        submitButton.setOnClickListener { submitProfile() }

        // Fetch userId from SharedPreferences
        val prefs = requireContext().getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
        userId = prefs.getString("USER_ID", "") ?: ""

        if (userId.isNotEmpty()) {
            fetchProfile()
        }

        return view
    }

    private fun fetchProfile() {
        RetrofitClient.instance.getProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { profile ->
                        // Set text fields
                        titleInput.setText(profile.title)
                        locationInput.setText(profile.location)
                        priceInput.setText(profile.price)
                        descriptionInput.setText(profile.description)

                        // Set spinners
                        setSpinnerSelection(categorySpinner, profile.category)
                        setSpinnerSelection(pricingModelSpinner, profile.pricingModel)

                        // Load images & documents
                        profilePicUri = profile.profilePictureURL?.let { Uri.parse(it) }
                        profilePicUri?.let { profileImageView.setImageURI(it) }

                        imageUris.clear()
                        imageUris.addAll(profile.workImageURLs?.map { Uri.parse(it) } ?: emptyList())
                        imageAdapter.notifyDataSetChanged()

                        docUris.clear()
                        docUris.addAll(profile.documentURLs?.map { Uri.parse(it) } ?: emptyList())
                        docAdapter.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Helper function to select spinner value
    private fun setSpinnerSelection(spinner: Spinner, value: String?) {
        if (value == null) return
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString().equals(value, ignoreCase = true)) {
                spinner.setSelection(i)
                break
            }
        }
    }


    private fun submitProfile() {
        val profileRequest = ProfileRequest(
            userId = userId,
            title = titleInput.text.toString().trim(),
            category = categorySpinner.selectedItem.toString(),
            location = locationInput.text.toString().trim(),
            price = priceInput.text.toString().trim(),
            pricingModel = pricingModelSpinner.selectedItem.toString(),
            description = descriptionInput.text.toString().trim(),
            profilePictureURL = profilePicUri?.toString(),
            workImageURLs = imageUris.map { it.toString() },
            documentURLs = docUris.map { it.toString() },
            verifiedBadgeTier = "none",
            servicePackages = null,
            packageStatus = "skipped"
        )

        RetrofitClient.instance.createOrUpdateProfile(profileRequest).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), response.body()?.message ?: "Profile updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openProfilePicPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, PICK_PROFILE_PIC)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_IMAGES)
    }

    private fun openDocPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_DOCS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                PICK_PROFILE_PIC -> {
                    data.data?.let {
                        profilePicUri = it
                        profileImageView.setImageURI(it)
                    }
                }
                PICK_IMAGES -> {
                    if (data.clipData != null) {
                        for (i in 0 until data.clipData!!.itemCount) {
                            imageUris.add(data.clipData!!.getItemAt(i).uri)
                        }
                    } else data.data?.let { imageUris.add(it) }
                    imageAdapter.notifyDataSetChanged()
                }
                PICK_DOCS -> {
                    if (data.clipData != null) {
                        for (i in 0 until data.clipData!!.itemCount) {
                            docUris.add(data.clipData!!.getItemAt(i).uri)
                        }
                    } else data.data?.let { docUris.add(it) }
                    docAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}
