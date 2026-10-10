from rewrite_support import *
import json
j='app/src/main/java/com/example/kelkin/'
p='firebase/database.rules.json'
rules=json.loads(read(p))
admin="auth != null && root.child('admins').child(auth.uid).val() === true"
rules['rules']['catalog_counters']={'.read':admin,'.write':admin,'$type':{'.validate':'newData.isNumber() && newData.val() > 0'}}
write(p,json.dumps(rules,indent=2)+'\n')
p=j+'utils/CatalogRepository.kt'
edit(p,'    fun refresh() {\n        syncError.value = null','''    fun refresh() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        listeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        listeners.clear()
        syncError.value = null''')
# For new atomic IDs channels need a Long rather than an overflowing 32-bit Int.
edit(j+'DataClass/Models.kt','val id: Int = 0,\n    val name_fa:', 'val id: Long = 0,\n    val name_fa:')
edit(p,'?.toIntOrNull() ?: 0,','?.toLongOrNull() ?: 0L,')
edit(p,'id = c.getInt(c.getColumnIndexOrThrow("id"))','id = c.getLong(c.getColumnIndexOrThrow("id"))')
edit(j+'Fragments/SearchFragment.kt','it.id == item.id.toInt()', 'it.id == item.id')
edit(j+'utils/WatchHistory.kt','        val sameEpisode = list.firstOrNull { it.identity() == item.identity() }\n','')
# Channel DiffUtil and focus now remain stable for large identifiers.
p=j+'Adapter/MovieGridAdapter.kt'
edit(p,'    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {','    var onVisible: ((Movie) -> Unit)? = null\n    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {')
edit(p,'        val movie = getItem(position)','        val movie = getItem(position)\n        onVisible?.invoke(movie)',1)
p=j+'Adapter/SeriesGridAdapter.kt'
edit(p,'    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {','    var onVisible: ((Series) -> Unit)? = null\n    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {')
edit(p,'        val series = getItem(position)','        val series = getItem(position)\n        onVisible?.invoke(series)',1)
edit(j+'Fragments/MoviesFragment.kt','        binding.rvMoviesAll.adapter = movieAdapter','        movieAdapter.onVisible = { viewModel.fetchTmdbDetails(it.tmdb_id) }\n        binding.rvMoviesAll.adapter = movieAdapter')
edit(j+'Fragments/SeriesFragment.kt','        binding.rvSeriesAll.adapter = seriesAdapter','        seriesAdapter.onVisible = { viewModel.fetchSeriesTmdbDetails(it.tmdb_id) }\n        binding.rvSeriesAll.adapter = seriesAdapter')
p='app/src/main/res/values/strings.xml'
text=read(p)
for key in ['format_episode_item','format_season_title','msg_episode_added_success']:
    pattern=r'(<string name="'+key+r'">)(.*?)(</string>)'
    def positional(m):
        count=0
        def fmt(f):
            nonlocal count
            count+=1
            return '%'+str(count)+'$'+f.group(1)
        return m.group(1)+re.sub(r'%([sd])',fmt,m.group(2))+m.group(3)
    text=re.sub(pattern,positional,text)
text=text.replace('<string name="sort_release_date">تاریخ ساخت</string>','<string name="sort_release_date">تازه اضافه‌شده</string>')
write(p,text)
