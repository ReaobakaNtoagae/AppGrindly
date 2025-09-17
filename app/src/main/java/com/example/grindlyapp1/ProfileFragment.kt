package com.example.grindlyapp1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grindlyapp1.R
import com.google.android.material.imageview.ShapeableImageView
import network.ApiResponse
import network.ProfileApiService
import network.RetrofitClient
import network.UserProfileUpdateRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: ShapeableImageView
    private lateinit var titleInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var phoneInput: EditText
    private lateinit var priceInput: EditText
    private lateinit var pricingSpinner: Spinner
    private lateinit var descriptionInput: EditText

    private lateinit var imagesRecycler: RecyclerView
    private lateinit var docsRecycler: RecyclerView
    private lateinit var btnUploadImages: Button
    private lateinit var btnUploadDocs: Button
    private lateinit var btnSubmit: Button

    private var userId: String? = null
    private val selectedImages = mutableListOf<Uri>()
    private val selectedDocs = mutableListOf<Uri>()

    companion object {
        private const val PICK_IMAGES = 100
        private const val PICK_DOCS = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId") // or get from intent if Activity
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val view = inflater.inflate(R.layout.fragment_manage_profile, container, false)

        // Bind views
        profileImageView = view.findViewById(R.id.profileImageView)
        titleInput = view.findViewById(R.id.editServiceTitle)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        phoneInput = view.findViewById(R.id.editPhoneNumber)
        priceInput = view.findViewById(R.id.editPrice)
        pricingSpinner = view.findViewById(R.id.pricingSpinner)
        descriptionInput = view.findViewById(R.id.editDescription)

        imagesRecycler = view.findViewById(R.id.imagesRecycler)
        docsRecycler = view.findViewById(R.id.docsRecycler)
        btnUploadImages = view.findViewById(R.id.btnUploadImages)
        btnUploadDocs = view.findViewById(R.id.btnUploadDocs)
        btnSubmit = view.findViewById(R.id.btnSubmit)

        // Setup RecyclerViews
        imagesRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        imagesRecycler.adapter = ImageAdapter(selectedImages)

        docsRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        docsRecycler.adapter = DocumentAdapter(selectedDocs)

        // Button click listeners
        btnUploadImages.setOnClickListener { pickImages() }
        btnUploadDocs.setOnClickListener { pickDocs() }
        btnSubmit.setOnClickListener { submitProfile() }

        // Load profile if exists
        loadProfile()

        return view
    }

    private fun pickImages() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_IMAGES)
    }

    private fun pickDocs() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_DOCS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return

        when (requestCode) {
            PICK_IMAGES -> {
                if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        selectedImages.add(data.clipData!!.getItemAt(i).uri)
                    }
                } else {
                    data.data?.let { selectedImages.add(it) }
                }
                imagesRecycler.adapter?.notifyDataSetChanged()
            }
            PICK_DOCS -> {
                if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        selectedDocs.add(data.clipData!!.getItemAt(i).uri)
                    }
                } else {
                    data.data?.let { selectedDocs.add(it) }
                }
                docsRecycler.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun loadProfile() {
        userId?.let { uid ->
            val api = RetrofitClient.instance.create(ProfileApiService::class.java)
            api.getProfile(uid).enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful) {
                        val profile = response.body()
                        titleInput.setText(profile?.get("title") as? String ?: "")
                        phoneInput.setText(profile?.get("phone") as? String ?: "")
                        priceInput.setText(profile?.get("price") as? String ?: "")
                        descriptionInput.setText(profile?.get("description") as? String ?: "")
                        // TODO: load images/docs URIs if needed
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    Toast.makeText(requireContext(), "Failed to load profile: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun submitProfile() {
        val title = titleInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val price = priceInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()

        if (title.isEmpty() || phone.isEmpty() || price.isEmpty()) {
            Toast.makeText(requireContext(), "Title, phone, and price are required", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UserProfileUpdateRequest(
            userId = userId ?: "unknown_user",
            title = title,
            category = categorySpinner.selectedItem.toString(),
            phone = phone,
            price = price,
            pricingModel = pricingSpinner.selectedItem.toString(),
            description = description,
            imageUris = selectedImages.map { it.toString() },
            docUris = selectedDocs.map { it.toString() }
        )

        val api = RetrofitClient.instance.create(ProfileApiService::class.java)
        api.updateProfile(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    // Navigate to ServicePackage
                    val intent = Intent(requireContext(), ServicePackage::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
