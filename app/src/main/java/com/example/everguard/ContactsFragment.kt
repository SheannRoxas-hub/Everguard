package com.example.everguard
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.widget.PopupMenu
import com.example.everguard.databinding.FragmentContactsBinding

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Dropdowns
        setupDropdowns()

        // 2. Kebab Menu
        binding.homeKebabMenu.setOnClickListener { v ->
            showPopupMenu(v)
        }

        // 3. Contact Cards logic (Pressing a contact opens Update Card)
        binding.Contact1.setOnClickListener {
            binding.updateContactCard.visibility = View.VISIBLE
            binding.dimOverlay.visibility = View.VISIBLE
        }

        binding.Contact2.setOnClickListener {
            binding.updateContactCard.visibility = View.VISIBLE
            binding.dimOverlay.visibility = View.VISIBLE
        }

        // 4. Add Contact Button logic
        binding.btnAddContact.setOnClickListener {
            binding.addContactCard.visibility = View.VISIBLE
            binding.dimOverlay.visibility = View.VISIBLE
        }

        // 5. Close Buttons and Overlay Dismiss
        val closeAction = View.OnClickListener {
            binding.updateContactCard.visibility = View.GONE
            binding.addContactCard.visibility = View.GONE
            binding.dimOverlay.visibility = View.GONE
        }

        binding.btnClosePopupUpdate.setOnClickListener(closeAction)
        binding.btnClosePopupAdd.setOnClickListener(closeAction)
        binding.dimOverlay.setOnClickListener(closeAction)

        // 6. Action Buttons (Toast messages)
        binding.updateRecipientChangesBtn.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Contact updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
            closeAction.onClick(it)
        }

        binding.addRecipientBtn.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Contact added successfully!", android.widget.Toast.LENGTH_SHORT).show()
            closeAction.onClick(it)
        }
    }

    // --- Dropdown Implementation ---
    private fun setupDropdowns() {
        val relationships = arrayOf("Parent", "Sibling", "Spouse", "Child", "Friend", "Other")

        // 1. Setup for the Add Contact card
        setupAdapter(binding.addRelationshipInput, relationships)

        // 2. Setup for the Update Contact card
        setupAdapter(binding.updateRelationshipInput, relationships)
    }

    private fun setupAdapter(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)

        // Forces the dropdown to show when tapped
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
        } catch (e: Exception) { e.printStackTrace() }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_profile -> {
                    startActivity(Intent(requireContext(), ProfileActivity::class.java))
                    true
                }
                R.id.menu_logout -> {
                    val intent = Intent(requireContext(), RegisterActivity::class.java)
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