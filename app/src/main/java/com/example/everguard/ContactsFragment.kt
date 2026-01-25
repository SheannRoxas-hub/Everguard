package com.example.everguard

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import com.example.everguard.databinding.FragmentContactsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var deviceId: String = ""
    private var allContacts = mutableMapOf<String, EmergencyContact>()
    private var selectedContactKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load emergency contacts from Firebase
        loadEmergencyContacts()

        setupDropdowns()

        binding.homeKebabMenu.setOnClickListener { v ->
            showPopupMenu(v)
        }

        // Contact Cards logic
        binding.Contact1.setOnClickListener {
            openUpdateContact("emgperson1")
        }

        binding.Contact2.setOnClickListener {
            openUpdateContact("emgperson2")
        }

        binding.Contact3.setOnClickListener {
            openUpdateContact("emgperson3")
        }

        // Add Contact Button logic
        binding.btnAddContact.setOnClickListener {
            val emptySlotCount = countEmptySlots()
            if (emptySlotCount == 0) {
                Toast.makeText(requireContext(), "Maximum 3 contacts allowed", Toast.LENGTH_SHORT).show()
            } else {
                openAddContact()
            }
        }

        // Close Buttons and Overlay Dismiss
        val closeAction = View.OnClickListener {
            binding.updateContactCard.visibility = View.GONE
            binding.addContactCard.visibility = View.GONE
            binding.dimOverlay.visibility = View.GONE
            clearAddContactInputs()
        }

        binding.btnClosePopupUpdate.setOnClickListener(closeAction)
        binding.btnClosePopupAdd.setOnClickListener(closeAction)
        binding.dimOverlay.setOnClickListener(closeAction)

        // Update Contact Button
        binding.updateRecipientChangesBtn.setOnClickListener {
            updateContact()
        }

        // Add Contact Button
        binding.addRecipientBtn.setOnClickListener {
            addNewContact()
        }

        // Delete Contact Button - CHANGED to clear instead of delete
        binding.btnDeleteContact.setOnClickListener {
            clearContact()
        }
    }

    private fun loadEmergencyContacts() {
        val userId = auth.currentUser?.uid ?: return

        // First get the user's deviceId
        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deviceId = snapshot.child("deviceId").getValue(String::class.java) ?: ""

                if (deviceId.isNotEmpty() && deviceId != "") {
                    loadContactsFromDevice(deviceId)
                } else {
                    // No device paired yet
                    updateUI()
                    Toast.makeText(requireContext(), "No device paired", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadContactsFromDevice(deviceId: String) {
        val deviceRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId).child("emergencyContacts")

        deviceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return // Add this check

                allContacts.clear()

                for (i in 1..3) {
                    val key = "emgperson$i"
                    val contactSnapshot = snapshot.child(key)

                    val fname = contactSnapshot.child("fname").getValue(String::class.java) ?: ""
                    val lname = contactSnapshot.child("lname").getValue(String::class.java) ?: ""
                    val relationship = contactSnapshot.child("relationship").getValue(String::class.java) ?: ""
                    val contact = contactSnapshot.child("contact").getValue(String::class.java) ?: ""

                    allContacts[key] = EmergencyContact(fname, lname, relationship, contact)
                }

                updateUI()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return // Add this check

                Toast.makeText(requireContext(), "Failed to load contacts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI() {
        if (!isAdded || _binding == null) return // Add this check

        val filledContactsCount = allContacts.values.count {
            it.fname.isNotEmpty() || it.lname.isNotEmpty()
        }

        binding.contactCount.text = "$filledContactsCount/3"

        // Update Contact 1
        val contact1 = allContacts["emgperson1"]
        if (contact1 != null && (contact1.fname.isNotEmpty() || contact1.lname.isNotEmpty())) {
            binding.contact1Name.text = "${contact1.fname} ${contact1.lname}"
            binding.contact1Relationship.text = contact1.relationship
            binding.contact1Phone.text = contact1.contact
            binding.Contact1.visibility = View.VISIBLE
        } else {
            binding.Contact1.visibility = View.GONE
        }

        // Update Contact 2
        val contact2 = allContacts["emgperson2"]
        if (contact2 != null && (contact2.fname.isNotEmpty() || contact2.lname.isNotEmpty())) {
            binding.contact2Name.text = "${contact2.fname} ${contact2.lname}"
            binding.contact2Relationship.text = contact2.relationship
            binding.contact2Phone.text = contact2.contact
            binding.Contact2.visibility = View.VISIBLE
        } else {
            binding.Contact2.visibility = View.GONE
        }

        // Update Contact 3
        val contact3 = allContacts["emgperson3"]
        if (contact3 != null && (contact3.fname.isNotEmpty() || contact3.lname.isNotEmpty())) {
            binding.contact3Name.text = "${contact3.fname} ${contact3.lname}"
            binding.contact3Relationship.text = contact3.relationship
            binding.contact3Phone.text = contact3.contact
            binding.Contact3.visibility = View.VISIBLE
        } else {
            binding.Contact3.visibility = View.GONE
        }

        val emptySlots = countEmptySlots()
        binding.btnAddContact.visibility = if (emptySlots > 0) View.VISIBLE else View.GONE
    }

    private fun countEmptySlots(): Int {
        return allContacts.values.count {
            it.fname.isEmpty() && it.lname.isEmpty()
        }
    }

    private fun getFirstEmptySlot(): String? {
        for (i in 1..3) {
            val key = "emgperson$i"
            val contact = allContacts[key]
            if (contact != null && contact.fname.isEmpty() && contact.lname.isEmpty()) {
                return key
            }
        }
        return null
    }

    private fun openUpdateContact(contactKey: String) {
        selectedContactKey = contactKey
        val contact = allContacts[contactKey]

        if (contact != null) {
            // Pre-fill the update form (even if empty)
            binding.updateRecipientFnameInput.setText(contact.fname)
            binding.updateRecipientLnameInput.setText(contact.lname)
            binding.updateRelationshipInput.setText(contact.relationship, false)
            binding.updateMobileInput.setText(contact.contact)

            binding.updateContactCard.visibility = View.VISIBLE
            binding.dimOverlay.visibility = View.VISIBLE
        }
    }

    private fun openAddContact() {
        clearAddContactInputs()
        binding.addContactCard.visibility = View.VISIBLE
        binding.dimOverlay.visibility = View.VISIBLE
    }

    private fun updateContact() {
        val fname = binding.updateRecipientFnameInput.text.toString().trim()
        val lname = binding.updateRecipientLnameInput.text.toString().trim()
        val relationship = binding.updateRelationshipInput.text.toString().trim()
        val mobile = binding.updateMobileInput.text.toString().trim()

        if (fname.isEmpty() || lname.isEmpty() || relationship.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (deviceId.isEmpty() || selectedContactKey == null) {
            Toast.makeText(requireContext(), "Error updating contact", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedContact = mapOf(
            "fname" to fname,
            "lname" to lname,
            "relationship" to relationship,
            "contact" to mobile
        )

        val contactRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId)
            .child("emergencyContacts").child(selectedContactKey!!)

        contactRef.setValue(updatedContact)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Contact updated successfully!", Toast.LENGTH_SHORT).show()
                binding.updateContactCard.visibility = View.GONE
                binding.dimOverlay.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to update: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addNewContact() {
        val fname = binding.addRecipientFnameInput.text.toString().trim()
        val lname = binding.addRecipientLnameInput.text.toString().trim()
        val relationship = binding.addRelationshipInput.text.toString().trim()
        val mobile = binding.addMobileInput.text.toString().trim()

        if (fname.isEmpty() || lname.isEmpty() || relationship.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (deviceId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Device not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Find the first empty slot
        val emptySlot = getFirstEmptySlot()
        if (emptySlot == null) {
            Toast.makeText(requireContext(), "Maximum 3 contacts allowed", Toast.LENGTH_SHORT).show()
            return
        }

        val newContact = mapOf(
            "fname" to fname,
            "lname" to lname,
            "relationship" to relationship,
            "contact" to mobile
        )

        val contactRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId)
            .child("emergencyContacts").child(emptySlot)

        contactRef.setValue(newContact)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Contact added successfully!", Toast.LENGTH_SHORT).show()
                binding.addContactCard.visibility = View.GONE
                binding.dimOverlay.visibility = View.GONE
                clearAddContactInputs()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to add contact: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearContact() {
        if (deviceId.isEmpty() || selectedContactKey == null) {
            Toast.makeText(requireContext(), "Error clearing contact", Toast.LENGTH_SHORT).show()
            return
        }

        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear Contact")
            .setMessage("Are you sure you want to clear this contact's information?")
            .setPositiveButton("Clear") { _, _ ->
                // Create empty contact data
                val emptyContact = mapOf(
                    "fname" to "",
                    "lname" to "",
                    "relationship" to "",
                    "contact" to ""
                )

                val contactRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("devices").child(deviceId)
                    .child("emergencyContacts").child(selectedContactKey!!)

                contactRef.setValue(emptyContact)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Contact cleared successfully!", Toast.LENGTH_SHORT).show()
                        binding.updateContactCard.visibility = View.GONE
                        binding.dimOverlay.visibility = View.GONE
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), "Failed to clear: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAddContactInputs() {
        binding.addRecipientFnameInput.text?.clear()
        binding.addRecipientLnameInput.text?.clear()
        binding.addRelationshipInput.text?.clear()
        binding.addMobileInput.text?.clear()
    }

    private fun setupDropdowns() {
        val relationships = arrayOf("Parent", "Sibling", "Spouse", "Child", "Friend", "Doctor", "Guardian", "Other")

        setupAdapter(binding.addRelationshipInput, relationships)
        setupAdapter(binding.updateRelationshipInput, relationships)
    }

    private fun setupAdapter(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
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
                    startActivity(Intent(requireContext(), ProfileActivity::class.java))
                    true
                }
                R.id.menu_logout -> {
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