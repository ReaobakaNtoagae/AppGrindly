package com.example.grindlyapp1

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.core.content.edit
import com.example.grindlyapp1.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Toast.makeText(requireContext(), "Settings loaded", Toast.LENGTH_SHORT).show()

        val languageSpinner = view.findViewById<Spinner>(R.id.languageSpinner)
        val notificationSwitch = view.findViewById<Switch>(R.id.notificationSwitch)
        val biometricsSwitch = view.findViewById<Switch>(R.id.biometricsSwitch)
        val changePasswordText = view.findViewById<TextView>(R.id.changePassword)
        val deleteAccountButton = view.findViewById<Button>(R.id.btnDelete)
        val logoutButton = view.findViewById<ImageButton>(R.id.btnLogout)

        val placeholderMessage = "This feature will be available in a future update."

        languageSpinner.setOnTouchListener { v, _ ->
            v.performClick()
            Toast.makeText(requireContext(), placeholderMessage, Toast.LENGTH_SHORT).show()
            true
        }

        notificationSwitch.setOnCheckedChangeListener { _, _ ->
            Toast.makeText(requireContext(), placeholderMessage, Toast.LENGTH_SHORT).show()
            notificationSwitch.isChecked = false
        }

        biometricsSwitch.setOnCheckedChangeListener { _, _ ->
            Toast.makeText(requireContext(), placeholderMessage, Toast.LENGTH_SHORT).show()
            biometricsSwitch.isChecked = false
        }

        changePasswordText.setOnClickListener {
            showChangePasswordDialog()
        }

        deleteAccountButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> deleteAccount() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_changepassword, null)
        val oldPassword = dialogView.findViewById<EditText>(R.id.editOldPassword)
        val newPassword = dialogView.findViewById<EditText>(R.id.editPassword)
        val confirmPassword = dialogView.findViewById<EditText>(R.id.editConfirmPassword)
        val submitButton = dialogView.findViewById<Button>(R.id.btnSubmit)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        submitButton.setOnClickListener {
            val oldPass = oldPassword.text.toString().trim()
            val newPass = newPassword.text.toString().trim()
            val confirmPass = confirmPassword.text.toString().trim()

            when {
                oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty() -> {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
                newPass != confirmPass -> {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    updatePassword(oldPass, newPass)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updatePassword(oldPassword: String, newPassword: String) {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", null)
        val token = prefs.getString("TOKEN", null)

        if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing credentials", Toast.LENGTH_SHORT).show()
            return
        }

        val request = PasswordChangeRequest(
            userId = userId,
            oldPassword = oldPassword,
            newPassword = newPassword
        )

        RetrofitClient.userSettingsService.changePassword("Bearer $token", request)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                        logout() // Optional: force logout after password change
                    } else {
                        Toast.makeText(requireContext(), "Update failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteAccount() {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", null)
        val token = prefs.getString("TOKEN", null)

        if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing credentials", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.userSettingsService.deleteAccount("Bearer $token", userId)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                        prefs.edit { clear() }
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "Deletion failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    private fun logout() {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing token", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.userSettingsService.logout("Bearer $token")
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    prefs.edit { clear() }
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

}

