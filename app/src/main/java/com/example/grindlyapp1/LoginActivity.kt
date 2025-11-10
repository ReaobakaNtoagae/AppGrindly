package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.grindlyapp1.network.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    // Biometric and Google Sign-In setup
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val RC_SIGN_IN = 1001
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d("LoginActivity", "onCreate: Initializing views and authentication")

        // UI elements
        val etEmail = findViewById<EditText>(R.id.edtEmail)
        val etPassword = findViewById<EditText>(R.id.edtPassword)
        val tvNoAccount = findViewById<TextView>(R.id.tvNoAccount)
        val tvBiometricLogin = findViewById<TextView>(R.id.tvBiometricLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogleSignIn = findViewById<TextView>(R.id.btnGoogleSignIn)

        // Underline links for clarity
        tvNoAccount.paintFlags = tvNoAccount.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        tvBiometricLogin.paintFlags = tvBiometricLogin.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // Google Sign-In configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("407312171553-506791t7ar1ad1erdorplf20uhqcublp.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogleSignIn.setOnClickListener {
            Log.d("GoogleSignIn", "Launching Google Sign-In intent")
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        // BiometricPrompt setup
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Log.d("BiometricAuth", "Authentication succeeded")
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val email = prefs.getString("USER_EMAIL", null)
                    val password = prefs.getString("USER_PASSWORD", null)

                    if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
                        loginUser(email, password)
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "No saved credentials. Log in manually.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.e("BiometricAuth", "Error $errorCode: $errString")
                    Toast.makeText(
                        this@LoginActivity,
                        "Authentication error: $errString",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationFailed() {
                    Log.w("BiometricAuth", "Authentication failed")
                    Toast.makeText(this@LoginActivity, "Authentication failed", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        // BiometricPrompt configuration
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login to Grindly")
            .setSubtitle("Use fingerprint, face, or device PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        tvBiometricLogin.setOnClickListener {
            Log.d("BiometricAuth", "Checking biometric capability")
            val biometricManager = BiometricManager.from(this)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            when (canAuth) {
                BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    Toast.makeText(
                        this,
                        "No biometrics enrolled. Please set up fingerprint or PIN.",
                        Toast.LENGTH_LONG
                    ).show()

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                    Toast.makeText(
                        this,
                        "Biometric hardware not available. Use your PIN instead.",
                        Toast.LENGTH_LONG
                    ).show()

                else ->
                    Toast.makeText(
                        this,
                        "Secure login unavailable. Use your username and password.",
                        Toast.LENGTH_LONG
                    ).show()
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("ManualLogin", "Attempting login with email: $email")
            loginUser(email, password)
        }

        tvNoAccount.setOnClickListener {
            Log.d("Navigation", "Navigating to SignUpActivity")
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        Log.d("LoginAPI", "Calling login API for $email")
        RetrofitClient.api.login(LoginRequest(email, password))
            .enqueue(object : Callback<AuthResponse> {
                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    val res = response.body()
                    if (response.isSuccessful && res != null) {
                        Log.d("LoginAPI", "Login successful for userId: ${res.userId}")
                        saveUser(res.userType, res.userId, res.token, email, password)
                        if (res.userType.equals("hustler", ignoreCase = true)) {
                            checkProfile(res.userId, res.token)
                        } else {
                            goToMain()
                        }
                    } else {
                        Log.w("LoginAPI", "Login failed: ${response.code()}")
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid credentials",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Log.e("LoginAPI", "Login error: ${t.message}")
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun saveUser(
        userType: String,
        userId: String,
        token: String,
        email: String,
        pass: String
    ) {
        Log.d("UserPrefs", "Saving user session for $email")
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("USER_TYPE", userType)
            putString("USER_ID", userId)
            putString("TOKEN", token)
            putString("USER_EMAIL", email)
            putString("USER_PASSWORD", pass)
            apply()
        }
    }

    private fun checkProfile(userId: String, token: String) {
        Log.d("ProfileCheck", "Checking profile for userId: $userId")
        val bearerToken = "Bearer $token"
        RetrofitClient.getClient(this).getProfile(bearerToken, userId)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(
                    call: Call<ProfileResponse>,
                    response: Response<ProfileResponse>
                ) {
                    when {
                        response.isSuccessful -> {
                            Log.d("ProfileCheck", "Profile exists. Navigating to MainActivity")
                            goToMain()
                        }

                        response.code() == 404 -> {
                            Log.d("ProfileCheck", "Profile not found. Navigating to CreateProfile")
                            goToCreateProfile()
                        }

                        else -> {
                            Log.w("ProfileCheck", "Unexpected response: ${response.code()}")
                            Toast.makeText(
                                this@LoginActivity,
                                "Error checking profile: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    Log.e("ProfileCheck", "Profile check failed: ${t.message}")
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun goToMain() {
        Log.d("Navigation", "Navigating to MainActivity")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goToCreateProfile() {
        Log.d("Navigation", "Navigating to CreateProfile")
        startActivity(Intent(this, CreateProfile::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            Log.d("GoogleSignIn", "Handling Google Sign-In result")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)

                val email = account.email ?: ""
                val name = account.displayName ?: ""
                val idToken = account.idToken ?: ""
                val googleId = account.id ?: ""

                Log.d("GoogleSignIn", "Sign-in successful: $email ($googleId)")

                // Save user session (you can treat this as login or registration)
                saveUser(
                    userType = "google",
                    userId = googleId,
                    token = idToken,
                    email = email,
                    pass = "" // No password for SSO
                )

                // Check if user has a profile; if not, treat as registration
                checkProfile(googleId, idToken)

            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode} - ${e.message}")
                Toast.makeText(this, "Google Sign-In failed. Please try again.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}