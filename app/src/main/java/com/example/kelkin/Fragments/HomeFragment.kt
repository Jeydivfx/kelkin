package com.example.kelkin.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.kelkin.Adapter.MovieAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.TmdbMovieDetails
import com.example.kelkin.PlayerActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentHomeBinding
import com.example.kelkin.`object`.TranslationHelper
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits
import com.google.gson.Gson
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel

    private lateinit var trendingAdapter: MovieAdapter
    private lateinit var continueAdapter: MovieAdapter
    private lateinit var myListAdapter: MovieAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        setupSidebarFocus()
        setupRecyclerViews()
        observeData()
        setupHeroButtonsScroll()
    }

    private fun setupRecyclerViews() {
        val onMovieClick: (Movie) -> Unit = { movie ->
            val bundle = Bundle().apply { putSerializable("selected_movie", movie) }
            NavHostFragment.findNavController(this)
                .navigate(R.id.action_homeFragment_to_movieDetailFragment, bundle)
        }

        trendingAdapter = MovieAdapter(onMovieClick = onMovieClick)
        continueAdapter = MovieAdapter(onMovieClick = onMovieClick)
        myListAdapter = MovieAdapter(onMovieClick = onMovieClick)

        binding.rvTrending.apply { layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false); adapter = trendingAdapter }
        binding.rvContinue.apply { layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false); adapter = continueAdapter }
        binding.rvMylist.apply { layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false); adapter = myListAdapter }
    }

    private fun observeData() {
        viewModel.moviesList.observe(viewLifecycleOwner) { movies ->
            if (!movies.isNullOrEmpty()) {
                movies.forEach { movie ->
                    viewModel.fetchTmdbDetails(movie.tmdb_id)
                }
            }
            renderUI()
        }

        viewModel.movieDetailsMap.observe(viewLifecycleOwner) { renderUI() }
        viewModel.myList.observe(viewLifecycleOwner) { renderUI() }
        viewModel.continueWatchingList.observe(viewLifecycleOwner) { renderUI() }
    }

    private fun renderUI() {
        val movies = viewModel.moviesList.value ?: return
        val details = viewModel.movieDetailsMap.value ?: emptyMap()

        fun processList(list: List<Movie>): List<Movie> {
            return list.map { movie ->
                movie.copy(posterUrl = details[movie.tmdb_id]?.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" } ?: movie.posterUrl)
            }
        }

        trendingAdapter.submitList(processList(movies.sortedByDescending { it.id }.take(6)))
        continueAdapter.submitList(processList(viewModel.continueWatchingList.value?.take(6) ?: emptyList()))
        myListAdapter.submitList(processList(viewModel.myList.value?.take(6) ?: emptyList()))

        val latest = movies.maxByOrNull { it.id }
        if (latest != null) {
            updateHeroBanner(details[latest.tmdb_id], latest)
        }
    }

    private fun updateHeroBanner(tmdb: TmdbMovieDetails?, fb: Movie) {
        binding.apply {
            //heroTitle.text = tmdb?.title ?: fb.name_fa
            heroTitle.text = tmdb?.title
            heroDescription.text = fb.description_fa

            if (tmdb != null) {
                imdbScore.text = String.format(Locale.ENGLISH, "%.1f", tmdb.vote_average).toPersianDigits()
                heroYear.text = tmdb.release_date.take(4).toPersianDigits()
                heroGenre.text = TranslationHelper.translateGenres(tmdb.genres.joinToString(", ") { it.name })
                Glide.with(requireContext())
                    .load("https://image.tmdb.org/t/p/original${tmdb.backdrop_path}")
                    .into(heroImage)
            } else {
                imdbScore.text = "--"
                heroYear.text = "--"
                heroGenre.text = "نامشخص"
                heroImage.setImageResource(R.drawable.hero_test_header) // Set a local placeholder
            }

            btnPlay.setOnClickListener {
                if (!fb.videoUrl1.isNullOrEmpty()) {
                    val intent = Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
                        putExtra("video_url", fb.videoUrl1)
                        putExtra("movie_json", Gson().toJson(fb))
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(context, "لینک ویدیو موجود نیست", Toast.LENGTH_SHORT).show()
                }
            }

            btnInfo.setOnClickListener {
                val bundle = Bundle().apply { putSerializable("selected_movie", fb) }
                NavHostFragment.findNavController(this@HomeFragment)
                    .navigate(R.id.action_homeFragment_to_movieDetailFragment, bundle)
            }
        }

    }

    private fun setupSidebarFocus() {
        val sidebar = requireActivity().findViewById<ViewGroup>(R.id.sidebar)
        binding.btnPlay.requestFocus()
        binding.root.postDelayed({ sidebar.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS }, 100)
    }

    private fun setupHeroButtonsScroll() {
        val listener = View.OnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.homeScrollView.smoothScrollTo(0, 0) }
        binding.btnPlay.onFocusChangeListener = listener
        binding.btnInfo.onFocusChangeListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}