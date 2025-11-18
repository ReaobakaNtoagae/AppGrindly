package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.grindlyapp1.network.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor
import android.util.Log

class LoginActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val RC_SIGN_IN = 1001
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.edtEmail)
        val etPassword = findViewById<EditText>(R.id.edtPassword)
        val tvNoAccount = findViewById<TextView>(R.id.tvNoAccount)
        val tvBiometricLogin = findViewById<TextView>(R.id.tvBiometricLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogleSignIn = findViewById<TextView>(R.id.btnGoogleSignIn)

        tvNoAccount.paintFlags = tvNoAccount.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        tvBiometricLogin.paintFlags = tvBiometricLogin.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // Initialize Google Sign-In
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("894570850018-bva6tbuihgnum4it75ml7f7je0spdbi0.apps.googleusercontent.com")
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Google Sign-In initialization failed", e)
            btnGoogleSignIn.isEnabled = false
        }

        btnGoogleSignIn.setOnClickListener {
            try {
                startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Sign-In not available", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Google Sign-In failed", e)
            }
        }

        // Initialize Biometric Prompt
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val email = prefs.getString("USER_EMAIL", null)
                    val password = prefs.getString("USER_PASSWORD", null)

                    if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
                        Log.d("Biometric", "Attempting login with saved credentials")
                        loginUser(email, password)
                    } else {
                        Log.d("Biometric", "No saved credentials found")
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "No saved credentials found. Please login manually first.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.d("Biometric", "Authentication error: $errString (code: $errorCode)")
                    // Don't show toast for user cancellation
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    Log.d("Biometric", "Authentication failed")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
            })

        // Check biometric availability and create appropriate prompt
        setupBiometricPrompt()

        // Check biometric availability
        checkBiometricAvailability()

        tvBiometricLogin.setOnClickListener {
            attemptBiometricLogin()
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        tvNoAccount.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun setupBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)

        // Check what authentication methods are available
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = biometricManager.canAuthenticate(authenticators)

        Log.d("Biometric", "📱 Device biometric capability: $canAuthenticate")

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login to Grindly")
            .setSubtitle("Use fingerprint, face, or device PIN")
            .setAllowedAuthenticators(authenticators) // Allow both biometric and device credential

        // IMPORTANT: When allowing DEVICE_CREDENTIAL, DO NOT set negative button text
        // Android will automatically handle the fallback to PIN/pattern/password

        promptInfo = promptBuilder.build()
        Log.d("Biometric", "✅ Biometric prompt setup complete - PIN fallback enabled")
    }

    private fun checkBiometricAvailability() {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedEmail = prefs.getString("USER_EMAIL", null)
            val savedPassword = prefs.getString("USER_PASSWORD", null)

            val hasSavedCredentials = !savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()

            val biometricManager = BiometricManager.from(this)
            val biometricStatus = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            val tvBiometricLogin = findViewById<TextView>(R.id.tvBiometricLogin)

            when {
                !hasSavedCredentials -> {
                    tvBiometricLogin.visibility = android.view.View.GONE
                    Log.d("Biometric", "No saved credentials, hiding biometric option")
                }
                biometricStatus != BiometricManager.BIOMETRIC_SUCCESS -> {
                    tvBiometricLogin.visibility = android.view.View.GONE
                    Log.d("Biometric", "Biometric not available, hiding option")
                }
                else -> {
                    tvBiometricLogin.visibility = android.view.View.VISIBLE
                    Log.d("Biometric", "Biometric available, showing option")
                }
            }
        } catch (e: Exception) {
            Log.e("Biometric", "Error checking biometric availability", e)
            findViewById<TextView>(R.id.tvBiometricLogin).visibility = android.view.View.GONE
        }
    }

    private fun attemptBiometricLogin() {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedEmail = prefs.getString("USER_EMAIL", null)
            val savedPassword = prefs.getString("USER_PASSWORD", null)

            if (savedEmail.isNullOrEmpty() || savedPassword.isNullOrEmpty()) {
                Toast.makeText(this, "No saved credentials found. Please login manually first.", Toast.LENGTH_LONG).show()
                return
            }

            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    Log.d("Biometric", "Starting biometric authentication")
                    biometricPrompt.authenticate(promptInfo)
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Toast.makeText(this, "No biometric hardware available", Toast.LENGTH_SHORT).show()
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Toast.makeText(this, "Biometric hardware unavailable", Toast.LENGTH_SHORT).show()
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(this, "No biometric credentials enrolled", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Biometrics unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("Biometric", "Error during biometric login", e)
            Toast.makeText(this, "Biometric login failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser(email: String, password: String) {
        val request = LoginRequest(email, password)

        RetrofitClient.api.login(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!

                    // Save user credentials for future biometric login
                    saveUser(res.role ?: "client", email, res.token, email, password)

                    // Get FCM token
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fcm = task.result
                            saveFcmToken(fcm)
                            sendFcmTokenToServer(email, fcm)
                        }
                        handleRoleNavigation(res.role ?: "client", email, res.token)
                    }

                    // Show success message and update biometric availability
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        checkBiometricAvailability()
                    }

                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("Login", "Login failed: $errorBody")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e("Login", "Network error: ${t.message}", t)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken ?: ""
                val email = account?.email ?: ""

                if (idToken.isEmpty() || email.isEmpty()) {
                    Toast.makeText(this, "Google Sign-In failed: Missing data", Toast.LENGTH_SHORT).show()
                    return
                }

                // Get FCM token before making the Google login request
                FirebaseMessaging.getInstance().token.addOnCompleteListener { fcmTask ->
                    val fcmToken = if (fcmTask.isSuccessful) fcmTask.result else ""

                    RetrofitClient.api.googleLogin(GoogleLoginRequest(idToken, fcmToken))
                        .enqueue(object : Callback<AuthResponse> {
                            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                                if (response.isSuccessful && response.body() != null) {
                                    val res = response.body()!!
                                    saveUser(res.role ?: "google", email, res.token, email, "")

                                    if (res.firstTime) {
                                        showRoleSelectionDialog(email, res.token)
                                    } else {
                                        handleRoleNavigation(res.role ?: "client", email, res.token)
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                    Log.e("GoogleLogin", "Google login failed: $errorBody")
                                    runOnUiThread {
                                        Toast.makeText(this@LoginActivity, "Google login failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                                Log.e("GoogleLogin", "Google login network error: ${t.message}", t)
                                runOnUiThread {
                                    Toast.makeText(this@LoginActivity, "Google login failed: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                }

            } catch (e: ApiException) {
                Log.e("GoogleLogin", "Google Sign-In failed", e)
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("GoogleLogin", "Unexpected error during Google Sign-In", e)
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRoleSelectionDialog(email: String, token: String) {
        val options = arrayOf("Hustler", "Client", "Admin")

        AlertDialog.Builder(this)
            .setTitle("Select your role")
            .setCancelable(false)
            .setItems(options) { _, which ->
                val selected = when (which) {
                    0 -> "hustler"
                    1 -> "client"
                    2 -> "admin"
                    else -> "client"
                }
                saveUser(selected, email, token, email, "")

                if (selected == "hustler") {
                    showPhoneDialog(email, selected, token)
                } else {
                    saveRoleToBackend(email, selected, null, token)
                }
            }
            .show()
    }

    private fun showPhoneDialog(email: String, role: String, token: String) {
        val input = EditText(this)
        input.hint = "Enter phone number (0XXXXXXXXX)"

        AlertDialog.Builder(this)
            .setTitle("Phone Number Required")
            .setMessage("Please enter your phone number.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Submit") { _, _ ->
                val phone = input.text.toString().trim()
                if (phone.matches(Regex("^0\\d{9}$"))) {
                    saveRoleToBackend(email, role, phone, token)
                } else {
                    Toast.makeText(this, "Invalid phone number format", Toast.LENGTH_LONG).show()
                    showPhoneDialog(email, role, token)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                saveRoleToBackend(email, role, null, token)
            }
            .show()
    }

    private fun saveRoleToBackend(email: String, role: String, phone: String?, token: String) {
        val body = mutableMapOf(
            "email" to email,
            "role" to role
        )
        if (phone != null) body["phoneNumber"] = phone

        RetrofitClient.api.setRole(body, "Bearer $token").enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                // Role saved successfully
                if (role == "hustler") {
                    goToCreateProfile()
                } else {
                    goToMain()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Role", "Failed to save role: ${t.message}")
                // Continue anyway even if role save fails
                if (role == "hustler") {
                    goToCreateProfile()
                } else {
                    goToMain()
                }
            }
        })
    }

    private fun handleRoleNavigation(role: String, email: String, token: String) {
        when (role.lowercase()) {
            "hustler" -> checkHustlerProfile(email, token)
            else -> goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goToCreateProfile() {
        startActivity(Intent(this, CreateProfile::class.java))
        finish()
    }

    private fun saveUser(type: String, userId: String, token: String, email: String, pass: String) {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("USER_TYPE", type)
                putString("USER_ID", userId)
                putString("TOKEN", token)
                putString("USER_EMAIL", email)
                // Only save password for regular login (not Google)
                if (pass.isNotEmpty()) {
                    putString("USER_PASSWORD", pass)
                }
                apply()
            }
            Log.d("Biometric", "Saved credentials: email=$email, hasPassword=${pass.isNotEmpty()}")
        } catch (e: Exception) {
            Log.e("SaveUser", "Failed to save user data", e)
        }
    }

    private fun saveFcmToken(fcm: String?) {
        try {
            if (!fcm.isNullOrEmpty()) {
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("FCM_TOKEN", fcm).apply()
            }
        } catch (e: Exception) {
            Log.e("FCM", "Failed to save FCM token", e)
        }
    }

    private fun checkHustlerProfile(userId: String, token: String) {
        RetrofitClient.api.getProfile("Bearer $token", userId)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val profile = response.body()
                        if (profile?.hasProfile == true) {
                            goToMain()
                        } else {
                            goToCreateProfile()
                        }
                    } else {
                        goToCreateProfile()
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    Log.e("Profile", "Failed to check profile: ${t.message}")
                    goToCreateProfile()
                }
            })
    }

    private fun sendFcmTokenToServer(userId: String, token: String?) {
        if (token.isNullOrEmpty()) return

        val request = mapOf(
            "userId" to userId,
            "fcmToken" to token
        )

        RetrofitClient.api.updateFcmToken(request)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    Log.d("FCM", "FCM token updated successfully")
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("FCM", "Failed to update FCM token: ${t.message}")
                }
            })
    }
}