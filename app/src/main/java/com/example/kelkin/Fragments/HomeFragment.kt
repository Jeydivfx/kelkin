package com.example.kelkin.Fragments

import com.example.kelkin.utils.*

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.kelkin.Adapter.MovieAdapter
import com.example.kelkin.Adapter.HomeCardAdapter
import com.example.kelkin.Adapter.HomeCard
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.TmdbMovieDetails
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentHomeBinding
import com.example.kelkin.`object`.TranslationHelper
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits
import com.google.gson.Gson
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!
    private var heroSynopsis = ""
    private lateinit var viewModel: HomeViewModel

    private lateinit var focusMemory: RecyclerFocusMemory
    private lateinit var trendingAdapter: MovieAdapter
    private lateinit var continueAdapter: HomeCardAdapter
    private lateinit var myListAdapter: MovieAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        setupRecyclerViews()
        focusMemory = RecyclerFocusMemory(this, binding.btnPlay)
        listOf(binding.rvTrending, binding.rvContinue, binding.rvMylist).forEach { focusMemory.track(it) }
        observeData()
        setupHeroButtonsScroll()
        val ui = binding
        ui.heroDescription.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) formatHeroSynopsis()
        }
        fun resizeHero() {
            if (ui.root.height == 0) return
            val density = resources.displayMetrics.density
            // Reserve space for the complete first poster row, without negative overlap.
            val posterHeight = maxOf(ui.rvTrending.height, (204 * density).toInt())
            val headingHeight = maxOf(ui.titleTrending.height, (24 * density).toInt())
            val available = ui.root.height - posterHeight - headingHeight - (32 * density).toInt()
            val height = if (com.example.kelkin.BuildConfig.IS_TV)
                maxOf(available, ui.heroContent.height + (88 * density).toInt())
            else maxOf((350 * density).toInt(), ui.heroContent.height + (84 * density).toInt())
            if (ui.heroSection.layoutParams.height != height) {
                ui.heroSection.layoutParams = ui.heroSection.layoutParams.apply { this.height = height }
            }
        }
        ui.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> resizeHero() }
        ui.heroContent.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> resizeHero() }



    }

    private fun setupRecyclerViews() {
        val onMovieClick: (Movie) -> Unit = onMovieClick@{ movie ->
            val navController = NavHostFragment.findNavController(this)
            if (movie.contentType == "series") {
                val series = viewModel.seriesList.value?.find { it.identity() == movie.identity() }
                if (series == null) {
                    Toast.makeText(context, "این سریال در آرشیو فعلی موجود نیست", Toast.LENGTH_SHORT).show()
                    return@onMovieClick
                }
                navController.navigate(R.id.action_homeFragment_to_seriesDetailsFragment, Bundle().apply { putSerializable("selected_series", series) })
            } else {
                navController.navigate(R.id.action_homeFragment_to_movieDetailFragment, Bundle().apply { putSerializable("selected_movie", movie) })
            }
        }
        trendingAdapter = MovieAdapter(onMovieClick).apply { posterSizeScale = 1.1f; homePolish = true }
        continueAdapter = HomeCardAdapter(true, onMovieClick)
        myListAdapter = MovieAdapter(onMovieClick).apply { posterSizeScale = 1.1f; homePolish = true }
        listOf(trendingAdapter, myListAdapter).forEach { adapter ->
            adapter.onVisible = { movie ->
                if (movie.contentType == "series") viewModel.fetchSeriesTmdbDetails(movie.tmdb_id) else viewModel.fetchTmdbDetails(movie.tmdb_id)
            }
        }
        continueAdapter.onVisible = { movie ->
            if (movie.contentType == "series") viewModel.fetchSeriesTmdbDetails(movie.tmdb_id)
            else viewModel.fetchTmdbDetails(movie.tmdb_id)
        }
        listOf(binding.rvTrending to trendingAdapter, binding.rvContinue to continueAdapter, binding.rvMylist to myListAdapter).forEach { (list, adapter) ->
            list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            list.itemAnimator = null
            list.adapter = adapter
        }
    }
    private fun observeData() {
        viewModel.moviesList.observe(viewLifecycleOwner) { movies ->
            movies.orEmpty().sortedByDescending { it.id }.take(10).forEach { viewModel.fetchTmdbDetails(it.tmdb_id) }
            renderUI()
        }
        viewModel.seriesList.observe(viewLifecycleOwner) { renderUI() }
        viewModel.myList.observe(viewLifecycleOwner) { items -> hydrateCards(items.orEmpty()); renderUI() }
        viewModel.continueWatchingList.observe(viewLifecycleOwner) { items -> hydrateCards(items.orEmpty()); renderUI() }
        viewModel.movieDetailsMap.observe(viewLifecycleOwner) { renderUI() }
        viewModel.languagesList.observe(viewLifecycleOwner) { renderUI() }
        viewModel.qualitiesList.observe(viewLifecycleOwner) { renderUI() }
        viewModel.syncError.observe(viewLifecycleOwner) { error ->
            if (viewModel.moviesList.value.isNullOrEmpty()) {
                binding.heroTitle.text = "به کلکین خوش آمدید"
                binding.heroDescription.text = error ?: "آرشیو هنوز خالی است."
                binding.btnPlay.text = "تلاش دوباره"
                binding.btnPlay.contentDescription = "تلاش دوباره"
                binding.btnPlay.setOnClickListener { viewModel.refresh() }
                binding.btnInfo.visibility = View.GONE
            }
        }
    }

    private fun hydrateCards(items: List<Movie>) {
        items.take(20).forEach {
            if (it.contentType == "series") viewModel.fetchSeriesTmdbDetails(it.tmdb_id) else viewModel.fetchTmdbDetails(it.tmdb_id)
        }
    }

    private fun renderUI() {
        val movies = viewModel.moviesList.value ?: return
        val details = viewModel.movieDetailsMap.value ?: emptyMap()

        fun processList(list: List<Movie>): List<Movie> {
            return list.map { movie ->
                val tmdbDetails = details[metadataKey(movie.contentType, movie.tmdb_id)]
                movie.copy(
                    posterUrl = tmdbDetails?.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" } ?: movie.posterUrl,
                    voteAverage = tmdbDetails?.vote_average ?: movie.voteAverage
                )
            }
        }

        trendingAdapter.submitList(processList(movies.sortedByDescending { it.id }.take(10))) { focusMemory.restore() }


        val continueList = processList(viewModel.continueWatchingList.value ?: emptyList())
        continueAdapter.submitList(continueList.map { movie ->
            val backdrop = details[metadataKey(movie.contentType, movie.tmdb_id)]?.backdrop_path
            HomeCard(movie, ImageUrls.backdrop(backdrop) ?: ImageUrls.poster(movie.posterUrl))
        }) { _binding?.rvContinue?.let { focusMemory.restoreAfterRemoval(it) } }
        if (continueList.isEmpty()) {
            binding.rvContinue.visibility = View.GONE
            binding.emptyContinueText.visibility = View.VISIBLE
        } else {
            binding.rvContinue.visibility = View.VISIBLE
            binding.emptyContinueText.visibility = View.GONE
        }


        val myList = processList(viewModel.myList.value ?: emptyList())
        myListAdapter.submitList(myList) { _binding?.rvMylist?.let { focusMemory.restoreAfterRemoval(it) } }
        if (myList.isEmpty()) {
            binding.rvMylist.visibility = View.GONE
            binding.emptyMylistText.visibility = View.VISIBLE
        } else {
            binding.rvMylist.visibility = View.VISIBLE
            binding.emptyMylistText.visibility = View.GONE
        }

        val latest = movies.maxByOrNull { it.id }
        if (latest != null) {
            updateHeroBanner(details[metadataKey("movie", latest.tmdb_id)], latest)
        }
    }

    private fun updateHeroBanner(tmdb: TmdbMovieDetails?, fb: Movie) {
        binding.apply {
            btnPlay.text = "پخش"
            btnPlay.contentDescription = "پخش فیلم"
            btnInfo.visibility = View.VISIBLE
            heroTitle.text = if (!fb.name_fa.isNullOrEmpty()) fb.name_fa else (tmdb?.title ?: "")
            heroSynopsis = fb.description_fa.orEmpty()
            formatHeroSynopsis()

            // تبدیل هوشمند شناسه کیفیت به نام اصلی (دقیقاً مثل MovieDetailFragment)
            val qualityName = viewModel.qualitiesList.value?.find {
                it.id.toString() == fb.quality || "qu_%02d".format(it.id) == fb.quality || it.name.equals(fb.quality, ignoreCase = true)
            }?.name?.toPersianDigits()

            if (!qualityName.isNullOrEmpty()) {
                heroQuality.text = qualityName
                heroQuality.visibility = View.VISIBLE
            } else {
                heroQuality.visibility = View.GONE
            }

            // تبدیل هوشمند شناسه زبان به نام اصلی
            val languageName = viewModel.languagesList.value?.find {
                it.id.toString() == fb.language || "lan_%02d".format(it.id) == fb.language || it.name.equals(fb.language, ignoreCase = true)
            }?.name?.toPersianDigits()

            if (!languageName.isNullOrEmpty()) {
                heroLanguage.text = languageName
                heroLanguage.visibility = View.VISIBLE
            } else {
                heroLanguage.visibility = View.GONE
            }

            // سال ساخت از TMDB
            val releaseDate = tmdb?.release_date
            if (!releaseDate.isNullOrEmpty() && releaseDate.length >= 4) {
                val year = releaseDate.substring(0, 4)
                heroYear.text = year.toPersianDigits()
                heroYear.visibility = View.VISIBLE
            } else {
                heroYear.visibility = View.GONE
            }

            if (tmdb != null && !tmdb.backdrop_path.isNullOrEmpty()) {
                Glide.with(heroImage)
                    .load(ImageUrls.backdrop(tmdb.backdrop_path))
                    .dontTransform()
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(450))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(heroImage)
            } else {
                heroImage.setImageResource(R.drawable.hero_test_header)
            }

            btnPlay.setOnClickListener {
                if (!fb.videoUrl1.isNullOrEmpty()) {
                    val intent = Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
                        putExtra("video_url", fb.videoUrl1)
                        putExtra("movie_json", Gson().toJson(fb))
                        putExtra("movie_tmdb_id", fb.tmdb_id)
                        putExtra("start_pos", 0L)
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

    private fun formatHeroSynopsis() {
        val ui = _binding ?: return
        val textView = ui.heroDescription
        val width = textView.width - textView.paddingLeft - textView.paddingRight
        val source = heroSynopsis.replace(Regex("\\s+"), " ").trim()
        if (width <= 0 || source.isEmpty()) {
            textView.text = source
            return
        }
        val layout = android.text.StaticLayout.Builder.obtain(source, 0, source.length, textView.paint, width)
            .setTextDirection(android.text.TextDirectionHeuristics.RTL)
            .setIncludePad(false).build()
        if (layout.lineCount <= 1) {
            textView.text = source
            return
        }
        val split = layout.getLineEnd(0)
        val first = source.substring(0, split).trimEnd()
        val remainder = source.substring(split).trimStart()
        val second = android.text.TextUtils.ellipsize(remainder, textView.paint, width * 0.5f,
            android.text.TextUtils.TruncateAt.END)
        textView.text = "$first\n$second"
    }

    private fun setupHeroButtonsScroll() {
        val listener = View.OnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.homeScrollView.smoothScrollTo(0, 0) }
        binding.btnPlay.onFocusChangeListener = listener
        binding.btnInfo.onFocusChangeListener = listener
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadContinueList()
        (requireActivity() as? MainActivity)?.collapseSidebar()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

