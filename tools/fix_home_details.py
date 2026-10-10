from rewrite_support import *
j='app/src/main/java/com/example/kelkin/'
p=j+'Fragments/HomeFragment.kt'
imports(p,['com.example.kelkin.utils.*'])
edit(p,'    private lateinit var trendingAdapter:', '    private lateinit var focusMemory: RecyclerFocusMemory\n    private lateinit var trendingAdapter:',1)
edit(p,'        observeData()', '''        focusMemory = RecyclerFocusMemory(this, binding.btnPlay)
        listOf(binding.rvTrending, binding.rvContinue, binding.rvMylist).forEach { focusMemory.track(it) }
        observeData()''',1)
edit(p,'        binding.btnPlay.post {\n            binding.btnPlay.requestFocus()\n        }','')
edit(p,'            val correspondingSeries = viewModel.seriesList.value?.find {\n                it.tmdb_id == movie.tmdb_id || it.id.toString() == movie.id.toString()\n            }','''            val correspondingSeries = if (movie.contentType == "series") viewModel.seriesList.value?.find {
                it.identity() == movie.identity()
            } else null
            if (movie.contentType == "series" && correspondingSeries == null) {
                Toast.makeText(context, "این سریال در آرشیو فعلی موجود نیست", Toast.LENGTH_SHORT).show()
                return@onMovieClick
            }''')
# A labeled lambda is needed for an early return.
edit(p,'val onMovieClick: (Movie) -> Unit = { movie ->','val onMovieClick: (Movie) -> Unit = onMovieClick@{ movie ->')
method(p,'setupRecyclerViews', '''    private fun setupRecyclerViews() {
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
        trendingAdapter = MovieAdapter(onMovieClick)
        continueAdapter = MovieAdapter(onMovieClick)
        myListAdapter = MovieAdapter(onMovieClick)
        listOf(binding.rvTrending to trendingAdapter, binding.rvContinue to continueAdapter, binding.rvMylist to myListAdapter).forEach { (list, adapter) ->
            list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            list.adapter = adapter
        }
    }''')
method(p,'observeData','''    private fun observeData() {
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
                binding.btnPlay.setOnClickListener { viewModel.refresh() }
                binding.btnInfo.visibility = View.GONE
            }
        }
    }

    private fun hydrateCards(items: List<Movie>) {
        items.take(20).forEach {
            if (it.contentType == "series") viewModel.fetchSeriesTmdbDetails(it.tmdb_id) else viewModel.fetchTmdbDetails(it.tmdb_id)
        }
    }''')
edit(p,'details[movie.tmdb_id]','details[metadataKey(movie.contentType, movie.tmdb_id)]')
edit(p,'?.take(10) ?: emptyList()', ' ?: emptyList()')
edit(p,'details[latest.tmdb_id]','details[metadataKey("movie", latest.tmdb_id)]')
edit(p,'voteAverage = tmdbDetails?.vote_average ?: 0.0','voteAverage = tmdbDetails?.vote_average ?: movie.voteAverage')
edit(p,'        binding.apply {\n            heroTitle.text', '        binding.apply {\n            btnPlay.text = "پخش"\n            btnInfo.visibility = View.VISIBLE\n            heroTitle.text')
edit(p,'Glide.with(requireContext())','Glide.with(heroImage)')
edit(p,'"https://image.tmdb.org/t/p/original${tmdb.backdrop_path}"','ImageUrls.backdrop(tmdb.backdrop_path)')
edit(p,'putExtra("last_pos", 0L)','putExtra("start_pos", 0L)')
edit(p,'myListAdapter.submitList(myList)','myListAdapter.submitList(myList) { focusMemory.restore() }')
edit(p,'continueAdapter.submitList(continueList)','continueAdapter.submitList(continueList) { focusMemory.restore() }')
edit(p,'trendingAdapter.submitList(processList(movies.sortedByDescending { it.id }.take(10)))','trendingAdapter.submitList(processList(movies.sortedByDescending { it.id }.take(10))) { focusMemory.restore() }')

