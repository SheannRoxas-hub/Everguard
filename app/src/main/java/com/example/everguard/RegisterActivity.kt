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
import com.example.everguard.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        auth = Firebase.auth

        setupListeners()

        binding.loginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
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
        val passwordRegex = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$")

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

            !password.matches(passwordRegex)-> {
                showError(binding.passwordLayout, "Password must have atleast one uppercase, lowercase, digit, special character")
                binding.passwordInput.requestFocus()
            }

            confirmPassword.isEmpty() -> {
                showError(binding.confirmPasswordLayout, "Please confirm your password")
                binding.confirmPasswordInput.requestFocus()
            }

            password != confirmPassword -> {
                showError(binding.confirmPasswordLayout, "Passwords do not match")
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
                binding.registrationBtn.isEnabled = false // Prevent double clicks

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser

                            user?.let {
                                // Create user object (DO NOT store password in database!)
                                val userProfile = User(
                                    username = username,
                                    email = email,
                                    userId = it.uid
                                )

                                // Save to Realtime Database
                                val database = FirebaseDatabase.getInstance("https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users")
                                database.child(it.uid).setValue(userProfile)
                                    .addOnSuccessListener { _ ->  // Changed 'it' to '_'
                                        // Send verification email after successful database save
                                        user.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                                            if (verifyTask.isSuccessful) {
                                                Toast.makeText(this, "Registration successful! Please verify your email.", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, RegAuthActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        binding.registrationBtn.isEnabled = true
                                        Toast.makeText(this, "Database Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            binding.registrationBtn.isEnabled = true
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

            }
        }
    }

    private fun clearError(layout: TextInputLayout) {
        layout.error = null
        layout.isErrorEnabled = false
    }

    private fun showError (layout: TextInputLayout, message: String) {
        layout.isErrorEnabled = true
        layout.error = message
    }
}
