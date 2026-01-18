package com.example.everguard

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.example.everguard.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Open alert details popup
        val openAlertDetails = View.OnClickListener {
            binding.alertDetailsPopup2.visibility = View.VISIBLE
            binding.dimOverlay2.visibility = View.VISIBLE
        }

        binding.AccidentAlert2.setOnClickListener(openAlertDetails)

        // Dim Overlay
        binding.dimOverlay2.setOnClickListener {
            binding.alertDetailsPopup2.visibility = View.GONE
            binding.dimOverlay2.visibility = View.GONE
        }

        binding.dimOverlay2.isClickable = true
        binding.dimOverlay2.isFocusable = true

        // Call/SMS logic
        binding.btnCall2.setOnClickListener {
            // goodlack
        }
        binding.btnSms2.setOnClickListener {
            // 67
        }

        // Kebab Menu Logic
        binding.homeKebabMenu.setOnClickListener { v ->
            showPopupMenu(v)
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
                    // Navigate to Profile
                    val intent = Intent(requireContext(), ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_logout -> {
                    // Navigate to Register
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