package com.example.everguard

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.everguard.databinding.ActivityRegisterAuthBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


class RegAuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterAuthBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterAuthBinding.inflate(layoutInflater)
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

        binding.proceedBtn.setOnClickListener {
            val user = auth.currentUser
            binding.proceedBtn.isEnabled = false


            // 2. RELOAD WITH NULL SAFETY
            user?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (user.isEmailVerified) {
                        Toast.makeText(this, "Email Verified!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, UserDetailsActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Please click the link in your email first.", Toast.LENGTH_LONG).show()
                        binding.proceedBtn.isEnabled = true
                    }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    binding.proceedBtn.isEnabled = true
                }
            } ?: run {
                // If user is null, send them back to login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        // 3. IMPROVED RESEND LOGIC
        binding.resendBtn.setOnClickListener {
            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email resent!", Toast.LENGTH_SHORT).show()
                } else {
                    // This catches the "Too many requests" error
                    Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}