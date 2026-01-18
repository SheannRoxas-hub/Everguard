package com.example.everguard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.example.everguard.databinding.FragmentHomeBinding
import android.content.Intent


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        // 3. Handle Call/SMS Logic
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