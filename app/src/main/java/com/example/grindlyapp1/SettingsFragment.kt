package com.example.grindlyapp1

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import com.example.grindlyapp1.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageSpinner = view.findViewById<Spinner>(R.id.languageSpinner)
        val notificationSwitch = view.findViewById<SwitchCompat>(R.id.notificationSwitch)
        val biometricsSwitch = view.findViewById<SwitchCompat>(R.id.biometricsSwitch)
        val changePasswordText = view.findViewById<TextView>(R.id.changePassowrd)
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
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_changepassword, null)
            val newPassword = dialogView.findViewById<EditText>(R.id.editPassword)
            val confirmPassword = dialogView.findViewById<EditText>(R.id.editConfirmPassword)
            val submitButton = dialogView.findViewById<Button>(R.id.btnLogin)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            submitButton.setOnClickListener {
                val newPass = newPassword.text.toString().trim()
                val confirmPass = confirmPassword.text.toString().trim()

                if (newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (newPass != confirmPass) {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                updatePassword(newPass)
                dialog.dismiss()
            }

            dialog.show()
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

    private fun updatePassword(newPassword: String) {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", "") ?: ""
        val token = prefs.getString("TOKEN", "") ?: ""

        val request = PasswordChangeRequest(userId, newPassword)

        RetrofitClient.userSettingsService.changePassword("Bearer $token", request)
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    Toast.makeText(requireContext(), "Password updated", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteAccount() {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", "") ?: ""
        val token = prefs.getString("TOKEN", "") ?: ""

        RetrofitClient.userSettingsService.deleteAccount("Bearer $token", userId)
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

    private fun logout() {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

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
