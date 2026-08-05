package com.example.kelkin.Fragments

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelkin.Adapter.MovieAdapter
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentSearchBinding
import com.example.kelkin.utils.DatabaseHelper
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var dbHelper: DatabaseHelper


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        dbHelper = DatabaseHelper(requireContext())

        setupRecyclerView()
        setupKeyboard()

        binding.edtSearch.addTextChangedListener { text ->
            performSearch(text.toString())
        }

        binding.edtSearch.requestFocus()
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter { movie ->
            val bundle = Bundle().apply {
                putSerializable("selected_movie", movie)
            }
            findNavController().navigate(R.id.action_searchFragment_to_movieDetailFragment, bundle)
        }

        binding.rvResults.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = movieAdapter
            clipToPadding = false
            setPadding(32, 0, 32, 0)
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            movieAdapter.submitList(emptyList())
            return
        }

        val results = dbHelper.searchMovies(query)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val hydratedMovies = results.map { movie ->

                    if (!movie.tmdb_id.isNullOrEmpty()) {
                        try {
                            val details = viewModel.fetchMovieDetailsForPoster(movie.tmdb_id)
                            movie.copy(
                                posterUrl = if (movie.posterUrl.isNullOrEmpty()) (details.poster_path ?: "") else movie.posterUrl,
                                voteAverage = if (details.vote_average > 0.0) details.vote_average else movie.voteAverage
                            )
                        } catch (e: Exception) {
                            movie
                        }
                    } else {
                        movie
                    }
                }
                movieAdapter.submitList(hydratedMovies)
            } catch (e: Exception) {
                Log.e("SearchFragment", "خطای دریافت اطلاعات: ${e.message}")
                movieAdapter.submitList(results)
            }
        }
    }

    private fun setupKeyboard() {
        val grid = binding.gridKeyboard
        val keys = "ا ب پ ت ث ج چ ح خ د ذ ر ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ه ی ⌫".split(" ")

        val row1 = keys.subList(0, 12)
        val row2 = keys.subList(12, 24)
        val row3 = keys.subList(24, keys.size)

        fun addRow(list: List<String>) {
            list.forEach { char ->
                val action = if (char == "⌫") {
                    { val text = binding.edtSearch.text.toString()
                        if (text.isNotEmpty()) binding.edtSearch.setText(text.dropLast(1)) }
                } else {
                    { binding.edtSearch.append(char) }
                }
                grid.addView(createButton(char, action))
            }
        }

        addRow(row1)
        addRow(row2)
        addRow(row3)
    }

    private fun createButton(text: String, onClick: () -> Unit): Button {
        return Button(requireContext()).apply {
            this.text = text


            val sizeInPx = (40 * resources.displayMetrics.density).toInt()
            val marginInPx = (4 * resources.displayMetrics.density).toInt()

            val params = GridLayout.LayoutParams().apply {
                width = sizeInPx
                height = sizeInPx
                setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
            }
            layoutParams = params

            setBackgroundResource(R.drawable.selector_keyboard_key)
            setTextColor(Color.WHITE)
            textSize = 16f
            isFocusable = true

            setOnFocusChangeListener { v, hasFocus ->
                (v as Button).setTextColor(if (hasFocus) Color.BLACK else Color.WHITE)
                v.animate().scaleX(if (hasFocus) 1.1f else 1.0f)
                    .scaleY(if (hasFocus) 1.1f else 1.0f)
                    .setDuration(150).start()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun addSpecialKey(labelText: String, onClick: () -> Unit) {
        binding.gridKeyboard.addView(createButton(labelText, onClick))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as? MainActivity)?.collapseSidebar()
    }
}