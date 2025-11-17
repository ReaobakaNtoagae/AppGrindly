package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.grindlyapp1.network.RetrofitClient
import com.example.grindlyapp1.network.SettingsUiState
import com.example.grindlyapp1.viewmodel.SettingsViewModel
import com.example.grindlyapp1.viewmodelfactory.SettingsVMFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel

    private lateinit var languageSpinner: Spinner
    private lateinit var notificationSwitch: Switch
    private lateinit var biometricsSwitch: Switch
    private lateinit var changePasswordText: TextView
    private lateinit var deleteAccountButton: Button
    private lateinit var logoutButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // --- Initialize UI ---
        languageSpinner = view.findViewById(R.id.languageSpinner)
        notificationSwitch = view.findViewById(R.id.notificationSwitch)
        biometricsSwitch = view.findViewById(R.id.biometricsSwitch)
        changePasswordText = view.findViewById(R.id.changePassword)
        deleteAccountButton = view.findViewById(R.id.btnDelete)
        logoutButton = view.findViewById(R.id.btnLogout)

        // --- Setup ViewModel ---
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val factory = SettingsVMFactory(prefs, RetrofitClient.api)
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        observeViewModel()
        setupUI()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state: SettingsUiState ->
                // Update UI according to state
                languageSpinner.setSelection(getLanguagePosition(state.language))
                notificationSwitch.isChecked = state.notificationsEnabled
                biometricsSwitch.isChecked = state.biometricsEnabled

                // Show messages
                state.message?.let { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupUI() {
        // --- Language selection ---
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val language = parent.getItemAtPosition(position).toString()
                viewModel.updateLanguage(language)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // --- Notification toggle ---
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
        }

        // --- Biometrics toggle ---
        biometricsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleBiometrics(isChecked)
        }

        // --- Change password ---
        changePasswordText.setOnClickListener { showChangePasswordDialog() }

        // --- Delete account ---
        deleteAccountButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteAccount() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- Logout ---
        logoutButton.setOnClickListener {
            viewModel.logout {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_changepassword, null)
        val oldPassword = dialogView.findViewById<EditText>(R.id.editOldPassword)
        val newPassword = dialogView.findViewById<EditText>(R.id.editPassword)
        val confirmPassword = dialogView.findViewById<EditText>(R.id.editConfirmPassword)
        val submitButton = dialogView.findViewById<Button>(R.id.btnSubmit)

        val dialog = MaterialAlertDialogBuilder(requireContext())
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
                    viewModel.changePassword(oldPass, newPass)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun getLanguagePosition(language: String?): Int {
        val adapter = languageSpinner.adapter ?: return 0
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == language) return i
        }
        return 0
    }
}