p=j+'Fragments/MovieDetailFragment.kt'
imports(p,['com.example.kelkin.utils.*','kotlinx.coroutines.CancellationException'])
edit(p,'if (lastPos > 10000) viewModel.updateContinueWatching(movie, lastPos, totalDur)','viewModel.loadContinueList()')
edit(p,'viewModel.isInMyList(movie.tmdb_id)','viewModel.isInMyList(movie)')
start=read(p).index('            val savedProgress =')
end=read(p).index('            btnPlay.setOnClickListener',start)
text=read(p)
text=text[:start]+'''            viewModel.continueWatchingList.observe(viewLifecycleOwner) { history ->
                val saved = history.orEmpty().find { it.identity() == movie.identity() }
                btnContinue.visibility = if (saved != null && saved.lastPosition >= 2000) View.VISIBLE else View.GONE
                btnContinue.text = "ادامه پخش از دقیقه ${(saved?.lastPosition ?: 0) / 60000}"
                btnContinue.setOnClickListener {
                    val position = viewModel.continueWatchingList.value.orEmpty().find { it.identity() == movie.identity() }?.lastPosition ?: 0L
                    launchPlayer(movie, position)
                }
            }

'''+text[end:]
write(p,text)
method(p,'fetchFullDetails','''    private fun fetchFullDetails(tmdbId: String) {
        if (tmdbId.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val (details, credits) = viewModel.getMovieFullDetails(tmdbId)
            if (details != null) updateUI(details, credits ?: com.example.kelkin.DataClass.MovieCreditsResponse(details.id, emptyList(), emptyList()))
        }
    }''')
edit(p,'"https://image.tmdb.org/t/p/original${details.backdrop_path}"','ImageUrls.backdrop(details.backdrop_path)')
edit(p,'        playerLauncher.launch(intent)','''        if (movie.videoUrl1.isBlank()) {
            android.widget.Toast.makeText(context, "لینک پخش موجود نیست", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        playerLauncher.launch(intent)''')
edit(p,'        (activity as? MainActivity)?.collapseSidebar()', '        (activity as? MainActivity)?.collapseSidebar()')
# Replace delayed focus stealing with saved control focus.
text=read(p)
text=re.sub(r'            btnPlay.postDelayed\(\{.*?            \}, 400\)', '            RecyclerFocusMemory(this, btnContinue.takeIf { it.visibility == View.VISIBLE } ?: btnPlay)',text,flags=re.S)
write(p,text)

p=j+'Fragments/SeriesDetailsFragment.kt'
imports(p,['com.example.kelkin.utils.*','androidx.navigation.fragment.findNavController'])
edit(p,'    private var currentBackdropPath:', '''    private var selectedSeasonKey = ""
    private lateinit var focusMemory: RecyclerFocusMemory
    private var currentBackdropPath:''',1)
edit(p,'        selectedSeries?.let { series ->','''        selectedSeasonKey = findNavController().currentBackStackEntry!!.savedStateHandle.get<String>("selected_season").orEmpty()
        focusMemory = RecyclerFocusMemory(this, binding.btnBookmarkSeries)
        selectedSeries?.let { series ->''',1)
edit(p,'val dummyMovieForBookmark = Movie().apply {','val dummyMovieForBookmark = series.asCard().apply {')
edit(p,'viewModel.isInMyList(series.tmdb_id)','viewModel.isInMyList(dummyMovieForBookmark)')
edit(p,'        val seasonKeys = series.seasons.keys.sorted()','        val seasonKeys = series.seasons.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }')
edit(p,'        var selectedSeasonKey = seasonKeys.first()','        if (selectedSeasonKey !in seasonKeys) selectedSeasonKey = seasonKeys.first()')
# Remove parent edge locks; allow Android's spatial focus to leave the row.
text=read(p)
start=text.index('        binding.rvEpisodes.setOnKeyListener')
end=text.index('        fun updateEpisodes',start)
text=text[:start]+text[end:]
write(p,text)
edit(p,'            binding.rvEpisodes.scrollToPosition(0)','            if (binding.rvEpisodes.hasFocus()) binding.rvEpisodes.scrollToPosition(0)')
edit(p,'                    selectedSeasonKey = seasonKey\n                    updateEpisodes(seasonKey)','''                    selectedSeasonKey = seasonKey
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("selected_season", seasonKey)
                    updateEpisodes(seasonKey)
                    for (i in 1 until container.childCount) container.getChildAt(i).isSelected = container.getChildAt(i).tag == seasonKey''')
