package com.example.kelkin.Fragments

import com.example.kelkin.utils.*
import androidx.navigation.fragment.findNavController

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import com.example.kelkin.`object`.TranslationHelper
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.kelkin.Adapter.EpisodesAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.DataClass.Series
import com.example.kelkin.DataClass.TmdbTvDetails
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentSeriesDetailsBinding
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Locale

class SeriesDetailsFragment : Fragment(R.layout.fragment_series_details) {

    private var selectedSeries: Series? = null
    private lateinit var viewModel: HomeViewModel
    private var _binding: FragmentSeriesDetailsBinding? = null

    private var selectedSeasonKey = ""
    private lateinit var focusMemory: RecyclerFocusMemory
    private var currentBackdropPath: String = ""
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSeriesDetailsBinding.bind(view)

        (activity as? MainActivity)?.collapseSidebar()
        view.animateDetailEntrance()

        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        selectedSeries = arguments?.getSerializable("selected_series") as? Series
            ?: arguments?.getSerializable("series_data") as? Series

        selectedSeasonKey = findNavController().currentBackStackEntry!!.savedStateHandle.get<String>("selected_season").orEmpty()
        focusMemory = RecyclerFocusMemory(this, binding.btnBookmarkSeries)
        selectedSeries?.let { series ->
            binding.txtTitle.text = series.name_fa
            binding.txtDescription.text = series.description_fa
            view.hideEmptyDetailTags()
            binding.txtRating.text = if (series.voteAverage > 0) "امتیاز %.1f".format(Locale.ENGLISH, series.voteAverage).toPersianDigits() else ""
            viewModel.languagesList.observe(viewLifecycleOwner) { view.bindLocalDetailTags(series.asCard(), it.orEmpty(), viewModel.qualitiesList.value.orEmpty()) }
            viewModel.qualitiesList.observe(viewLifecycleOwner) { view.bindLocalDetailTags(series.asCard(), viewModel.languagesList.value.orEmpty(), it.orEmpty()) }

            setupBookmarkButton(series)
            setupSeasonsButtonsAndEpisodes(series)

            if (!series.tmdb_id.isNullOrEmpty()) {
                fetchSeriesTmdbDetails(series.tmdb_id)

            }
        } ?: Log.e("SeriesDebug", "خطا: شیء selectedSeries در آرگومان‌ها نال است!")
    }

    private fun setupBookmarkButton(series: Series) {
        val dummyMovieForBookmark = series.asCard().apply {
            id = series.id
            name_fa = series.name_fa
            description_fa = series.description_fa
            category = series.category
            tmdb_id = series.tmdb_id
            posterUrl = series.posterUrl
        }

        fun updateBookmarkIcon(isBookmarked: Boolean) {
            val iconRes = if (com.example.kelkin.BuildConfig.IS_TV) {
                if (isBookmarked) R.drawable.ic_detail_added else R.drawable.ic_detail_add
            } else if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
            binding.btnBookmarkSeries.setIconResource(iconRes)
        }

        val isInitialBookmarked = viewModel.isInMyList(dummyMovieForBookmark)
        updateBookmarkIcon(isInitialBookmarked)

        binding.btnBookmarkSeries.setOnClickListener {
            val currentlyBookmarked = viewModel.isInMyList(dummyMovieForBookmark)
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
        val seasonKeys = series.seasons.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
        if (seasonKeys.isEmpty()) {
            binding.episodeSectionTitle.text = "هنوز قسمتی برای این سریال ثبت نشده است."
            return
        }

        if (selectedSeasonKey !in seasonKeys) selectedSeasonKey = seasonKeys.first()

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvEpisodes.layoutManager = layoutManager

        fun updateEpisodes(seasonKey: String) {
            val episodesMap = series.seasons[seasonKey] ?: emptyMap()
            binding.episodeSectionTitle.text = if (episodesMap.isEmpty()) "این فصل هنوز قسمتی ندارد." else getString(R.string.title_episodes)

            // مرتب‌سازی صعودی دقیق از قسمت ۱ به بعد
            val episodeList = episodesMap.entries.sortedBy { it.key.replace("ep", "").toIntOrNull() ?: 0 }

            val adapter = EpisodesAdapter(episodeList, currentBackdropPath) { episodeTitle, videoUrl ->
                launchPlayer(series, videoUrl, episodeTitle)
            }
            binding.rvEpisodes.adapter = adapter

            // تضمین شروع از اولین کارت (قسمت اول)
            if (binding.rvEpisodes.hasFocus()) binding.rvEpisodes.scrollToPosition(0)
        }

        val container = binding.layoutSeasonButtonsContainer
        // Keep the static actions in mobile layouts; remove only generated season buttons.
        for (index in container.childCount - 1 downTo 0) {
            if (container.getChildAt(index).tag != null) container.removeViewAt(index)
        }

        var firstButton: MaterialButton? = null

        seasonKeys.forEachIndexed { index, seasonKey ->
            val seasonButton = MaterialButton(requireContext()).apply {
                text = "فصل ${seasonKey.toIntOrNull() ?: seasonKey}"
                tag = seasonKey
                isSelected = seasonKey == selectedSeasonKey
                typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.yekanbakh_bold)
                textSize = if (com.example.kelkin.BuildConfig.IS_TV) 14f else 16f
                setTextColor(ContextCompat.getColorStateList(context, R.color.sl_text_high_contrast))
                backgroundTintList = null
                setBackgroundResource(R.drawable.sl_button_high_contrast)
                backgroundTintList = null
                cornerRadius = 6.dpToPx(requireContext())
                if (com.example.kelkin.BuildConfig.IS_TV) {
                    insetTop = 0
                    insetBottom = 0
                    minimumHeight = 0
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (if (com.example.kelkin.BuildConfig.IS_TV) 40 else 48).dpToPx(requireContext())
                ).apply {
                    marginEnd = 8.dpToPx(requireContext())
                }
                layoutParams = params

                setOnClickListener {
                    selectedSeasonKey = seasonKey
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("selected_season", seasonKey)
                    updateEpisodes(seasonKey)
                    for (i in 0 until container.childCount) {
                        val child = container.getChildAt(i)
                        if (child.tag != null) child.isSelected = child.tag == seasonKey
                    }
                }
            }

            if (index == 0) {
                firstButton = seasonButton
            }

            container.addView(seasonButton)
        }



        updateEpisodes(selectedSeasonKey)
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
    }

    private fun launchPlayer(series: Series, videoUrl: String, episodeTitle: String, startPos: Long = 0L) {
        if (videoUrl.isBlank()) { Toast.makeText(context, "لینک پخش موجود نیست", Toast.LENGTH_SHORT).show(); return }
        val card = series.asCard().copy(videoUrl1 = videoUrl, seasonKey = selectedSeasonKey, episodeKey = episodeTitle)
        val intent = android.content.Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
            putExtra("video_url", videoUrl)
            putExtra("movie_json", Gson().toJson(card))
            putExtra("start_pos", startPos)
        }
        startActivity(intent)
    }

    private fun fetchSeriesTmdbDetails(tmdbId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tvDetails = viewModel.fetchTvDetailsForPoster(tmdbId)
                if (tvDetails != null && _binding != null) {
                    activity?.runOnUiThread {
                        updateUI(tvDetails)
                    }
                    val alternate = viewModel.getSeriesBackdropPaths(tmdbId)
                        .firstOrNull { it != tvDetails.backdrop_path }
                    if (alternate != null && _binding != null) {
                        currentBackdropPath = alternate
                        (binding.rvEpisodes.adapter as? EpisodesAdapter)?.updateBackdrop(alternate)
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
                .load(ImageUrls.backdrop(details.backdrop_path))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .into(binding.imgBackdrop)

            (binding.rvEpisodes.adapter as? EpisodesAdapter)?.updateBackdrop(currentBackdropPath)
        }


        val yearStr = (details.first_air_date?.take(4) ?: "").toPersianDigits()
        binding.txtYear.text = yearStr


        val ratingStr = String.format(Locale.ENGLISH, "%.1f", details.vote_average).toPersianDigits()
        binding.txtRating.text = if (details.vote_average > 0) "امتیاز $ratingStr" else ""


        val limitedGenres = details.genres.joinToString("، ") { it.name }
        val genresStr = TranslationHelper.translateDetailGenres(limitedGenres)
        binding.txtGenres.text = genresStr


        val series = selectedSeries
        val langName = viewModel.languagesList.value?.find {
            it.id.toString() == series?.language || "lan_%02d".format(it.id) == series?.language || it.name == series?.language
        }?.name?.toPersianDigits()

        binding.txtLanguage.apply {
            if (!langName.isNullOrEmpty()) {
                text = langName
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }



    }

    override fun onResume() {
        super.onResume()
        viewModel.loadContinueList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}
