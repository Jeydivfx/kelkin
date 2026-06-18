package com.example.kelkin.Fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.MovieAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.R
import androidx.navigation.fragment.findNavController
import com.example.kelkin.Adapter.MovieGridAdapter
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentSearchBinding
import com.example.kelkin.utils.DatabaseHelper
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel



    // کیبورد فارسی مرتب شده
    private val persianKeys = "ا ب پ ت ث ج چ ح خ د ذ ر ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ه ی"
    private lateinit var movieAdapter: MovieGridAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        dbHelper = DatabaseHelper(requireContext())
        setupRecyclerView()
        setupKeyboard()

        // گوش دادن به تغییرات متن برای جستجوی آنی
        binding.edtSearch.addTextChangedListener { text ->
            performSearch(text.toString())
        }

        binding.edtSearch.requestFocus()
    }

    // در متد setupRecyclerView در SearchFragment
    // در SearchFragment.kt
    private fun setupRecyclerView() {
        movieAdapter = MovieGridAdapter { movie ->
            val bundle = Bundle().apply {
                // تغییر از movie_data به selected_movie
                putSerializable("selected_movie", movie)
            }
            findNavController().navigate(R.id.action_searchFragment_to_movieDetailFragment, bundle)
        }

        binding.rvResults.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = movieAdapter
        }
    }

    // In SearchFragment.kt
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            movieAdapter.submitList(emptyList())
            return
        }

        val results = dbHelper.searchMovies(query)

        // We launch a coroutine to fetch details for these search results
        viewLifecycleOwner.lifecycleScope.launch {
            val hydratedMovies = results.map { movie ->
                // Check if we need to fetch details
                if (movie.posterUrl.isNullOrEmpty()) {
                    // Assuming your viewModel has a function to fetch minimal details
                    // or you can call TMDB directly here
                    val details = viewModel.fetchMovieDetailsForPoster(movie.tmdb_id)
                    movie.apply { posterUrl = details.poster_path ?: "" }
                } else {
                    movie
                }
            }

            // Now submit the list with the filled poster URLs
            movieAdapter.submitList(hydratedMovies)
        }
    }
    private fun setupKeyboard() {
        val grid = binding.gridKeyboard
        val keysList = persianKeys.split(" ")

        keysList.forEach { char ->
            val button = createButton(char) {
                binding.edtSearch.append(char)
            }
            grid.addView(button)
        }

        addSpecialKey("پاک کردن") {
            val text = binding.edtSearch.text.toString()
            if (text.isNotEmpty()) binding.edtSearch.setText(text.dropLast(1))
        }
    }

    private fun createButton(text: String, onClick: () -> Unit): Button {
        return Button(requireContext()).apply {
            this.text = text
            val params = GridLayout.LayoutParams()
            params.width = 80 // یا استفاده از dimens
            params.height = 80
            params.setMargins(8, 8, 8, 8)
            layoutParams = params
            setBackgroundResource(R.drawable.selector_keyboard_key)
            setTextColor(Color.WHITE)
            isFocusable = true
            setOnFocusChangeListener { v, hasFocus ->
                (v as Button).setTextColor(if (hasFocus) Color.BLACK else Color.WHITE)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun addSpecialKey(labelText: String, onClick: () -> Unit) {
        val button = createButton(labelText, onClick)
        binding.gridKeyboard.addView(button)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}