edit(p,'                text = "فصل ${seasonKey.toIntOrNull() ?: seasonKey}"','''                text = "فصل ${seasonKey.toIntOrNull() ?: seasonKey}"
                tag = seasonKey
                isSelected = seasonKey == selectedSeasonKey''')
text=read(p)
text=re.sub(r'        firstButton\?\.postDelayed\(\{.*?        \}, 400\)','',text,flags=re.S)
write(p,text)
edit(p,'        updateEpisodes(selectedSeasonKey)\n    }','''        updateEpisodes(selectedSeasonKey)
        focusMemory.track(binding.rvEpisodes)
        viewModel.continueWatchingList.observe(viewLifecycleOwner) { history ->
            val saved = history.orEmpty().find { it.identity() == series.identity() && it.episodeKey.isNotBlank() }
            binding.btnContinueSeries.visibility = if (saved != null) View.VISIBLE else View.GONE
            binding.btnContinueSeries.text = "ادامه فصل ${saved?.seasonKey?.toIntOrNull() ?: ""}، قسمت ${saved?.episodeKey?.removePrefix("ep")?.toIntOrNull() ?: ""}"
            binding.btnContinueSeries.setOnClickListener {
                val latest = viewModel.continueWatchingList.value.orEmpty().find { it.identity() == series.identity() } ?: return@setOnClickListener
                val url = series.seasons[latest.seasonKey]?.get(latest.episodeKey)
                if (url.isNullOrBlank()) {
                    Toast.makeText(context, "این قسمت دیگر در آرشیو موجود نیست", Toast.LENGTH_SHORT).show()
                } else {
                    selectedSeasonKey = latest.seasonKey
                    launchPlayer(series, url, latest.episodeKey, latest.lastPosition)
                }
            }
        }
    }''')
method(p,'launchPlayer','''    private fun launchPlayer(series: Series, videoUrl: String, episodeTitle: String, startPos: Long = 0L) {
        if (videoUrl.isBlank()) { Toast.makeText(context, "لینک پخش موجود نیست", Toast.LENGTH_SHORT).show(); return }
        val card = series.asCard().copy(videoUrl1 = videoUrl, seasonKey = selectedSeasonKey, episodeKey = episodeTitle)
        val intent = android.content.Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
            putExtra("video_url", videoUrl)
            putExtra("movie_json", Gson().toJson(card))
            putExtra("start_pos", startPos)
        }
        startActivity(intent)
    }''')
edit(p,'        lifecycleScope.launch {','        viewLifecycleOwner.lifecycleScope.launch {')
edit(p,'                if (tvDetails != null && isAdded)', '                if (tvDetails != null && _binding != null)')
edit(p,'"https://image.tmdb.org/t/p/original${details.backdrop_path}"','ImageUrls.backdrop(details.backdrop_path)')
edit(p,'            selectedSeries?.let { setupSeasonsButtonsAndEpisodes(it) }','            (binding.rvEpisodes.adapter as? EpisodesAdapter)?.updateBackdrop(currentBackdropPath)')
edit(p,'    override fun onDestroyView()', '''    override fun onResume() {
        super.onResume()
        viewModel.loadContinueList()
    }

    override fun onDestroyView()''')
# Resume button precedes seasons without changing their child indices.
p='app/src/main/res/layout/fragment_series_details.xml'
edit(p,'        <HorizontalScrollView','''        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnContinueSeries"
            android:layout_width="wrap_content"
            android:layout_height="48dp"
            android:text="ادامه تماشا"
            android:visibility="gone"
            android:background="@drawable/sl_button_high_contrast"
            android:textColor="@drawable/sl_text_high_contrast"
            app:backgroundTint="@null" />

        <HorizontalScrollView''')
p=j+'Adapter/EpisodesAdapter.kt'
edit(p,'    private val backdropPath:', '    private var backdropPath:')
edit(p,'    override fun getItemCount(): Int = episodes.size','''    fun updateBackdrop(path: String) { backdropPath = path; notifyItemRangeChanged(0, itemCount, "backdrop") }
    override fun getItemCount(): Int = episodes.size''')
