package com.example.grindlyapp1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AddGuide : AppCompatActivity() {

    private lateinit var submitBtn : Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addguide)

        submitBtn = findViewById(R.id.btnSubmit)

        submitBtn.setOnClickListener{
            val intent = Intent(this, ManageMicroAcademy::class.java)
            startActivity(intent)
        }
        }
}