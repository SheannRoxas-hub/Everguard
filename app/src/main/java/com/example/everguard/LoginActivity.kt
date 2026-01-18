package com.example.everguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.everguard.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        val user = auth.currentUser

        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val refreshedUser = auth.currentUser
                if (refreshedUser?.isEmailVerified == true) {
                    startActivity(Intent(this, HomeNotificationsContactsActivity::class.java))
                } else {
                    startActivity(Intent(this, RegAuthActivity::class.java))
                }
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
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

        binding.signinBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user?.isEmailVerified == true) {
                        startActivity(Intent(this, HomeNotificationsContactsActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, RegAuthActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}