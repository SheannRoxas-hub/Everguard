package com.example.everguard

import android.R.id.input
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var getStartedButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
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
    }

    private fun setupListeners() {
        getStartedButton.setOnClickListener {
            handleRegistration()
        }
    }

    private fun handleRegistration() {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        when {
            username.isEmpty() -> {
                usernameInput.error = "Username is required"
                usernameInput.requestFocus()
            }

            email.isEmpty() -> {
                emailInput.error = "Email is required"
                emailInput.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailInput.error = "Invalid email format"
                emailInput.requestFocus()
            }

            password.isEmpty() -> {
                passwordInput.error = "Password is required"
                passwordInput.requestFocus()
            }

            password.length < 6 -> {
                passwordInput.error = "Password must be at least 6 characters"
                passwordInput.requestFocus()
            }

            confirmPassword.isEmpty() -> {
                confirmPasswordInput.error = "Please confirm your password"
                confirmPasswordInput.requestFocus()
            }

            password != confirmPassword -> {
                confirmPasswordInput.error = "Passwords do not match"
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

                val toProfile = Intent(this, ProfileActivity::class.java)

                toProfile.putExtra("username", username)
                toProfile.putExtra("email", email)
                toProfile.putExtra("password", password)

                startActivity(toProfile)
                finish()
            }
        }
    }
    private fun onRegistrationSuccess(username: String, email: String) {
        Toast.makeText(
            this,
            "Welcome, $username!",
            Toast.LENGTH_LONG
        ).show()
    }
}
