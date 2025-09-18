package com.example.grindlyapp1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grindlyapp1.R
import com.example.grindlyapp1.network.ApiResponse
import com.example.grindlyapp1.network.ProfileRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateProfile : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var locationInput: EditText
    private lateinit var priceInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var submitButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var btnUploadImg: Button
    private lateinit var btnUploadDocs: Button

    private lateinit var imageRecycler: RecyclerView
    private lateinit var docRecycler: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var documentAdapter: DocAdapter

    private val imageUris = mutableListOf<Uri>()
    private val docUris = mutableListOf<Uri>()
    private var profilePicUri: Uri? = null

    companion object {
        private const val PICK_PROFILE_PIC = 50
        private const val PICK_IMAGES = 100
        private const val PICK_DOCS = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createprofile)

        // Inputs
        titleInput = findViewById(R.id.editServiceTitle)
        categorySpinner = findViewById(R.id.categorySpinner)
        locationInput = findViewById(R.id.editLocation)
        priceInput = findViewById(R.id.editPrice)
        descriptionInput = findViewById(R.id.editDescription)
        submitButton = findViewById(R.id.btnDone)
        profileImageView = findViewById(R.id.profileImageView)
        btnUploadImg = findViewById(R.id.btnUploadImg)
        btnUploadDocs = findViewById(R.id.browsedocuments)

        // RecyclerViews
        imageRecycler = findViewById(R.id.imageRecycler)
        docRecycler = findViewById(R.id.docRecycler)

        imageAdapter = ImageAdapter(imageUris)
        documentAdapter = DocAdapter(docUris)

        imageRecycler.adapter = imageAdapter
        docRecycler.adapter = documentAdapter

        imageRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        docRecycler.layoutManager = LinearLayoutManager(this)

        // Upload buttons
        profileImageView.setOnClickListener { openProfilePicPicker() }
        btnUploadImg.setOnClickListener { openImagePicker() }
        btnUploadDocs.setOnClickListener { openDocPicker() }

        // Submit
        submitButton.setOnClickListener { submitProfile() }
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
                            val uri = data.clipData!!.getItemAt(i).uri
                            imageUris.add(uri)
                        }
                    } else {
                        data.data?.let { imageUris.add(it) }
                    }
                    imageAdapter.notifyDataSetChanged()
                }

                PICK_DOCS -> {
                    if (data.clipData != null) {
                        for (i in 0 until data.clipData!!.itemCount) {
                            val uri = data.clipData!!.getItemAt(i).uri
                            docUris.add(uri)
                        }
                    } else {
                        data.data?.let { docUris.add(it) }
                    }
                    documentAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun submitProfile() {
        val title = titleInput.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val location = locationInput.text.toString().trim()
        val price = priceInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()

        if (title.isEmpty() || category.isEmpty() || description.length < 250) {
            Toast.makeText(
                this,
                "Please fill in all required fields. Description must be at least 250 characters.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val profileRequest = ProfileRequest(
            userId = "user789",
            title = title,
            category = category,
            location = location,
            price = price,
            description = description,
            profilePictureURL = profilePicUri?.toString() ?: "https://example.com/profile.jpg",
            workImageURLs = imageUris.map { it.toString() },
            documentURLs = docUris.map { it.toString() },
            verifiedBadgeTier = "none",
            servicePackages = null,
            packageStatus = "skipped"
        )

        val api = RetrofitClient.instance.create(ProfileApiService::class.java)
        api.createOrUpdateProfile(profileRequest).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@CreateProfile,
                        response.body()?.message ?: "Profile created",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@CreateProfile, ServicePackage::class.java)
                    intent.putExtra("userId", profileRequest.userId)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Toast.makeText(
                        this@CreateProfile,
                        "Server error: ${response.code()} - $errorMsg",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(
                    this@CreateProfile,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
