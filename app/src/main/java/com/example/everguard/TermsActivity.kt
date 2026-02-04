package com.example.everguard
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.everguard.databinding.ActivityTermsBinding

class TermsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable full-screen edge-to-edge support
        enableEdgeToEdge()

        // Initialize View Binding
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Force Light Mode for readability
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )

        // Handle system bar padding (prevents text from being hidden under the clock/status bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Logic for the button to go back to Register Activity
        binding.backToRegisterBtn.setOnClickListener {
            // Simply finish this activity to return to the previous screen (RegisterActivity)
            finish()
        }
    }
}