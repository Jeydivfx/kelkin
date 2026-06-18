package com.example.kelkin.Fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.kelkin.Adapter.CastAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentMovieDetailBinding
import com.example.kelkin.`object`.TranslationHelper
import com.example.kelkin.`object`.TranslationHelper.toPersianDigits
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Locale

class MovieDetailFragment : Fragment(R.layout.fragment_movie_detail) {

    private var selectedMovie: Movie? = null
    private lateinit var viewModel: HomeViewModel
    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!

    private val playerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val lastPos = data?.getLongExtra("last_pos", 0L) ?: 0L
            val totalDur = data?.getLongExtra("total_dur", 0L) ?: 0L
            selectedMovie?.let { movie ->
                if (lastPos > 10000) viewModel.updateContinueWatching(movie, lastPos, totalDur)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        selectedMovie = arguments?.getSerializable("selected_movie") as? Movie
            ?: arguments?.getSerializable("movie_data") as? Movie


        selectedMovie?.let { movie ->
            Log.d("SearchTest", "فیلم دریافت شد: ${movie.name_fa}")
            val btnContinue = view.findViewById<Button>(R.id.btnContinue)
            val btnPlay = view.findViewById<Button>(R.id.btnPlay)
            val btnBookmark = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBookmark)
            val txtTitle = view.findViewById<TextView>(R.id.txtTitle)
            val txtDescription = view.findViewById<TextView>(R.id.txtDescription)


            btnPlay.post {
                btnPlay.requestFocus()
            }

            txtTitle.text = movie.name_fa
            txtDescription.text = movie.description_fa

            // Bookmark Logic
            btnBookmark.setIconResource(if (viewModel.isInMyList(movie.tmdb_id)) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark)
            btnBookmark.setOnClickListener {
                if (viewModel.isInMyList(movie.tmdb_id)) {
                    viewModel.removeFromMyList(movie)
                    btnBookmark.setIconResource(R.drawable.ic_bookmark)
                } else {
                    viewModel.addToMyList(movie)
                    btnBookmark.setIconResource(R.drawable.ic_bookmark_filled)
                }
            }

            // Play/Continue Logic
            val savedProgress = viewModel.continueWatchingList.value?.find { it.tmdb_id == movie.tmdb_id }
            if (savedProgress != null && savedProgress.lastPosition > 10000) {
                btnContinue.visibility = View.VISIBLE
                btnContinue.text = "ادامه پخش از دقیقه ${savedProgress.lastPosition / 1000 / 60}"
                btnContinue.setOnClickListener { launchPlayer(movie, savedProgress.lastPosition) }
            } else {
                btnContinue.visibility = View.GONE
            }

            btnPlay.setOnClickListener { launchPlayer(movie, 0L) }

            // Trigger Detail Fetch - Directly call the ViewModel to fetch data
            fetchFullDetails(movie.tmdb_id)
        }
    }

    private fun launchPlayer(movie: Movie, startPos: Long) {
        val intent = android.content.Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
            putExtra("video_url", movie.videoUrl1)
            putExtra("movie_tmdb_id", movie.tmdb_id)
            putExtra("movie_json", Gson().toJson(movie))
            putExtra("start_pos", startPos)
        }
        playerLauncher.launch(intent)
    }

    private fun fetchFullDetails(tmdbId: String) {
        lifecycleScope.launch {
            try {
                viewModel.getMovieFullDetails(tmdbId) { details, credits ->
                    if (details != null && credits != null) {
                        activity?.runOnUiThread {
                            updateUI(details, credits)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("kelkinDebug", "Error: ${e.message}")
            }
        }
    }

    private fun updateUI(details: com.example.kelkin.DataClass.TmdbMovieDetails, credits: com.example.kelkin.DataClass.MovieCreditsResponse) {
        val view = view ?: return

        // ۱. نمایش لیست بازیگران
        view.findViewById<RecyclerView>(R.id.rvCast)?.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = CastAdapter(credits.cast.take(7))
        }

        // ۲. نمایش عنوان انگلیسی
        view.findViewById<TextView>(R.id.txtTitleEnglish)?.text = details.title

        // ۳. نمایش تصویر بک‌دراپ
        view.findViewById<ImageView>(R.id.imgBackdrop)?.let {
            Glide.with(this)
                .load("https://image.tmdb.org/t/p/original${details.backdrop_path}")
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .into(it)
        }

        // ۴. ساخت متن اطلاعات (IMDB، ژانر، سال)
        val ratingStr = String.format(Locale.ENGLISH, "%.1f", details.vote_average).toPersianDigits()
        val genresStr = TranslationHelper.translateGenres(details.genres.joinToString("، ") { it.name })
        val yearStr = details.release_date.take(4).toPersianDigits()

        val fullText = "IMDB $ratingStr  |  $genresStr  |  $yearStr"
        val spannable = SpannableString(fullText)

        // ۵. استایل‌دهی به بخش IMDB
        val imdbPart = "IMDB $ratingStr"
        val startIndex = fullText.indexOf(imdbPart)

        if (startIndex != -1) {
            val endIndex = startIndex + imdbPart.length

            // رنگ طلایی برای IMDB
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FFD700")),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // بولد کردن IMDB
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        view.findViewById<TextView>(R.id.txtInfo)?.text = spannable
    }
}