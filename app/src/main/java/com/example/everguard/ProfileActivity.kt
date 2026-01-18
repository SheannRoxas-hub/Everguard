package com.example.everguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.example.everguard.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get recipient data from the intent
        val contactFirstName = intent.getStringExtra("CONTACT_FIRST_NAME") ?: ""
        val contactLastName = intent.getStringExtra("CONTACT_LAST_NAME") ?: ""
        val contactRelationship = intent.getStringExtra("CONTACT_RELATIONSHIP") ?: ""
        val contactMobile = intent.getStringExtra("CONTACT_MOBILE") ?: ""

        //Set the recipient data to the TextViews
        binding.recipientFname.text = contactFirstName
        binding.recipientLname.text = contactLastName
        binding.recipientRelationship.text = contactRelationship
        binding.recipientMobile.text = contactMobile

        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        //variable declaration: to extract values from the form of MainActivity.kt
        val username = intent.getStringExtra("username")
        val email = intent.getStringExtra("email")
        val password = intent.getStringExtra("password")

        //value set from the value extracted
        val usernameTextView = binding.helloUser
        usernameTextView.text = getString(R.string.hello_user, username)

        val emailTextView = binding.email
        emailTextView.text = email

        val passwordTextView = binding.password
        passwordTextView.setText(password)

        //adds the back function to registration
        val backArrow = binding.backArrow
        backArrow.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)

            val options = android.app.ActivityOptions.makeCustomAnimation(
                this,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )

            startActivity(intent, options.toBundle())
        }

        //cardview toggle for profile
        val expandableLayoutProfile = binding.expandableViewProfile
        val arrowIconProfile = binding.arrowIconProfile

        binding.cardviewExpandableProfile.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(
                binding.root, // Use binding.root to animate the whole screen
                android.transition.AutoTransition()
            )
            if (expandableLayoutProfile.visibility == android.view.View.GONE) {
                // Expand
                expandableLayoutProfile.visibility = android.view.View.VISIBLE
                arrowIconProfile.setImageResource(R.drawable.arrow_up) // Change icon to up
            } else {
                // Collapse
                expandableLayoutProfile.visibility = android.view.View.GONE
                arrowIconProfile.setImageResource(R.drawable.arrow_down) // Change icon to down
            }
        }

        //cardview toggle for recipient profile
        val expandableLayoutRecipientProfile = binding.expandableViewRecipientProfile
        val arrowIconRecipientProfile = binding.arrowIconRecipientProfile

        binding.cardviewExpandableRecipientProfile.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(
                binding.root, // Use binding.root to animate the whole screen
                android.transition.AutoTransition()
            )
            if (expandableLayoutRecipientProfile.visibility == android.view.View.GONE) {
                // Expand
                expandableLayoutRecipientProfile.visibility = android.view.View.VISIBLE
                arrowIconRecipientProfile.setImageResource(R.drawable.arrow_up) // Change icon to up
            } else {
                // Collapse
                expandableLayoutRecipientProfile.visibility = android.view.View.GONE
                arrowIconRecipientProfile.setImageResource(R.drawable.arrow_down) // Change icon to down }
            }
        }

        // Show the edit profile card
        binding.editProfileBtn.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(binding.root)

            if (binding.editProfileCardView.visibility == View.GONE) {
                binding.editProfileCardView.visibility = View.VISIBLE
                // Pre-fill the input with current data
                binding.editUsernameInput.setText(binding.helloUser.text.toString().replace("Hello, ", ""))
                binding.editEmailInput.setText(binding.email.text)
                binding.editPasswordInput.setText(binding.password.text)
            } else {
                binding.editProfileCardView.visibility = View.GONE
            }
        }

        // Back profile button click
        binding.backEditProfileBtn.setOnClickListener {
            // Add smooth animation
            android.transition.TransitionManager.beginDelayedTransition(binding.root)

            // Hide the edit card
            binding.editProfileCardView.visibility = View.GONE
        }

        // Save profile button click
        binding.updateProfileChangesBtn.setOnClickListener {
            val newUsername = binding.editUsernameInput.text.toString().trim()
            val newEmail = binding.editEmailInput.text.toString().trim()
            val newPassword = binding.editPasswordInput.text.toString().trim()

            // Validation: Check if any fields are empty
            if (newUsername.isNotEmpty() && newEmail.isNotEmpty() && newPassword.isNotEmpty()) {

                // Update the UI text immediately
                binding.helloUser.text = getString(R.string.hello_user, newUsername)
                binding.email.text = newEmail
                binding.password.setText(newPassword)

                // Hide the edit card smoothly
                android.transition.TransitionManager.beginDelayedTransition(binding.root)
                binding.editProfileCardView.visibility = View.GONE

                // how success message
                android.widget.Toast.makeText(this, "Profile updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                // Show error if fields are empty
                android.widget.Toast.makeText(this, "All fields are required", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        setupDropdowns()

        // Show the edit recipient card
        binding.editRecipientBtn.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(binding.root)

            if (binding.editRecipientCardView.visibility == View.GONE) {
                binding.editRecipientCardView.visibility = View.VISIBLE
                // Pre-fill the input with current data
                binding.editRecipientFnameInput.setText(binding.recipientFname.text)
                binding.editRecipientLnameInput.setText(binding.recipientLname.text)
                binding.editRelationshipInput.setText(binding.recipientRelationship.text)
                binding.editMobileInput.setText(binding.recipientMobile.text)
            } else {
                binding.editRecipientCardView.visibility = View.GONE
            }
        }

        // Back recipient button click
        binding.backEditRecipientBtn.setOnClickListener {
            // Add smooth animation
            android.transition.TransitionManager.beginDelayedTransition(binding.root)

            // Hide the edit card
            binding.editRecipientCardView.visibility = View.GONE
        }

        // Save Recipient button click
        binding.updateRecipientChangesBtn.setOnClickListener {
            val newRecipientFname = binding.editRecipientFnameInput.text.toString().trim()
            val newRecipientLname = binding.editRecipientLnameInput.text.toString().trim()
            val newRelationship = binding.editRelationshipInput.text.toString().trim()
            val newMobile = binding.editMobileInput.text.toString().trim()

            // Validation: Check if any fields are empty
            if (newRecipientFname.isNotEmpty() && newRecipientLname.isNotEmpty() && newRelationship.isNotEmpty() && newMobile.isNotEmpty()) {

                // Update the UI text immediately
                binding.recipientFname.text = newRecipientFname
                binding.recipientLname.text = newRecipientLname
                binding.recipientRelationship.text = newRelationship
                binding.recipientMobile.text = newMobile

                // Hide the edit card smoothly
                android.transition.TransitionManager.beginDelayedTransition(binding.root)
                binding.editRecipientCardView.visibility = View.GONE

                // Show success message
                android.widget.Toast.makeText(this, "Recipient Profile updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                // Show error if fields are empty
                android.widget.Toast.makeText(this, "All fields are required", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDropdowns() {
        val relationships = arrayOf("Parent", "Sibling", "Spouse", "Child", "Friend", "Other")

        // Use the helper to set up the AutoCompleteTextView
        setupAdapter(binding.editRelationshipInput, relationships)
    }

    private fun setupAdapter(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)

        // Ensures the dropdown opens immediately when the user taps the field
        view.setOnClickListener { view.showDropDown() }
    }
}