package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grindlyapp1.network.*
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.edtEmail)
        val etPassword = findViewById<EditText>(R.id.edtPassword)
        val noAccount = findViewById<TextView>(R.id.noAccount)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        noAccount.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        val request = LoginRequest(email, password)

        RetrofitClient.api.login(request)
            .enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        saveUser(res.userType, res.userId, res.token)

                        // ✅ Get FCM token after login
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val fcmToken = task.result
                                saveFcmToken(fcmToken)
                                sendFcmTokenToServer(res.userId, fcmToken)
                            }

                            if (res.userType.equals("hustler", ignoreCase = true)) {
                                checkProfile(res.userId, res.token)
                            } else {
                                goToMain()
                            }
                        }

                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveUser(userType: String, userId: String, token: String) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("USER_TYPE", userType)
            .putString("USER_ID", userId)
            .putString("TOKEN", token)
            .apply()
    }

    private fun saveFcmToken(fcmToken: String?) {
        if (fcmToken.isNullOrEmpty()) return
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("FCM_TOKEN", fcmToken).apply()
    }

    private fun sendFcmTokenToServer(userId: String, fcmToken: String?) {
        if (fcmToken.isNullOrEmpty()) return

        val data = mapOf(
            "userId" to userId,
            "token" to fcmToken
        )


        RetrofitClient.api.updateFcmToken(data)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        println("FCM token updated successfully")
                    } else {
                        println("Failed to update FCM token: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    println("Error sending FCM token: ${t.message}")
                }
            })
    }

    private fun checkProfile(userId: String, token: String) {
        val bearerToken = "Bearer $token"
        RetrofitClient.getClient(this).getProfile(bearerToken, userId)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    when {
                        response.isSuccessful -> goToMain()
                        response.code() == 404 -> goToCreateProfile()
                        else -> Toast.makeText(this@LoginActivity, "Error checking profile: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goToCreateProfile() {
        startActivity(Intent(this, CreateProfile::class.java))
        finish()
    }
}
