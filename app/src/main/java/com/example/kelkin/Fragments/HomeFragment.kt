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
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
    private var heroImageRequestKey: String? = null
    private lateinit var viewModel: HomeViewModel

    private lateinit var focusMemory: RecyclerFocusMemory
    private lateinit var trendingAdapter: MovieAdapter
    private lateinit var continueAdapter: HomeCardAdapter
    private lateinit var myListAdapter: MovieAdapter
    private var rowFocusScroll: HomeRowFocusScroll? = null


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
        if (com.example.kelkin.BuildConfig.IS_TV) {
            rowFocusScroll = HomeRowFocusScroll(binding.homeScrollView,
                listOf(binding.rvTrending, binding.rvContinue, binding.rvMylist))
        }
        val ui = binding
        ui.heroDescription.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) formatHeroSynopsis()
        }
        fun resizeHero() {
            if (ui.root.height == 0) return
            val density = resources.displayMetrics.density
            if (com.example.kelkin.BuildConfig.IS_TV) {
                // Reflow only the viewport when the menu opens, not the hero's text column.
                val collapsedWidth = ui.root.rootView.width - (60 * density).toInt()
                val heroWidth = (collapsedWidth * 0.56f).toInt()
                if (heroWidth > 0 && ui.heroContent.layoutParams.width != heroWidth) {
                    ui.heroContent.layoutParams = ui.heroContent.layoutParams.apply { width = heroWidth }
                }
                val sections = ui.rvTrending.parent as ViewGroup
                val menuGap = (14 * density).toInt()
                val oppositeGap = (8 * density).toInt()
                if (sections.paddingStart != menuGap || sections.paddingEnd != oppositeGap) {
                    // The three shelves share this container, so all physical gutters match.
                    sections.setPaddingRelative(menuGap, sections.paddingTop, oppositeGap, sections.paddingBottom)
                }
                // Keep scrolling artwork out of the outer gutters. Each shelf retains 12dp
                // of inner padding so focused cards can grow without clipping their border.
                sections.clipToPadding = true
                ui.rvTrending.clipToPadding = false
                ui.rvMylist.clipToPadding = false
            }
            // Reserve space for the complete first poster row, without negative overlap.
            val posterHeight = maxOf(ui.rvTrending.height, (204 * density).toInt())
            val headingHeight = maxOf(ui.titleTrending.height, (24 * density).toInt())
            val available = ui.root.height - posterHeight - headingHeight - (32 * density).toInt()
            val height = if (com.example.kelkin.BuildConfig.IS_TV)
                maxOf(available, ui.heroContent.height + (88 * density).toInt()) + (28 * density).toInt()
            else maxOf((350 * density).toInt(), ui.heroContent.height + (84 * density).toInt())
            if (ui.heroSection.layoutParams.height != height) {
                ui.heroSection.layoutParams = ui.heroSection.layoutParams.apply { this.height = height }
            }
            if (com.example.kelkin.BuildConfig.IS_TV) {
                // Let the soft fade extend slightly behind the top of the first poster row.
                val backdropHeight = height + headingHeight + (32 * density).toInt()
                if (ui.heroImage.layoutParams.height != backdropHeight) {
                    ui.heroImage.layoutParams = ui.heroImage.layoutParams.apply { this.height = backdropHeight }
                }
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
            adapter.yearProvider = { movie ->
                viewModel.movieDetailsMap.value?.get(metadataKey(movie.contentType, movie.tmdb_id))?.release_date?.take(4).orEmpty()
            }
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
        viewModel.heroFanartBackdrop.observe(viewLifecycleOwner) { renderUI() }
        viewModel.heroArtworkChoice.observe(viewLifecycleOwner) { renderUI() }
        viewModel.heroMovieKey.observe(viewLifecycleOwner) { renderUI() }
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

        trendingAdapter.submitList(processList(movies.sortedByDescending { it.id }.take(10))) {
            trendingAdapter.refreshHomeCaptions()
            focusMemory.restore()
        }


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
        myListAdapter.submitList(myList) {
            myListAdapter.refreshHomeCaptions()
            _binding?.rvMylist?.let { focusMemory.restoreAfterRemoval(it) }
        }
        if (myList.isEmpty()) {
            binding.rvMylist.visibility = View.GONE
            binding.emptyMylistText.visibility = View.VISIBLE
        } else {
            binding.rvMylist.visibility = View.VISIBLE
            binding.emptyMylistText.visibility = View.GONE
        }

        val featured = selectHeroMovie(movies, viewModel.heroMovieKey.value)
        if (featured != null) {
            val choice = viewModel.heroArtworkChoice.value?.takeIf { it.movieKey == featured.remoteKey }
            viewModel.fetchHeroBackdrop(featured.tmdb_id, choice?.number ?: 1, choice?.fanartUrl)
            viewModel.fetchTmdbDetails(featured.tmdb_id)
            updateHeroBanner(details[metadataKey("movie", featured.tmdb_id)], featured)
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

            val choice = viewModel.heroArtworkChoice.value?.takeIf { it.movieKey == fb.remoteKey }
            val tmdbImage = ImageUrls.backdrop(choice?.tmdbPath ?: tmdb?.backdrop_path)
            val fanartImage = viewModel.heroFanartBackdrop.value
                ?.takeIf { it.movieId == fb.tmdb_id && it.number == (choice?.number ?: 1) }?.url
            val imageKey = "${fb.tmdb_id}|$fanartImage|$tmdbImage"
            if (heroImageRequestKey != imageKey) {
                heroImageRequestKey = imageKey
                fun artworkListener(source: String) = object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        if (com.example.kelkin.BuildConfig.DEBUG) android.util.Log.d("HeroArtwork", "$source image failed for movie ${fb.tmdb_id}")
                        return false
                    }
                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        if (com.example.kelkin.BuildConfig.DEBUG) android.util.Log.d("HeroArtwork", "$source image loaded for movie ${fb.tmdb_id}")
                        return false
                    }
                }
                val fallback = Glide.with(heroImage).load(tmdbImage).dontTransform()
                    .diskCacheStrategy(DiskCacheStrategy.ALL).error(R.drawable.hero_test_header)
                    .listener(artworkListener("TMDB"))
                val request = Glide.with(heroImage).load(fanartImage ?: tmdbImage).dontTransform()
                    .placeholder(heroImage.drawable ?: androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.hero_test_header))
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(450))
                    .diskCacheStrategy(DiskCacheStrategy.ALL).error(R.drawable.hero_test_header)
                    .listener(artworkListener(if (fanartImage != null) "Fanart" else "TMDB"))
                if (fanartImage != null && tmdbImage != null) request.error(fallback)
                request.into(heroImage)
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
        if (com.example.kelkin.BuildConfig.IS_TV) {
            if (layout.lineCount <= 2) textView.text = source
            else {
                val split = layout.getLineStart(1)
                val last = android.text.TextUtils.ellipsize(source.substring(split), textView.paint,
                    width.toFloat(), android.text.TextUtils.TruncateAt.END)
                textView.text = source.substring(0, split).trimEnd() + "\n" + last
            }
            return
        }
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

    fun restoreFocusFromSidebar() {
        focusMemory.restoreFromSidebar()
    }


    override fun onDestroyView() {
        heroImageRequestKey = null
        rowFocusScroll?.dispose()
        rowFocusScroll = null
        super.onDestroyView()
        _binding = null
    }
}

