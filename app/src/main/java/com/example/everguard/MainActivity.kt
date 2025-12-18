package com.example.everguard

import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import androidx.core.widget.doOnTextChanged

class MainActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var getStartedButton: Button
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        usernameInput = findViewById(R.id.username_input)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        termsCheckbox = findViewById(R.id.terms_and_conditions_checkbox)
        getStartedButton = findViewById(R.id.registration_btn)
        usernameLayout = findViewById(R.id.username_layout)
        emailLayout = findViewById(R.id.email_layout)
        passwordLayout = findViewById(R.id.password_layout)
        confirmPasswordLayout = findViewById (R.id.confirm_password_layout)
    }

    private fun setupListeners() {
        getStartedButton.setOnClickListener {
            handleRegistration()
        }
        usernameInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank()) {
                clearError(usernameLayout)
            }
        }

        emailInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(text.toString()).matches()
            ) {
                clearError(emailLayout)
            }
        }

        passwordInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank() && text.length >= 6) {
                clearError(passwordLayout)
            }
        }

        confirmPasswordInput.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank() &&
                text.toString() == passwordInput.text.toString()
            ) {
                clearError(confirmPasswordLayout)
            }
        }
    }

    private fun handleRegistration() {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        when {
            username.isEmpty() -> {
                showError(usernameLayout, "Username is required")
                usernameInput.requestFocus()
            }

            email.isEmpty() -> {
                showError(emailLayout, "Email is required")
                emailInput.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError(emailLayout, "Invalid email required")
                emailInput.requestFocus()
            }

            password.isEmpty() -> {
                showError(passwordLayout, "Password is required")
                passwordInput.requestFocus()
            }

            password.length < 6 -> {
                showError(passwordLayout, "Password must be at least 6 characters")
                passwordInput.requestFocus()
            }

            confirmPassword.isEmpty() -> {
                showError(confirmPasswordLayout, "Please confirm your password")
                confirmPasswordInput.requestFocus()
            }

            password != confirmPassword -> {
                confirmPasswordLayout.error = "Passwords do not match"
                confirmPasswordInput.requestFocus()
            }

            !termsCheckbox.isChecked -> {
                Toast.makeText(
                    this,
                    "You must agree to the Terms & Conditions",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                onRegistrationSuccess(username, email)
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

    private fun onRegistrationSuccess(username: String, email: String) {
        Toast.makeText(
            this,
            "Welcome, $username!",
            Toast.LENGTH_LONG
        ).show()

    }
}
