from rewrite_support import *
j='app/src/main/java/com/example/kelkin/'
for name,adapter,rv,button,loading,source in [('MoviesFragment','movieAdapter','rvMoviesAll','btnGenreFilter','progressBarMovies','moviesList'),('SeriesFragment','seriesAdapter','rvSeriesAll','btnSeriesGenreFilter','progressBarSeries','seriesList'),('TvFragment','channelAdapter','rvChannels','btnTvCategory','progressBar','channels')]:
    p=j+'Fragments/'+name+'.kt'
    imports(p,['com.example.kelkin.utils.*'])
    edit(p,'    private var isFirstFocusSet = false','    private lateinit var focusMemory: RecyclerFocusMemory\n    private lateinit var statusView: CatalogStatusView')
    edit(p,'        setupFilterButtons()','''        focusMemory = RecyclerFocusMemory(this, binding.BUTTON)
        focusMemory.track(binding.RV)
        statusView = CatalogStatusView(binding.root) { viewModel.refresh() }
        viewModel.syncError.observe(viewLifecycleOwner) { applyFilters() }
        viewModel.catalogLoading.observe(viewLifecycleOwner) { applyFilters() }
        setupFilterButtons()'''.replace('BUTTON',button).replace('RV',rv))
    edit(p,'        '+adapter+'.submitList(', '        '+adapter+'.submitList(',1)
    text=read(p)
    text=re.sub(r'        '+adapter+r'\.submitList\((\w+)\)',r'''        ADAPTER.submitList(\1) { focusMemory.restore() }
        val empty = \1.isEmpty()
        binding.RV.visibility = if (empty) View.GONE else View.VISIBLE
        statusView.show(empty, viewModel.catalogLoading.value == true, viewModel.syncError.value)
        binding.LOADING.visibility = if (empty && viewModel.catalogLoading.value == true) View.VISIBLE else View.GONE'''.replace('ADAPTER',adapter).replace('RV',rv).replace('LOADING',loading),text)
    text=text.replace('GridLayoutManager(requireContext(), 5)','GridLayoutManager(requireContext(), (resources.configuration.screenWidthDp / 180).coerceIn(3, 6))')
    text=re.sub(r'        binding\.'+rv+r'\.addItemDecoration\([^\n]+\) [^\n]*', '        binding.'+rv+'.addItemDecoration(GridSpacingItemDecoration((binding.'+rv+'.layoutManager as GridLayoutManager).spanCount, (12 * resources.displayMetrics.density).toInt(), true))',text)
    text=text.replace('detailsMap[movie.tmdb_id]','detailsMap[metadataKey("movie", movie.tmdb_id)]').replace('detailsMap[seriesItem.tmdb_id]','detailsMap[metadataKey("series", seriesItem.tmdb_id)]').replace('detailsMap.containsKey(seriesItem.tmdb_id)','detailsMap.containsKey(metadataKey("series", seriesItem.tmdb_id))')
    text=text.replace('listOf("تاریخ ساخت", "عنوان (الفبا)")','listOf("تازه اضافه‌شده", "عنوان (الفبا)")')
    text=text.replace('                tryAutoFocus()','').replace('                if (channels.isNotEmpty() && !isFirstFocusSet) setFirstFocus()','')
    # Do not block sorting when categories have not loaded.
    text=re.sub(r'        android\.util\.Log\.d\("CategoryDebug".*?        val sortOptions', '        val sortOptions', text, flags=re.S)
    write(p,text)
    method(p,'setFirstFocus' if name=='TvFragment' else 'tryAutoFocus','    private fun restoreFocus() { focusMemory.restore() }')
    # Persist filters in this navigation entry as well as the focus anchor.
    imports(p,['androidx.navigation.fragment.findNavController'])
    state='''        val screenState = findNavController().currentBackStackEntry!!.savedStateHandle
        currentCategory = screenState.get<CATEGORYTYPE>("category") ?: ZERO
        currentSort = screenState.get<String>("sort") ?: SORT
'''.replace('CATEGORYTYPE','Int' if name=='SeriesFragment' else 'Long').replace('ZERO','0' if name=='SeriesFragment' else '0L').replace('SORT','"default"' if name=='TvFragment' else '"date"')
    edit(p,'        setupRecyclerView()',state+'        setupRecyclerView()',1)
    edit(p,'    private fun applyFilters() {','''    private fun applyFilters() {
        if (!::statusView.isInitialized) return
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("category", currentCategory); set("sort", currentSort)
        }''')
    if name=='MoviesFragment':
        edit(p,'        val finalDisplayList = filteredList.map { movie ->','''        filteredList.take(30).forEach { viewModel.fetchTmdbDetails(it.tmdb_id) }
        val finalDisplayList = filteredList.map { movie ->''')
    if name=='SeriesFragment':
        edit(p,'            if (seriesItem.tmdb_id.isNotEmpty()', '            if (filteredList.indexOf(seriesItem) < 30 && seriesItem.tmdb_id.isNotEmpty()')
    # Refresh displayed filter labels from persisted selections after the lookup list arrives.
    if name=='MoviesFragment':
        edit(p,'            this.categoryList = categories ?: emptyList()','''            this.categoryList = categories ?: emptyList()
            binding.btnGenreFilter.text = categoryList.find { it.id.toLong() == currentCategory }?.name ?: "همه ژانرها"
            binding.btnSortOrder.text = if (currentSort == "date") "تازه اضافه‌شده" else "عنوان (الفبا)"''')
        edit(p,'        binding.btnGenreFilter.setOnClickListener', '        binding.btnTypeFilter.visibility = View.GONE\n        binding.btnGenreFilter.setOnClickListener',1)
    elif name=='SeriesFragment':
        edit(p,'            this.seriesCategoryList = categories ?: emptyList()','''            this.seriesCategoryList = categories ?: emptyList()
            binding.btnSeriesGenreFilter.text = seriesCategoryList.find { it.id == currentCategory }?.name ?: "همه دسته‌ها"
            binding.btnSeriesSortOrder.text = if (currentSort == "date") "تازه اضافه‌شده" else "عنوان (الفبا)"''')
    else:
        edit(p,'            this.tvCategoryList = categories ?: emptyList()','''            this.tvCategoryList = categories ?: emptyList()
            binding.btnTvCategory.text = tvCategoryList.find { it.id.toLong() == currentCategory }?.name ?: "همه شبکه‌ها"
            binding.btnTvSort.text = if (currentSort == "default") "لیست پیش‌فرض" else "ترتیب الفبا"''')
        edit(p,'        binding.btnTvFavorites.setOnClickListener {  }','''        binding.btnTvFavorites.text = if (favoritesOnly) "همه کانال‌ها" else "علاقه‌مندی‌ها"
        binding.btnTvFavorites.setOnClickListener {
            favoritesOnly = !favoritesOnly
            binding.btnTvFavorites.text = if (favoritesOnly) "همه کانال‌ها" else "علاقه‌مندی‌ها"
            applyFilters()
        }''')
        edit(p,'    private var currentCategory:', '    private var favoritesOnly = false\n    private var currentCategory:',1)
        edit(p,'        val persianCollator =', '''        if (favoritesOnly) {
            val favorites = requireContext().getSharedPreferences("kelkin_prefs", 0).getStringSet("favorite_channels", emptySet()).orEmpty()
            filteredList = filteredList.filter { it.id.toString() in favorites }
        }
        val persianCollator =''')
        edit(p,'        super.onResume()','        super.onResume()\n        if (::channelAdapter.isInitialized) applyFilters()')

# Global focus changes caused by initial inflation must not overwrite a pending saved anchor.
edit(j+'utils/RecyclerFocusMemory.kt','if (!restoring && focused != null)', 'if (!restoring && !pending && focused != null)')

for p in ['app/src/main/res/layout/fragment_home.xml','app/src/main/res/layout/fragment_search.xml']:
    text=read(p)
    text=re.sub(r'\s*android:nextFocusLeft="@id/rv_[^"]+"','',text)
    text=text.replace('android:clipToPadding="false"','android:clipToPadding="false"\n                android:clipChildren="false"')
    write(p,text)
