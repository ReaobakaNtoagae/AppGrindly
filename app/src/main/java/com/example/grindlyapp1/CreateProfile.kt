package com.example.grindlyapp1

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class CreateProfile : AppCompatActivity() {

    private lateinit var profileImageView: ShapeableImageView
    private var selectedImageUri: Uri? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                profileImageView.load(it) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }

                uploadProfileImage(it)
            }
        }

    private lateinit var edtServiceTitle: EditText
    private lateinit var edtPrice: EditText
    private lateinit var edtDescription: EditText
    private lateinit var edtLocation: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createprofile)

        profileImageView = findViewById(R.id.profileImageView)

        profileImageView.setOnClickListener {
            pickImage.launch("image/*")
        }

        edtPrice = findViewById(R.id.editPrice)

        edtDescription = findViewById(R.id.editDescription)

        edtLocation = findViewById(R.id.editLocation)

        edtServiceTitle= findViewById(R.id.editServiceTitle)

    }

    private fun uploadProfileImage(uri: Uri) {
        try {
            // Convert URI → File
            val file = File(getRealPathFromUri(uri))
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("profileImage", file.name, requestFile)

            // ✅ Now uses apiService directly
            val call = RetrofitClient.apiService.uploadProfileImage(multipartBody)

            call.enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@CreateProfile, "✅ Upload successful!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CreateProfile, "❌ Upload failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@CreateProfile, "⚠️ Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("UploadError", "Upload failed", t)
                }
            })

        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ File error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("UploadError", "File conversion failed", e)
        }
    }

    // 🔹 Helper: Convert URI → Absolute file path
    private fun getRealPathFromUri(uri: Uri): String {
        val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(projection[0])
                return cursor.getString(columnIndex)
            }
        }
        throw IllegalArgumentException("Cannot find file path from URI")
    }
}


