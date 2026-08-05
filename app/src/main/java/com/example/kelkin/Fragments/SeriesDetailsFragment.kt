package com.example.kelkin.Fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.kelkin.Adapter.EpisodesAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.Series
import com.example.kelkin.DataClass.TmdbTvDetails
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentSeriesDetailsBinding
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Locale

class SeriesDetailsFragment : Fragment(R.layout.fragment_series_details) {

    private var selectedSeries: Series? = null
    private lateinit var viewModel: HomeViewModel
    private var _binding: FragmentSeriesDetailsBinding? = null

    private var currentBackdropPath: String = ""
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSeriesDetailsBinding.bind(view)

        (activity as? MainActivity)?.collapseSidebar()

        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        selectedSeries = arguments?.getSerializable("selected_series") as? Series
            ?: arguments?.getSerializable("series_data") as? Series

        selectedSeries?.let { series ->
            binding.txtTitle.text = series.name_fa
            binding.txtDescription.text = series.description_fa

            setupBookmarkButton(series)
            setupSeasonsButtonsAndEpisodes(series)

            if (!series.tmdb_id.isNullOrEmpty()) {
                fetchSeriesTmdbDetails(series.tmdb_id)
            }
        } ?: Log.e("SeriesDebug", "خطا: شیء selectedSeries در آرگومان‌ها نال است!")
    }

    private fun setupBookmarkButton(series: Series) {
        val dummyMovieForBookmark = Movie().apply {
            id = series.id
            name_fa = series.name_fa
            description_fa = series.description_fa
            category = series.category
            tmdb_id = series.tmdb_id
            posterUrl = series.posterUrl
        }

        fun updateBookmarkIcon(isBookmarked: Boolean) {
            val iconRes = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
            binding.btnBookmarkSeries.setIconResource(iconRes)
        }

        val isInitialBookmarked = viewModel.isInMyList(series.tmdb_id)
        updateBookmarkIcon(isInitialBookmarked)

        binding.btnBookmarkSeries.setOnClickListener {
            val currentlyBookmarked = viewModel.isInMyList(series.tmdb_id)
            if (currentlyBookmarked) {
                viewModel.removeFromMyList(dummyMovieForBookmark)
                updateBookmarkIcon(false)
                Toast.makeText(requireContext(), "از نشان‌شده‌ها حذف شد", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addToMyList(dummyMovieForBookmark)
                updateBookmarkIcon(true)
                Toast.makeText(requireContext(), "به نشان‌شده‌ها اضافه شد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("ResourceType")
    private fun setupSeasonsButtonsAndEpisodes(series: Series) {
        val seasonKeys = series.seasons.keys.sorted()
        if (seasonKeys.isEmpty()) return

        var selectedSeasonKey = seasonKeys.first()

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvEpisodes.layoutManager = layoutManager

        binding.rvEpisodes.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val layoutManager = binding.rvEpisodes.layoutManager as LinearLayoutManager
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val itemCount = binding.rvEpisodes.adapter?.itemCount ?: 0

                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT && firstVisible <= 0) {
                    return@setOnKeyListener true
                }

                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && lastVisible >= itemCount - 1) {
                    return@setOnKeyListener true
                }
            }
            false
        }

        fun updateEpisodes(seasonKey: String) {
            val episodesMap = series.seasons[seasonKey] ?: emptyMap()

            // مرتب‌سازی صعودی دقیق از قسمت ۱ به بعد
            val episodeList = episodesMap.entries.sortedBy { it.key.replace("ep", "").toIntOrNull() ?: 0 }

            val adapter = EpisodesAdapter(episodeList, currentBackdropPath) { episodeTitle, videoUrl ->
                launchPlayer(series, videoUrl, episodeTitle)
            }
            binding.rvEpisodes.adapter = adapter

            // تضمین شروع از اولین کارت (قسمت اول)
            binding.rvEpisodes.scrollToPosition(0)
        }

        val container = binding.layoutSeasonButtonsContainer
        if (container.childCount > 1) {
            container.removeViews(1, container.childCount - 1)
        }

        var firstButton: MaterialButton? = null

        seasonKeys.forEachIndexed { index, seasonKey ->
            val seasonButton = MaterialButton(requireContext()).apply {
                text = "فصل ${seasonKey.toIntOrNull() ?: seasonKey}"
                typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.yekanbakh_bold)
                textSize = 16f
                setTextColor(ContextCompat.getColorStateList(context, R.drawable.sl_text_high_contrast))
                setBackgroundResource(R.drawable.sl_button_high_contrast)
                backgroundTintList = null
                cornerRadius = 6.dpToPx(requireContext())

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    44.dpToPx(requireContext())
                ).apply {
                    marginEnd = 8.dpToPx(requireContext())
                }
                layoutParams = params

                setOnClickListener {
                    selectedSeasonKey = seasonKey
                    updateEpisodes(seasonKey)
                }
            }

            if (index == 0) {
                firstButton = seasonButton
            }

            container.addView(seasonButton)
        }

        firstButton?.postDelayed({
            if (isAdded && view != null) {
                firstButton?.requestFocus()
            }
        }, 400)

        updateEpisodes(selectedSeasonKey)
    }

    private fun launchPlayer(series: Series, videoUrl: String, episodeTitle: String) {
        val intent = android.content.Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
            putExtra("video_url", videoUrl)
            putExtra("movie_tmdb_id", series.tmdb_id)
            putExtra("movie_json", Gson().toJson(series))
            putExtra("start_pos", 0L)
        }
        startActivity(intent)
    }

    private fun fetchSeriesTmdbDetails(tmdbId: String) {
        lifecycleScope.launch {
            try {
                val tvDetails = viewModel.fetchTvDetailsForPoster(tmdbId)
                if (tvDetails != null && isAdded) {
                    activity?.runOnUiThread {
                        updateUI(tvDetails)
                    }
                }
            } catch (e: Exception) {
                Log.e("SeriesDebug", "Error fetching series details: ${e.message}")
            }
        }
    }

    private fun updateUI(details: TmdbTvDetails) {
        binding.txtTitleEnglish.text = details.name

        if (!details.backdrop_path.isNullOrEmpty()) {
            currentBackdropPath = details.backdrop_path

            Glide.with(this)
                .load("https://image.tmdb.org/t/p/original${details.backdrop_path}")
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .into(binding.imgBackdrop)

            selectedSeries?.let { setupSeasonsButtonsAndEpisodes(it) }
        }

        val ratingStr = String.format(Locale.ENGLISH, "%.1f", details.vote_average)
        val genresStr = details.genres.joinToString("، ") { it.name }
        val yearStr = details.first_air_date?.take(4) ?: ""

        val fullText = "IMDB $ratingStr  |  $genresStr  |  $yearStr"
        val spannable = SpannableString(fullText)
        val imdbPart = "IMDB $ratingStr"
        val startIndex = fullText.indexOf(imdbPart)

        if (startIndex != -1) {
            val endIndex = startIndex + imdbPart.length
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FFD700")), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        binding.txtInfo.text = spannable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}