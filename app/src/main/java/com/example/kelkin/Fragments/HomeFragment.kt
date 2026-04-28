package com.example.kelkin.Fragments

import android.os.Bundle
import android.util.Log
import androidx.navigation.fragment.findNavController
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.kelkin.Adapter.MovieAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.TmdbMovieDetails
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentHomeBinding
import com.example.kelkin.`object`.TranslationHelper
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel

    private lateinit var trendingAdapter: MovieAdapter
    private lateinit var continueAdapter: MovieAdapter
    private lateinit var myListAdapter: MovieAdapter
    private var isReturningFromDetails = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        //Collapse sidebar in the beginning when the app starts
        val sidebar = requireActivity().findViewById<ViewGroup>(R.id.sidebar)
        view.postDelayed({
            sidebar.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }, 100)

        binding.btnPlay.requestFocus()
        setupRecyclerViews()
        observeData()

        SetupHeroButtonsScroll()

        viewModel.fetchData()
        view.postDelayed({
            Log.d("MONITOR", "Checking after 5 seconds...")
            Log.d("MONITOR", "ViewModel instance: ${viewModel.hashCode()}")
            Log.d("MONITOR", "MoviesList value: ${viewModel.moviesList.value?.size ?: "is NULL"}")
        }, 5000)

        viewModel.moviesList.value?.let { movies ->
            if (movies.isNotEmpty()) {

                trendingAdapter.submitList(movies.sortedByDescending { it.id }.take(6))
                continueAdapter.submitList(movies.shuffled())
                myListAdapter.submitList(movies.reversed())

                if (!viewModel.movieDetailsMap.value.isNullOrEmpty()) {
                    trendingAdapter.notifyDataSetChanged()
                }
            }
        }



    }

    private fun setupRecyclerViews() {
        val onMovieClick: (Movie) -> Unit = { movie ->
            Log.d("Check_Data", "Navigating to Details for: ${movie.name_fa}")

            val bundle = Bundle().apply {
                putSerializable("selected_movie", movie)
            }

            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.action_homeFragment_to_movieDetailFragment, bundle)
        }


        trendingAdapter = MovieAdapter(onMovieClick = onMovieClick)
        continueAdapter = MovieAdapter(onMovieClick = onMovieClick)
        myListAdapter = MovieAdapter(onMovieClick = onMovieClick)


        binding.rvTrending.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = trendingAdapter
        }
        binding.rvContinue.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = continueAdapter
        }
        binding.rvMylist.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = myListAdapter
        }
    }


    private fun observeData() {

        viewModel.movieDetailsMap.observe(viewLifecycleOwner) { detailsMap ->
            val movies = viewModel.moviesList.value ?: return@observe

            viewModel.myList.value?.let { userList ->
                val updatedUserList = userList.map { movie ->
                    movie.copy().apply {
                        posterUrl = detailsMap[tmdb_id]?.poster_path ?: ""
                    }
                }
                myListAdapter.submitList(ArrayList(updatedUserList.reversed()))
            }


            viewModel.continueWatchingList.value?.let { continueList ->
                val updated = continueList.map { movie ->
                    movie.copy().apply { posterUrl = detailsMap[tmdb_id]?.poster_path ?: "" }
                }
                continueAdapter.submitList(ArrayList(updated))
            }

            val updatedMovies = movies.map { movie ->
                movie.copy().apply {
                    posterUrl = detailsMap[tmdb_id]?.poster_path ?: ""
                }
            }

            updateAdapters(updatedMovies)

            val latestMovie = updatedMovies.maxByOrNull { it.id }
            val heroDetails = detailsMap[latestMovie?.tmdb_id]
            if (latestMovie != null && heroDetails != null) {
                updateHeroBanner(heroDetails, latestMovie)
            }
        }


        viewModel.moviesList.observe(viewLifecycleOwner) { movies ->
            if (movies.isNullOrEmpty()) return@observe

            val detailsMap = viewModel.movieDetailsMap.value ?: emptyMap()

            val updatedMovies = movies.map { movie ->
                movie.copy().apply {
                    posterUrl = detailsMap[tmdb_id]?.poster_path ?: ""
                }
            }

            updateAdapters(updatedMovies)

            val apiKey = viewModel.credentials.value?.apiKey ?: ""
            if (apiKey.isNotEmpty()) {
                movies.forEach { movie ->
                    viewModel.fetchTmdbDetails(movie.tmdb_id, apiKey)
                }
            }

            val latestMovie = updatedMovies.maxByOrNull { it.id }
            val heroDetails = detailsMap[latestMovie?.tmdb_id]
            if (latestMovie != null && heroDetails != null) {
                updateHeroBanner(heroDetails, latestMovie)
            }
        }

        viewModel.myList.observe(viewLifecycleOwner) { userList ->
            val detailsMap = viewModel.movieDetailsMap.value ?: emptyMap()
            val updatedUserList = userList.map { movie ->
                movie.copy().apply {
                    posterUrl = detailsMap[tmdb_id]?.poster_path ?: ""
                }
            }
            myListAdapter.submitList(ArrayList(updatedUserList.reversed()))
        }

        viewModel.continueWatchingList.observe(viewLifecycleOwner) { continueList ->

            val detailsMap = viewModel.movieDetailsMap.value ?: emptyMap()
            val updatedList = continueList.map { movie ->
                movie.copy().apply {
                    posterUrl = detailsMap[tmdb_id]?.poster_path ?: ""
                }
            }
            continueAdapter.submitList(ArrayList(updatedList))
        }

    }

    private fun updateAdapters(movies: List<Movie>) {

        val sortedMovies = movies.sortedByDescending { it.id }

        trendingAdapter.submitList(sortedMovies.take(6))

    }

    private fun updateHeroBanner(tmdb: TmdbMovieDetails, fb: Movie) {
        binding.apply {
            val mainTitle = tmdb.title
            val backupTitle = fb.name_fa


            if (!mainTitle.isNullOrEmpty()) {
                heroTitle.text = mainTitle
            } else {
                heroTitle.text = backupTitle
            }

            heroDescription.text = fb.description_fa

            val rawRating = String.format(Locale.ENGLISH, "%.1f", tmdb.vote_average)
            imdbScore.text = rawRating.toPersianDigits()

            val rawYear = if (tmdb.release_date.length >= 4) tmdb.release_date.substring(0, 4) else ""
            heroYear.text = rawYear.toPersianDigits()


            val rawGenres = tmdb.genres.joinToString(", ") { it.name }
            heroGenre.text = TranslationHelper.translateGenres(rawGenres)


            Glide.with(this@HomeFragment)
                .load("https://image.tmdb.org/t/p/original${tmdb.backdrop_path}")
                .into(heroImage)


            btnPlay.setOnClickListener {
                if (!fb.videoUrl1.isNullOrEmpty()) {
                    val intent = android.content.Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
                        putExtra("video_url", fb.videoUrl1)
                    }
                    startActivity(intent)
                } else {
                    android.widget.Toast.makeText(requireContext(), "لینک ویدیو برای این فیلم موجود نیست", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            btnInfo.setOnClickListener {

                val bundle = Bundle().apply {
                    putSerializable("selected_movie", fb)
                }

                androidx.navigation.fragment.NavHostFragment.findNavController(this@HomeFragment)
                    .navigate(R.id.action_homeFragment_to_movieDetailFragment, bundle)
            }
        }
    }


    private fun SetupHeroButtonsScroll()
    {
        binding.apply {
            val HeroFocusChangeListener = View.OnFocusChangeListener {v, hasFocus ->
                if(hasFocus){
                    homeScrollView.smoothScrollTo(0,0)
                }
            }

            btnPlay.onFocusChangeListener = HeroFocusChangeListener
            btnInfo.onFocusChangeListener = HeroFocusChangeListener
        }

    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadContinueList()
    }
}