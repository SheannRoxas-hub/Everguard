package com.example.everguard

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import androidx.core.widget.doOnTextChanged
import com.example.everguard.databinding.ActivityMainBinding
import kotlin.jvm.java


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.registrationBtn.setOnClickListener {
            handleRegistration()
        }
        binding.usernameInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank()) {
                clearError(binding.usernameLayout)
            }
        }

        binding.emailInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(text.toString()).matches()
            ) {
                clearError(binding.emailLayout)
            }
        }

        binding.passwordInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank() && text.length >= 6) {
                clearError(binding.passwordLayout)
            }
        }

        binding.confirmPasswordInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank() &&
                text.toString() == binding.passwordInput.text.toString()
            ) {
                clearError(binding.confirmPasswordLayout)
            }
        }
    }

    private fun handleRegistration() {
        val username = binding.usernameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()

        when {
            username.isEmpty() -> {
                showError(binding.usernameLayout, "Username is required")
                binding.usernameInput.requestFocus()
            }

            email.isEmpty() -> {
                showError(binding.emailLayout, "Email is required")
                binding.emailInput.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError(binding.emailLayout, "Invalid email required")
                binding.emailInput.requestFocus()
            }

            password.isEmpty() -> {
                showError(binding.passwordLayout, "Password is required")
                binding.passwordInput.requestFocus()
            }

            password.length < 6 -> {
                showError(binding.passwordLayout, "Password must be at least 6 characters")
                binding.passwordInput.requestFocus()
            }

            confirmPassword.isEmpty() -> {
                showError(binding.confirmPasswordLayout, "Please confirm your password")
                binding.confirmPasswordInput.requestFocus()
            }

            password != confirmPassword -> {
                binding.confirmPasswordLayout.error = "Passwords do not match"
                binding.confirmPasswordInput.requestFocus()
            }

            !binding.termsCheckbox.isChecked -> {
                Toast.makeText(
                    this,
                    "You must agree to the Terms & Conditions",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                onRegistrationSuccess(username)

                val toProfile = Intent(this, ProfileActivity::class.java)

                toProfile.putExtra("username", username)
                toProfile.putExtra("email", email)
                toProfile.putExtra("password", password)

                val options = android.app.ActivityOptions.makeCustomAnimation(
                    this,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )

                startActivity(toProfile, options.toBundle())
                finish()
            }
        }
    }

    private fun clearError(layout: TextInputLayout) {
        layout.error = null
        layout.isErrorEnabled = false
    }

    private fun showError (layout: TextInputLayout, message: String) {
        layout.error = message
        layout.isErrorEnabled = true
    }

    private fun onRegistrationSuccess(username: String) {
        Toast.makeText(
            this,
            "Welcome, $username!",
            Toast.LENGTH_LONG
        ).show()
    }
}
