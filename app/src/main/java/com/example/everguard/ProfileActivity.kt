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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Load user profile data from Firebase
        loadUserProfile()

        // Load recipient data from Firebase
        loadRecipientProfile()

        //adds the back function to registration
        val backArrow = binding.backArrow
        backArrow.setOnClickListener {
            val intent = Intent(this, HomeNotificationsContactsActivity::class.java)

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
                binding.root,
                android.transition.AutoTransition()
            )
            if (expandableLayoutProfile.visibility == android.view.View.GONE) {
                expandableLayoutProfile.visibility = android.view.View.VISIBLE
                arrowIconProfile.setImageResource(R.drawable.arrow_up)
            } else {
                expandableLayoutProfile.visibility = android.view.View.GONE
                arrowIconProfile.setImageResource(R.drawable.arrow_down)
            }
        }

        //cardview toggle for recipient profile
        val expandableLayoutRecipientProfile = binding.expandableViewRecipientProfile
        val arrowIconRecipientProfile = binding.arrowIconRecipientProfile

        binding.cardviewExpandableRecipientProfile.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(
                binding.root,
                android.transition.AutoTransition()
            )
            if (expandableLayoutRecipientProfile.visibility == android.view.View.GONE) {
                expandableLayoutRecipientProfile.visibility = android.view.View.VISIBLE
                arrowIconRecipientProfile.setImageResource(R.drawable.arrow_up)
            } else {
                expandableLayoutRecipientProfile.visibility = android.view.View.GONE
                arrowIconRecipientProfile.setImageResource(R.drawable.arrow_down)
            }
        }

        // Show the edit profile card
        binding.editProfileBtn.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(binding.root)

            if (binding.editProfileCardView.visibility == View.GONE) {
                binding.editProfileCardView.visibility = View.VISIBLE
                binding.editUsernameInput.setText(binding.helloUser.text.toString().replace("Hello, ", "").replace("!", ""))
                binding.editEmailInput.setText(binding.email.text)
            } else {
                binding.editProfileCardView.visibility = View.GONE
            }
        }

        // Back profile button click
        binding.backEditProfileBtn.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(binding.root)
            binding.editProfileCardView.visibility = View.GONE
        }

        // Save profile button click
        binding.updateProfileChangesBtn.setOnClickListener {
            val newUsername = binding.editUsernameInput.text.toString().trim()
            val newEmail = binding.editEmailInput.text.toString().trim()

            if (newUsername.isNotEmpty() && newEmail.isNotEmpty()) {
                updateUserProfile(newUsername, newEmail)
            } else {
                android.widget.Toast.makeText(this, "All fields are required", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        setupDropdowns()

        // Show the edit recipient card
        binding.editRecipientBtn.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(binding.root)

            if (binding.editRecipientCardView.visibility == View.GONE) {
                binding.editRecipientCardView.visibility = View.VISIBLE
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
            android.transition.TransitionManager.beginDelayedTransition(binding.root)
            binding.editRecipientCardView.visibility = View.GONE
        }

        // Save Recipient button click
        binding.updateRecipientChangesBtn.setOnClickListener {
            val newRecipientFname = binding.editRecipientFnameInput.text.toString().trim()
            val newRecipientLname = binding.editRecipientLnameInput.text.toString().trim()
            val newRelationship = binding.editRelationshipInput.text.toString().trim()
            val newMobile = binding.editMobileInput.text.toString().trim()

            if (newRecipientFname.isNotEmpty() && newRecipientLname.isNotEmpty() &&
                newRelationship.isNotEmpty() && newMobile.isNotEmpty()) {
                updateRecipientProfile(newRecipientFname, newRecipientLname, newRelationship, newMobile)
            } else {
                android.widget.Toast.makeText(this, "All fields are required", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance(databaseUrl).getReference("users").child(userId)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    binding.helloUser.text = getString(R.string.hello_user, it.username)
                    binding.email.text = it.email
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.widget.Toast.makeText(
                    this@ProfileActivity,
                    "Failed to load profile: ${error.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadRecipientProfile() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId).child("seniorDetails")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val senior = snapshot.getValue(SeniorDetails::class.java)
                senior?.let {
                    // Display senior's name in the Recipient section
                    binding.recipientFname.text = it.firstName
                    binding.recipientLname.text = it.lastName
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error silently
            }
        })

        // Also load emergency contact details
        val contactDatabase = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId).child("emergencyContact")

        contactDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val contact = snapshot.getValue(EmergencyContact::class.java)
                contact?.let {
                    binding.recipientRelationship.text = it.relationship
                    binding.recipientMobile.text = it.mobile
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun updateUserProfile(username: String, email: String) {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance(databaseUrl).getReference("users").child(userId)

        val updates = hashMapOf<String, Any>(
            "username" to username,
            "email" to email
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                binding.helloUser.text = getString(R.string.hello_user, username)
                binding.email.text = email

                android.transition.TransitionManager.beginDelayedTransition(binding.root)
                binding.editProfileCardView.visibility = View.GONE

                android.widget.Toast.makeText(this, "Profile updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                android.widget.Toast.makeText(this, "Update failed: ${exception.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecipientProfile(firstName: String, lastName: String, relationship: String, mobile: String) {
        val userId = auth.currentUser?.uid ?: return

        // Update senior details
        val seniorDatabase = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId).child("seniorDetails")

        val seniorUpdates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName
        )

        // Update emergency contact
        val contactDatabase = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId).child("emergencyContact")

        val contactUpdates = hashMapOf<String, Any>(
            "relationship" to relationship,
            "mobile" to mobile
        )

        // Update both in parallel
        seniorDatabase.updateChildren(seniorUpdates)
            .addOnSuccessListener {
                contactDatabase.updateChildren(contactUpdates)
                    .addOnSuccessListener {
                        // Update UI
                        binding.recipientFname.text = firstName
                        binding.recipientLname.text = lastName
                        binding.recipientRelationship.text = relationship
                        binding.recipientMobile.text = mobile

                        android.transition.TransitionManager.beginDelayedTransition(binding.root)
                        binding.editRecipientCardView.visibility = View.GONE

                        android.widget.Toast.makeText(this, "Recipient Profile updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        android.widget.Toast.makeText(this, "Failed to update contact: ${exception.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                android.widget.Toast.makeText(this, "Failed to update senior details: ${exception.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDropdowns() {
        val relationships = arrayOf("Parent", "Sibling", "Spouse", "Child", "Friend", "Other")
        setupAdapter(binding.editRelationshipInput, relationships)
    }

    private fun setupAdapter(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
    }
}