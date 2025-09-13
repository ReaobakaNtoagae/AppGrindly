package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName = findViewById<EditText>(R.id.editUsername)
        val etEmail = findViewById<EditText>(R.id.editEmail)
        val spinnerRole = findViewById<Spinner>(R.id.roleSpinner)
        val etPassword = findViewById<EditText>(R.id.editPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.editConfirmPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val role = spinnerRole.selectedItem.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // ✅ Basic validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("USER_NAME", name)
                .putString("USER_EMAIL", email)
                .putString("USER_TYPE", role.lowercase())
                .putString("USER_PASSWORD", password)
                .apply()

            Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()


            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
