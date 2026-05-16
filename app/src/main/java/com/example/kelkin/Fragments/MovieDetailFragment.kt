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
import android.widget.ImageButton
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
import com.example.kelkin.DataClass.TmdbApiService
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
    private val tmdbApi = TmdbApiService.create()
    private lateinit var viewModel: HomeViewModel
    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!

    private val playerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val lastPos = data?.getLongExtra("last_pos", 0L) ?: 0L
            val totalDur = data?.getLongExtra("total_dur", 0L) ?: 0L


            selectedMovie?.let { movie ->
                if (lastPos > 10000) {
                    viewModel.updateContinueWatching(movie, lastPos, totalDur)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPlay = view.findViewById<Button>(R.id.btnPlay)
        val btnContinue = view.findViewById<Button>(R.id.btnContinue)

        view.post {
            if (btnContinue.visibility == View.VISIBLE) {
                btnContinue.requestFocus()
            } else {
                btnPlay.requestFocus()
            }
        }


        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)


        selectedMovie = arguments?.getSerializable("selected_movie") as? Movie

        selectedMovie?.let { movie ->
            Log.d("kelkinDebug", "Selected Movie: ${movie.name_fa}, TMDB_ID: ${movie.tmdb_id}")


            val btnContinue = view.findViewById<Button>(R.id.btnContinue)
            val btnPlay = view.findViewById<Button>(R.id.btnPlay)
            val btnBookmark = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBookmark)
            val txtTitle = view.findViewById<TextView>(R.id.txtTitle)
            val txtDescription = view.findViewById<TextView>(R.id.txtDescription)


            txtTitle.text = movie.name_fa
            txtDescription.text = movie.description_fa


            if (viewModel.isInMyList(movie.tmdb_id)) {
                btnBookmark.setIconResource(R.drawable.ic_bookmark_filled)
            } else {
                btnBookmark.setIconResource(R.drawable.ic_bookmark)
            }

            btnBookmark.setOnClickListener {
                if (viewModel.isInMyList(movie.tmdb_id)) {

                    viewModel.removeFromMyList(movie)
                    btnBookmark.setIconResource(R.drawable.ic_bookmark)
                    Toast.makeText(requireContext(), "از لیست من حذف شد", Toast.LENGTH_SHORT).show()
                } else {

                    viewModel.addToMyList(movie)
                    btnBookmark.setIconResource(R.drawable.ic_bookmark_filled)
                    Toast.makeText(requireContext(), "به لیست من اضافه شد", Toast.LENGTH_SHORT).show()
                }
            }

            val savedProgress = viewModel.continueWatchingList.value?.find { it.tmdb_id == movie.tmdb_id }

            if (savedProgress != null && savedProgress.lastPosition > 10000) {
                btnContinue.visibility = View.VISIBLE
                val minutes = savedProgress.lastPosition / 1000 / 60
                btnContinue.text = "ادامه پخش از دقیقه $minutes"

                btnContinue.setOnClickListener {
                    val intent = android.content.Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
                        putExtra("video_url", movie.videoUrl1)
                        putExtra("movie_tmdb_id", movie.tmdb_id)
                        putExtra("movie_json", Gson().toJson(movie)) // ارسال کل فیلم
                        putExtra("start_pos", savedProgress?.lastPosition ?: 0L)
                    }
                    playerLauncher.launch(intent)
                }
            } else {
                btnContinue.visibility = View.GONE
            }


            btnPlay.setOnClickListener {
                if (!movie.videoUrl1.isNullOrEmpty()) {
                    val intent = android.content.Intent(requireContext(), com.example.kelkin.PlayerActivity::class.java).apply {
                        putExtra("video_url", movie.videoUrl1)
                        putExtra("movie_tmdb_id", movie.tmdb_id)
                        putExtra("movie_json", Gson().toJson(movie))
                        putExtra("start_pos", 0L)
                    }
                    playerLauncher.launch(intent)
                }
            }


            val apiKey = viewModel.credentials.value?.apiKey ?: ""
            if (apiKey.isNotEmpty()) {
                fetchFullDetails(movie.tmdb_id, apiKey)
            } else {
                Log.e("kelkinDebug", "API Key is EMPTY!")
            }
        }
    }

    private fun fetchFullDetails(tmdbId: String, apiKey: String) {
        lifecycleScope.launch {
            try {

                val credits = tmdbApi.getMovieCredits(tmdbId, apiKey)

                val detailsEn = tmdbApi.getMovieDetails(tmdbId, apiKey, lang = "en-US")
                val rvCast = view?.findViewById<RecyclerView>(R.id.rvCast)
                rvCast?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                rvCast?.adapter = CastAdapter(credits.cast.take(7))

                val details = tmdbApi.getMovieDetails(tmdbId, apiKey)
                val txtTitleEnglish = view?.findViewById<TextView>(R.id.txtTitleEnglish)
                txtTitleEnglish?.text = detailsEn.title


                val imgBackdrop = view?.findViewById<ImageView>(R.id.imgBackdrop)
                Glide.with(this@MovieDetailFragment)
                    .load("https://image.tmdb.org/t/p/original${details.backdrop_path}")
                    .transition(DrawableTransitionOptions.withCrossFade(1000))
                    .into(imgBackdrop!!)


                val rawYear = if (details.release_date.length >= 4) details.release_date.substring(0, 4) else "نامشخص"
                val year = rawYear.toPersianDigits()

                val rawGenres = details.genres.joinToString("، ") { it.name }
                val genres = TranslationHelper.translateGenres(rawGenres)

                val rawRating = String.format(Locale.ENGLISH, "%.1f", details.vote_average)
                val rating = rawRating.toPersianDigits()



                val fullText = "IMDB $rating  |  $genres  |  $year"
                val spannable = SpannableString(fullText)

// پیدا کردن ایندکس برای رنگی کردن بخش IMDB
                val imdbPart = "IMDB $rating"
                val startIndex = fullText.indexOf(imdbPart)
                val endIndex = startIndex + imdbPart.length

                if (startIndex != -1) {
                    // استفاده از رنگی که در colors.xml تعریف کردیم
                    spannable.setSpan(
                        ForegroundColorSpan(Color.parseColor("#FFD700")),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }



                view?.findViewById<TextView>(R.id.txtInfo)?.text = spannable


            } catch (e: Exception) {
                Log.e("kelkinDebug", "Error in fetchFullDetails: ${e.message}")
            }
        }
    }
}
