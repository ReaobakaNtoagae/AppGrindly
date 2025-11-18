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
import com.bumptech.glide.Glide
import com.example.grindlyapp1.adapters.DocAdapter
import com.example.grindlyapp1.adapters.ImageAdapter
import com.example.grindlyapp1.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    // UI Components
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

    // Media Storage
    private val imageUris = mutableListOf<Uri>()
    private val docUris = mutableListOf<Uri>()
    private var profilePicUri: Uri? = null

    // Adapters
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var docAdapter: DocAdapter

    // User Data
    private var userId: String = ""
    private var token: String = " "

    private var fetchedPackages: List<ServicePackage> = emptyList()

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
        initializeViews(view)
        initializeAdapters()
        initializeClickListeners()
        loadPrefs()

        if (userId.isNotEmpty()) {
            fetchProfile()
        }

        return view
    }

    // ---------------------------
    // Initialization
    // ---------------------------

    private fun initializeViews(view: View) {
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
    }

    private fun initializeAdapters() {
        imageAdapter = ImageAdapter(imageUris)
        docAdapter = DocAdapter(docUris)

        imageRecycler.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        docRecycler.apply {
            adapter = docAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun initializeClickListeners() {
        profileImageView.setOnClickListener { openProfilePicPicker() }
        btnUploadImages.setOnClickListener { openImagePicker() }
        btnUploadDocs.setOnClickListener { openDocPicker() }
        submitButton.setOnClickListener { submitProfile() }
    }

    private fun loadPrefs() {
        val prefs = requireContext().getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
        userId = prefs.getString("USER_ID", "") ?: ""
        token = prefs.getString("TOKEN", " ") ?: " "
    }

    // ---------------------------
    // Fetch Profile
    // ---------------------------

    private fun fetchProfile() {
        RetrofitClient.getClient(requireContext()).getProfile(token, userId)
            .enqueue(object : Callback<ProfileResponse> {

                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    if (!response.isSuccessful) {
                        showToast("Failed to fetch profile")
                        return
                    }

                    response.body()?.let { profile ->
                        populateProfileData(profile)
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    showToast("Network error: ${t.message}")
                }
            })
    }

    private fun populateProfileData(profile: ProfileResponse) {
        titleInput.setText(profile.title ?: "")
        locationInput.setText(profile.location ?: "")
        priceInput.setText(profile.price?.let { String.format("%.2f", it) } ?: "")
        descriptionInput.setText(profile.description ?: "")

        setSpinnerSelection(categorySpinner, profile.category)
        setSpinnerSelection(pricingModelSpinner, profile.pricingModel)

        profile.profilePictureURL?.let {
            profilePicUri = Uri.parse(it)
            Glide.with(this)
                .load(profilePicUri)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_menu_gallery)
                .into(profileImageView)
        }

        imageUris.apply {
            clear()
            addAll(profile.workImageURLs?.map { Uri.parse(it) } ?: emptyList())
        }
        imageAdapter.notifyDataSetChanged()

        docUris.apply {
            clear()
            addAll(profile.documentURLs?.map { Uri.parse(it) } ?: emptyList())
        }
        docAdapter.notifyDataSetChanged()

        fetchedPackages = (profile.servicePackages?.filterNotNull() ?: emptyList()) as List<ServicePackage>

    }

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

    // ---------------------------
    // Submit Profile
    // ---------------------------

    private fun submitProfile() {
        val priceValue = priceInput.text.toString().trim().toDouble()

        val servicePackageList =
            if (fetchedPackages.isNotEmpty()) fetchedPackages
            else listOf(
                ServicePackage(
                    title = titleInput.text.toString().trim(),
                    price =  priceValue,
                    services = descriptionInput.text.toString().trim(),
                    sampleImageURLs = imageUris.map { it.toString() }
                )
            )

        val profileRequest = ProfileRequest(
            userId = userId,
            title = titleInput.text.toString().trim(),
            category = categorySpinner.selectedItem.toString(),
            location = locationInput.text.toString().trim(),
            price = priceValue,
            pricingModel = pricingModelSpinner.selectedItem.toString(),
            description = descriptionInput.text.toString().trim(),
            profilePictureURL = profilePicUri?.toString(),
            workImageURLs = imageUris.map { it.toString() },
            documentURLs = docUris.map { it.toString() },
            verificationStatus = "unverified",
            servicePackages = servicePackageList as List<com.example.grindlyapp1.network.ServicePackage>?,
            packageStatus = "submitted",
            hasProfile = true
        )

        RetrofitClient.getClient(requireContext())
            .createOrUpdateProfile(token, profileRequest)
            .enqueue(object : Callback<ApiResponse> {

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        showToast(response.body()?.message ?: "Profile updated")
                    } else {
                        showToast("Server error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showToast("Network error: ${t.message}")
                }
            })
    }

    // ---------------------------
    // Pickers
    // ---------------------------

    private fun openProfilePicPicker() {
        pickFile("image/*", PICK_PROFILE_PIC, allowMultiple = false)
    }

    private fun openImagePicker() {
        pickFile("image/*", PICK_IMAGES, allowMultiple = true)
    }

    private fun openDocPicker() {
        pickFile("*/*", PICK_DOCS, allowMultiple = true)
    }

    private fun pickFile(type: String, requestCode: Int, allowMultiple: Boolean) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            this.type = type
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        }
        startActivityForResult(intent, requestCode)
    }

    // ---------------------------
    // Activity Result
    // ---------------------------

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK || data == null) return

        when (requestCode) {
            PICK_PROFILE_PIC -> handleSingleImage(data) { uri ->
                profilePicUri = uri
                profileImageView.setImageURI(uri)
            }

            PICK_IMAGES -> handleMultipleOrSingle(data, imageUris).also {
                imageAdapter.notifyDataSetChanged()
            }

            PICK_DOCS -> handleMultipleOrSingle(data, docUris).also {
                docAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun handleSingleImage(data: Intent, onImageSelected: (Uri) -> Unit) {
        data.data?.let { uri ->
            persistPermission(uri)
            onImageSelected(uri)
        }
    }

    private fun handleMultipleOrSingle(data: Intent, targetList: MutableList<Uri>) {
        if (data.clipData != null) {
            for (i in 0 until data.clipData!!.itemCount) {
                val uri = data.clipData!!.getItemAt(i).uri
                persistPermission(uri)
                targetList.add(uri)
            }
        } else {
            data.data?.let { uri ->
                persistPermission(uri)
                targetList.add(uri)
            }
        }
    }

    private fun persistPermission(uri: Uri) {
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }



    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
