package com.example.kelkin.Fragments

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentAdminDashboardBinding

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminDashboardBinding.bind(view)

        setupButtonListeners()
        applyFocusAnimations()
    }

    private fun setupButtonListeners() {
        binding.btnManageMovies.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_movieListFragment)
        }

        binding.btnManageTV.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_tvListFragment)
        }

    }

    private fun applyFocusAnimations() {
        val buttons = listOf(
            binding.btnManageMovies,
            binding.btnManageTV
        )

        buttons.forEach { btn ->
            btn.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                } else {
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}