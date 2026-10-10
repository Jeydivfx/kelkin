package com.example.kelkin.Fragments

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kelkin.R
import com.example.kelkin.utils.AdminSession
import kotlinx.coroutines.launch

open class AdminFragment(layout: Int) : Fragment(layout) {
    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            val allowed = try { AdminSession.verify() }
                catch (e: kotlinx.coroutines.TimeoutCancellationException) { false }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { false }
            if (!allowed && isAdded && view != null) {
                findNavController().navigate(R.id.settingsFragment, null, androidx.navigation.navOptions {
                    popUpTo(R.id.adminDashboardFragment) { inclusive = true }
                    launchSingleTop = true
                })
            }
        }
    }
}
