package com.example.kelkin.Fragments

import android.content.Intent
import android.os.Bundle
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
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.MainActivity
import com.example.kelkin.PlayerActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentSearchBinding
import com.example.kelkin.utils.*
import com.google.gson.Gson
import kotlinx.coroutines.*

class SearchFragment : Fragment(R.layout.fragment_search) {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var focusMemory: RecyclerFocusMemory
    private var searchJob: Job? = null
    private var results = emptyList<Movie>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSearchBinding.bind(view)
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        movieAdapter = MovieAdapter { item ->
            val args = Bundle()
            when (item.contentType) {
                "series" -> {
                    val series = viewModel.seriesList.value.orEmpty().find { it.identity() == item.identity() }
                    if (series != null) {
                        args.putSerializable("selected_series", series)
                        findNavController().navigate(R.id.action_searchFragment_to_seriesDetailsFragment, args)
                    }
                }
                "channel" -> {
                    val channel = viewModel.channels.value.orEmpty().find { it.id == item.id }
                    if (channel != null) startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                        putExtra("channel_json", Gson().toJson(channel)); putExtra("video_url", channel.videoUrl)
                        putExtra("is_live", true); putExtra("selected_category_id", -1L)
                    })
                }
                else -> {
                    args.putSerializable("selected_movie", item)
                    findNavController().navigate(R.id.action_searchFragment_to_movieDetailFragment, args)
                }
            }
        }
        movieAdapter.showFullPoster = true
        movieAdapter.fillGridCell = !com.example.kelkin.BuildConfig.IS_TV
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvResults.adapter = movieAdapter
        binding.edtSearch.showSoftInputOnFocus = !com.example.kelkin.BuildConfig.IS_TV
        if (com.example.kelkin.BuildConfig.IS_TV) setupKeyboard()
        else {
            binding.gridKeyboard.visibility = View.GONE
            binding.rvResults.configureCatalogGrid()
            binding.edtSearch.setOnEditorActionListener { _, action, _ ->
                if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                        .hideSoftInputFromWindow(binding.edtSearch.windowToken, 0)
                    binding.edtSearch.clearFocus()
                    true
                } else false
            }
        }
        focusMemory = RecyclerFocusMemory(this, binding.edtSearch)
        focusMemory.track(binding.rvResults)
        binding.edtSearch.setText(findNavController().currentBackStackEntry!!.savedStateHandle.get<String>("search_query").orEmpty())
        binding.edtSearch.addTextChangedListener { search(it.toString()) }
        viewModel.moviesList.observe(viewLifecycleOwner) { search(binding.edtSearch.text.toString()) }
        viewModel.seriesList.observe(viewLifecycleOwner) { search(binding.edtSearch.text.toString()) }
        viewModel.channels.observe(viewLifecycleOwner) { search(binding.edtSearch.text.toString()) }
        viewModel.movieDetailsMap.observe(viewLifecycleOwner) { renderResults() }
        viewModel.syncError.observe(viewLifecycleOwner) { renderResults() }
        search(binding.edtSearch.text.toString())
    }
    private fun normalize(text: String) = text.replace('ي', 'ی').replace('ك', 'ک').trim().lowercase().replace(Regex("\\s+"), " ")
    private fun search(query: String) {
        findNavController().currentBackStackEntry?.savedStateHandle?.set("search_query", query)
        searchJob?.cancel()
        val normalized = normalize(query)
        if (normalized.isBlank()) { results = emptyList(); renderResults(); return }
        binding.txtResultTitle.text = "در حال جستجو…"
        val movies = viewModel.moviesList.value.orEmpty().toList()
        val series = viewModel.seriesList.value.orEmpty().map { it.asCard() }
        val channels = viewModel.channels.value.orEmpty().map {
            Movie(id = it.id.toLong(), name_fa = it.name_fa, posterUrl = it.logoUrl, videoUrl1 = it.videoUrl, contentType = "channel")
        }
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(250)
            results = withContext(Dispatchers.Default) {
                (movies + series + channels).filter {
                    ensureActive()
                    normalize(it.name_fa + " " + it.description_fa).contains(normalized)
                }.sortedBy { !normalize(it.name_fa).startsWith(normalized) }
            }
            renderResults()
            results.take(20).forEach {
                if (it.contentType == "series") viewModel.fetchSeriesTmdbDetails(it.tmdb_id)
                else if (it.contentType == "movie") viewModel.fetchTmdbDetails(it.tmdb_id)
            }
        }
    }
    private fun renderResults() {
        if (_binding == null) return
        val details = viewModel.movieDetailsMap.value.orEmpty()
        val display = results.map { item ->
            val metadata = details[metadataKey(item.contentType, item.tmdb_id)]
            item.copy(posterUrl = ImageUrls.poster(metadata?.poster_path ?: item.posterUrl).orEmpty(), voteAverage = metadata?.vote_average ?: item.voteAverage)
        }
        movieAdapter.submitList(display) {
            val ui = _binding ?: return@submitList
            focusMemory.restore()
            // When results appear, reveal the whole poster row rather than its top edge.
            if (display.isNotEmpty() && com.example.kelkin.BuildConfig.IS_TV) ui.rvResults.post {
                if (_binding === ui) {
                    val row = android.graphics.Rect()
                    ui.rvResults.getDrawingRect(row)
                    val scroll = ui.searchScrollView as? androidx.core.widget.NestedScrollView ?: return@post
                    scroll.offsetDescendantRectToMyCoords(ui.rvResults, row)
                    val bottom = scroll.scrollY + scroll.height - scroll.paddingBottom
                    if (row.bottom > bottom) scroll.smoothScrollTo(0,
                        (row.bottom - scroll.height + scroll.paddingBottom).coerceAtLeast(0))
                }
            }
        }
        binding.rvResults.visibility = if (display.isEmpty()) View.GONE else View.VISIBLE
        binding.txtResultTitle.text = when {
            binding.edtSearch.text.isNullOrBlank() -> "نام فیلم، سریال یا کانال را بنویسید"
            display.isEmpty() -> viewModel.syncError.value ?: "نتیجه‌ای پیدا نشد؛ عبارت دیگری بنویسید."
            else -> "${display.size} نتیجه"
        }
    }
    private fun setupKeyboard() {
        val grid = binding.gridKeyboard
        grid.removeAllViews()
        val letters = "ا ب پ ت ث ج چ ح خ د ذ ر ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ه ی"
        val keys = letters.split(" ") + (0..9).map { it.toString() } + listOf("فاصله", "⌫", "پاک‌کردن")
        keys.forEach { key ->
            val button = Button(requireContext()).apply {
                text = when (key) {
                    "پاک‌کردن" -> "پاک"
                    else -> key
                }
                contentDescription = when (key) {
                    "⌫" -> "حذف آخرین حرف"
                    "پاک‌کردن" -> "پاک کردن جستجو"
                    else -> key
                }
                textSize = if (key.length > 2) 11f else 16f
                includeFontPadding = false
                isFocusable = true
                minWidth = 0; minHeight = 0
                setPadding(0, 0, 0, 0)
                backgroundTintList = null
                setBackgroundResource(R.drawable.selector_keyboard_key)
                setTextColor(androidx.core.content.ContextCompat.getColorStateList(context, R.color.sl_text_high_contrast))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = (40 * resources.displayMetrics.density).toInt()
                    height = (36 * resources.displayMetrics.density).toInt()
                    val margin = (2 * resources.displayMetrics.density).toInt()
                    setMargins(margin, margin, margin, margin)
                }
                setOnClickListener {
                    when (key) {
                        "فاصله" -> binding.edtSearch.append(" ")
                        "⌫" -> binding.edtSearch.setText(binding.edtSearch.text.toString().dropLast(1))
                        "پاک‌کردن" -> binding.edtSearch.text.clear()
                        else -> binding.edtSearch.append(key)
                    }
                }
            }
            grid.addView(button)
        }
    }
    override fun onResume() { super.onResume(); (activity as? MainActivity)?.collapseSidebar() }
    override fun onDestroyView() { searchJob?.cancel(); _binding = null; super.onDestroyView() }
}
