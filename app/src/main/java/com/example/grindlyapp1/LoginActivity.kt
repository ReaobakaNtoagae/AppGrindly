package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.edtEmail)
        val etPassword = findViewById<EditText>(R.id.edtPassword)
        val noAccount = findViewById<TextView>(R.id.noAccount)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                username == "admin" && password == "1234" -> {
                    saveLogin("admin", "Admin User")
                    goToMain()
                }
                username == "client" && password == "1234" -> {
                    saveLogin("client", "Client User")
                    goToMain()
                }
                username == "hustler" && password == "1234" -> {
                    saveLogin("hustler", "Hustler User")
                    goToCreateProfile()
                }
                else -> {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }

        noAccount.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveLogin(userType: String, name: String) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("USER_TYPE", userType)
            .putString("USER_NAME", name)
            .apply()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToCreateProfile() {
        val intent = Intent(this, CreateProfile::class.java)
        startActivity(intent)
        finish()
    }

}