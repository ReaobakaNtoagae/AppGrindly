package com.example.grindlyapp1

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.imageview.ShapeableImageView

class CreateProfile : AppCompatActivity() {

    private lateinit var profileImageView: ShapeableImageView

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {

                profileImageView.load(it) {
                    crossfade(true)
                    transformations(coil.transform.CircleCropTransformation())
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createprofile)

        profileImageView = findViewById(R.id.profileImageView)


        profileImageView.setOnClickListener {
            pickImage.launch("image/*")
        }
    }


}