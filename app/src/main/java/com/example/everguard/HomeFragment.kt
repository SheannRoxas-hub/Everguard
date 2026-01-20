package com.example.everguard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.example.everguard.databinding.FragmentHomeBinding
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.widget.Toast

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user data from Firebase
        loadUserData()

        // Load senior details
        loadSeniorDetails()

        // Accident Alert card clicked
        binding.AccidentAlert.setOnClickListener {
            binding.alertDetailsPopup.visibility = View.VISIBLE
            binding.dimOverlay.visibility = View.VISIBLE
        }

        // Close popup
        binding.dimOverlay.setOnClickListener {
            binding.alertDetailsPopup.visibility = View.GONE
            binding.dimOverlay.visibility = View.GONE
        }

        // Handle Call/SMS Logic
        binding.btnCall.setOnClickListener {
            // goodluck po
        }

        binding.btnSms.setOnClickListener {
            // goodluck po
        }

        binding.homeKebabMenu.setOnClickListener { v ->
            showPopupMenu(v)
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    // Update the "Hello, username!" text
                    binding.helloUser.text = "Hello, ${it.username}!"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load user data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadSeniorDetails() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId).child("seniorDetails")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val senior = snapshot.getValue(SeniorDetails::class.java)
                senior?.let {
                    // Update the recipient name
                    binding.recipientName.text = "${it.firstName} ${it.lastName}"

                    // Calculate and display age (optional)
                    val age = calculateAge(it.birthday)
                    binding.recipientAge.text = "Age: $age"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load senior details: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateAge(birthday: String): Int {
        // Parse birthday string (format: "MMMM dd, yyyy" e.g., "January 1, 1958")
        try {
            val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
            val birthDate = dateFormat.parse(birthday)

            val birthCalendar = java.util.Calendar.getInstance()
            birthCalendar.time = birthDate

            val today = java.util.Calendar.getInstance()

            var age = today.get(java.util.Calendar.YEAR) - birthCalendar.get(java.util.Calendar.YEAR)

            // Check if birthday hasn't occurred yet this year
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--
            }

            return age
        } catch (e: Exception) {
            return 0
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.home_kebab, popup.menu)

        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_profile -> {
                    val intent = Intent(requireContext(), ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_logout -> {
                    // Sign out from Firebase
                    auth.signOut()

                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}