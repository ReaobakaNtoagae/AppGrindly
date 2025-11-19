package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.grindlyapp1.network.AuthResponse
import com.example.grindlyapp1.network.RegisterRequest
import com.example.grindlyapp1.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {

    private val TAG = "SignUpActivity"

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", null) ?: "default"
        val contextWithLocale = LanguageManager.applyLanguage(newBase, userId)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName = findViewById<EditText>(R.id.editUsername)
        val etEmail = findViewById<EditText>(R.id.editEmail)
        val etPhone = findViewById<EditText>(R.id.editPhoneNumber)
        val spinnerRole = findViewById<Spinner>(R.id.roleSpinner)
        val etPassword = findViewById<EditText>(R.id.editPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.editConfirmPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val phone = etPhone.text.toString().trim()
            val role = spinnerRole.selectedItem.toString().lowercase()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (!validateInputs(name, email, phone, role, password, confirmPassword)) return@setOnClickListener

            val request = RegisterRequest(
                email = email,
                password = password,
                fullName = name,
                phoneNumber = phone,
                role = role
            )

            Log.d(TAG, "Sending registration request: $request")

            RetrofitClient.api.register(request).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    Log.d(TAG, "Response received: ${response.code()} ${response.message()}")

                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        Log.d(TAG, "SignUp successful: $res")
                        saveUser(res.role, res.userId, res.token, name)
                        showToast("Sign Up Successful!")

                        etName.postDelayed({
                            startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                            finish()
                        }, 1500)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "SignUp failed: ${response.code()} $errorBody")
                        showToast("Registration failed: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Log.e(TAG, "SignUp error: ${t.message}", t)
                    showToast("Error: ${t.message}")
                }
            })
        }
    }

    private fun validateInputs(
        name: String, email: String, phone: String,
        role: String, password: String, confirmPassword: String
    ): Boolean {
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("All fields are required")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Invalid email format")
            return false
        }

        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*]).{8,}$")
        if (!passwordRegex.matches(password)) {
            showToast("Password must be 8+ chars, include upper/lowercase, number, and symbol")
            return false
        }

        if (password != confirmPassword) {
            showToast("Passwords do not match")
            return false
        }

        val phoneRegex = Regex("^0\\d{9}$")
        if (!phoneRegex.matches(phone)) {
            showToast("Phone number must start with 0 and be exactly 10 digits")
            return false
        }

        if (role !in listOf("admin", "hustler", "client")) {
            showToast("Please select a valid role")
            return false
        }

        return true
    }

    private fun saveUser(userType: String, userId: String, token: String, fullName: String) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("USER_TYPE", userType)
            .putString("USER_ID", userId)
            .putString("TOKEN", token)
            .putString("USER_NAME", fullName)
            .apply()

        Log.d(TAG, "User saved: $userType, $userId, $fullName")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}