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
import android.widget.Toast
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
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }

        setupDropdowns()

        // Show the edit recipient card
        binding.backEditRecipientBtn.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(binding.root)

            if (binding.editRecipientCardView.visibility == View.GONE) {
                binding.editRecipientCardView.visibility = View.VISIBLE
                binding.editRecipientFnameInput.setText(binding.recipientFname.text)
                binding.editRecipientLnameInput.setText(binding.recipientLname.text)
                binding.editRecipientBdateInput.setText(binding.recipientBdate.text)
                binding.editRecipientGenderInput.setText(binding.recipientGender.text)
                binding.editRecipientContactInput.setText(binding.recipientContact.text)
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
            val newRecipientBdate = binding.editRecipientBdateInput.text.toString().trim()
            val newRecipientGender = binding.editRecipientGenderInput.text.toString().trim()
            val newRecipientContact = binding.editRecipientContactInput.text.toString().trim()

            if (newRecipientFname.isNotEmpty() && newRecipientLname.isNotEmpty() &&
                newRecipientBdate.isNotEmpty() && newRecipientGender.isNotEmpty() &&
                newRecipientContact.isNotEmpty()) {
                updateRecipientProfile(newRecipientFname, newRecipientLname, newRecipientBdate, newRecipientGender, newRecipientContact)
            } else {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
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
                    binding.helloUser.text = "Hello, ${it.username}!"
                    binding.email.text = it.email

                    // Display password as asterisks (masked)
                    // Since we don't store password in database, we'll show a placeholder
                    binding.password.setText("********")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Failed to load profile: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadRecipientProfile() {
        val userId = auth.currentUser?.uid ?: return

        // Load care person (recipient) details
        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get care person data
                val carePerson = snapshot.child("carePerson").getValue(CarePerson::class.java)

                carePerson?.let {
                    binding.recipientFname.text = it.fname
                    binding.recipientLname.text = it.lname
                    binding.recipientBdate.text = it.bdate
                    binding.recipientGender.text = it.gender.replaceFirstChar { char -> char.uppercase() }
                    binding.recipientContact.text = it.contact
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Failed to load recipient: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                binding.helloUser.text = "Hello, $username!"
                binding.email.text = email

                android.transition.TransitionManager.beginDelayedTransition(binding.root)
                binding.editProfileCardView.visibility = View.GONE

                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Update failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecipientProfile(
        firstName: String,
        lastName: String,
        bdate: String,
        gender: String,
        contact: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        // Update care person details
        val carePersonRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId).child("carePerson")

        val carePersonUpdates = hashMapOf<String, Any>(
            "fname" to firstName,
            "lname" to lastName,
            "bdate" to bdate,
            "gender" to gender.lowercase(),
            "contact" to contact
        )

        carePersonRef.updateChildren(carePersonUpdates)
            .addOnSuccessListener {
                // Update UI
                binding.recipientFname.text = firstName
                binding.recipientLname.text = lastName
                binding.recipientBdate.text = bdate
                binding.recipientGender.text = gender.replaceFirstChar { it.uppercase() }
                binding.recipientContact.text = contact

                android.transition.TransitionManager.beginDelayedTransition(binding.root)
                binding.editRecipientCardView.visibility = View.GONE

                Toast.makeText(this, "Recipient Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to update: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDropdowns() {
        val genders = arrayOf("Male", "Female", "Other")
        setupAdapter(binding.editRecipientGenderInput, genders)
    }

    private fun setupAdapter(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
    }
}