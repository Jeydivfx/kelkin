package com.example.kelkin.Fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.databinding.FragmentAdminDashboardBinding

class AdminDashboardFragment : AdminFragment(R.layout.fragment_admin_dashboard) {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminDashboardBinding.bind(view)

        setupButtonListeners()

        com.example.kelkin.utils.RecyclerFocusMemory(this, binding.btnManageMovies)
    }

    private fun setupButtonListeners() {
        binding.btnAdminBack.setOnClickListener {
            findNavController().popBackStack(R.id.settingsFragment, false)
        }
        binding.btnAdminSignOut.setOnClickListener {
            com.example.kelkin.utils.AdminSession.signOut()
            findNavController().popBackStack(R.id.settingsFragment, false)
        }
        binding.btnManageMovies.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_movieListFragment)
        }

        binding.btnManageTV.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_tvListFragment)
        }

        binding.btnManageSeries.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboard_to_seriesListFragment)
        }

    }

    override fun onResume() {
        super.onResume()

        (requireActivity() as? MainActivity)?.collapseSidebar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
