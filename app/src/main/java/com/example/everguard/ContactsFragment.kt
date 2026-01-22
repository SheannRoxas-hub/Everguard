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
    private var currentContacts = mutableMapOf<String, EmergencyContact>()
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
            val contactsList = currentContacts.toList()
            if (contactsList.isNotEmpty()) {
                openUpdateContact(contactsList[0].first)
            }
        }

        binding.Contact2.setOnClickListener {
            val contactsList = currentContacts.toList()
            if (contactsList.size > 1) {
                openUpdateContact(contactsList[1].first)
            }
        }

        // Add Contact Button logic
        binding.btnAddContact.setOnClickListener {
            val contactCount = currentContacts.size
            if (contactCount >= 3) {
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

        // Delete Contact Button
        binding.btnDeleteContact.setOnClickListener {
            deleteContact()
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
                currentContacts.clear()

                for (contactSnapshot in snapshot.children) {
                    val key = contactSnapshot.key ?: continue

                    // Parse the contact data
                    val fname = contactSnapshot.child("fname").getValue(String::class.java) ?: ""
                    val lname = contactSnapshot.child("lname").getValue(String::class.java) ?: ""
                    val relationship = contactSnapshot.child("relationship").getValue(String::class.java) ?: ""
                    val contact = contactSnapshot.child("contact").getValue(String::class.java) ?: ""

                    if (fname.isNotEmpty() || lname.isNotEmpty()) {
                        currentContacts[key] = EmergencyContact(fname, lname, relationship, contact)
                    }
                }

                updateUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load contacts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI() {
        val contactsList = currentContacts.toList()

        // Update contact count
        binding.contactCount.text = "${currentContacts.size}/3"

        // Update Contact 1
        if (contactsList.isNotEmpty()) {
            val (key1, contact1) = contactsList[0]
            binding.contact1Name.text = "${contact1.fname} ${contact1.lname}"
            binding.contact1Relationship.text = contact1.relationship
            binding.contact1Phone.text = contact1.contact
            binding.Contact1.visibility = View.VISIBLE
        } else {
            binding.Contact1.visibility = View.GONE
        }

        // Update Contact 2
        if (contactsList.size > 1) {
            val (key2, contact2) = contactsList[1]
            binding.contact2Name.text = "${contact2.fname} ${contact2.lname}"
            binding.contact2Relationship.text = contact2.relationship
            binding.contact2Phone.text = contact2.contact
            binding.Contact2.visibility = View.VISIBLE
        } else {
            binding.Contact2.visibility = View.GONE
        }

        // Show/hide add button
        binding.btnAddContact.visibility = if (currentContacts.size < 3) View.VISIBLE else View.GONE
    }

    private fun openUpdateContact(contactKey: String) {
        selectedContactKey = contactKey
        val contact = currentContacts[contactKey]

        if (contact != null) {
            // Pre-fill the update form
            binding.updateRecipientFnameInput.setText(contact.fname)
            binding.updateRecipientLnameInput.setText(contact.lname)
            binding.updateRelationshipInput.setText(contact.relationship)
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

        contactRef.setValue(updatedContact)  // Changed from updateChildren to setValue
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

        if (currentContacts.size >= 3) {
            Toast.makeText(requireContext(), "Maximum 3 contacts allowed", Toast.LENGTH_SHORT).show()
            return
        }

        val newContact = mapOf(
            "fname" to fname,
            "lname" to lname,
            "relationship" to relationship,
            "contact" to mobile
        )

        // Determine the next contact key
        val nextContactNumber = currentContacts.size + 1
        val newContactKey = "emgperson$nextContactNumber"

        val contactRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId)
            .child("emergencyContacts").child(newContactKey)

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

    private fun deleteContact() {
        if (deviceId.isEmpty() || selectedContactKey == null) {
            Toast.makeText(requireContext(), "Error deleting contact", Toast.LENGTH_SHORT).show()
            return
        }

        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Delete") { _, _ ->
                val contactRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("devices").child(deviceId)
                    .child("emergencyContacts").child(selectedContactKey!!)

                contactRef.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Contact deleted successfully!", Toast.LENGTH_SHORT).show()
                        binding.updateContactCard.visibility = View.GONE
                        binding.dimOverlay.visibility = View.GONE

                        // Reorganize remaining contacts
                        reorganizeContacts()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), "Failed to delete: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reorganizeContacts() {
        if (deviceId.isEmpty()) return

        val contactsRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId).child("emergencyContacts")

        contactsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val contacts = mutableListOf<Map<String, String>>()

                for (contactSnapshot in snapshot.children) {
                    val fname = contactSnapshot.child("fname").getValue(String::class.java) ?: ""
                    val lname = contactSnapshot.child("lname").getValue(String::class.java) ?: ""
                    val relationship = contactSnapshot.child("relationship").getValue(String::class.java) ?: ""
                    val contact = contactSnapshot.child("contact").getValue(String::class.java) ?: ""

                    if (fname.isNotEmpty() || lname.isNotEmpty()) {
                        contacts.add(mapOf(
                            "fname" to fname,
                            "lname" to lname,
                            "relationship" to relationship,
                            "contact" to contact
                        ))
                    }
                }

                // Clear all contacts
                contactsRef.removeValue().addOnSuccessListener {
                    // Re-add contacts with correct keys
                    contacts.forEachIndexed { index, contactData ->
                        val newKey = "emgperson${index + 1}"
                        contactsRef.child(newKey).setValue(contactData)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
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