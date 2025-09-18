package com.example.grindlyapp1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grindlyapp1.R
import com.example.grindlyapp1.network.ApiResponse
import com.example.grindlyapp1.network.ServicePackageUpdateRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ServicePackage : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var servicesInput: EditText
    private lateinit var priceInput: EditText
    private lateinit var previewRecycler: RecyclerView
    private lateinit var submitButton: Button
    private lateinit var skipTextView: TextView
    private lateinit var btnUploadImg: Button

    private lateinit var imageAdapter: ImageAdapter
    private val imageUris = mutableListOf<Uri>()

    private var userId: String? = null

    companion object {
        private const val PICK_IMAGES = 300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_service_package)

        // Get userId from intent
        userId = intent.getStringExtra("userId")

        // Bind views
        titleInput = findViewById(R.id.editUsername)
        servicesInput = findViewById(R.id.serviceList)
        priceInput = findViewById(R.id.editPassword)
        submitButton = findViewById(R.id.btnLogin)
        skipTextView = findViewById(R.id.okayTextView)
        btnUploadImg = findViewById(R.id.btnUploadImg)
        previewRecycler = findViewById(R.id.imageRecycler)



        imageAdapter = ImageAdapter(imageUris)
        previewRecycler.adapter = imageAdapter
        previewRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Upload button
        btnUploadImg.setOnClickListener { openImagePicker() }

        // Submit & skip
        submitButton.setOnClickListener { submitServicePackage() }
        skipTextView.setOnClickListener { submitNoneAsServicePackage() }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGES) {
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
        }
    }

    private fun submitServicePackage() {
        val title = titleInput.text.toString().trim()
        val services = servicesInput.text.toString().trim()
        val price = priceInput.text.toString().trim()

        if (title.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Title and price are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Build request
        val servicePackage = com.example.grindlyapp1.network.ServicePackage(
            title = title,
            price = price,
            services = services,
            sampleImageURLs = imageUris.map { it.toString() }
        )

        val request = ServicePackageUpdateRequest(
            userId = userId ?: "unknown_user",
            servicePackages = listOf(servicePackage),
            packageStatus = "submitted"
        )

        Log.d("ServicePackage", "Request: $request")
        sendServicePackageRequest(request)
    }

    private fun submitNoneAsServicePackage() {
        val request = ServicePackageUpdateRequest(
            userId = userId ?: "unknown_user",
            servicePackages = null,
            packageStatus = "skipped"
        )

        sendServicePackageRequest(request)
    }

    private fun sendServicePackageRequest(request: ServicePackageUpdateRequest) {
        val api = RetrofitClient.instance.create(ProfileApiService::class.java)
        api.updateServicePackages(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ServicePackage,
                        response.body()?.message ?: "Service package updated",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@ServicePackage, MainActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(
                        this@ServicePackage,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ServicePackage", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(
                    this@ServicePackage,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ServicePackage", "Failure: ${t.message}")
            }
        })
    }
}
