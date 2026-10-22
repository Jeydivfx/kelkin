package com.example.kelkin.Fragments

import com.example.kelkin.utils.*
import kotlinx.coroutines.CancellationException

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.kelkin.Adapter.CastAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.MainActivity
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
                viewModel.loadContinueList()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.collapseSidebar()
        view.animateDetailEntrance()

        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        selectedMovie = arguments?.getSerializable("selected_movie") as? Movie
            ?: arguments?.getSerializable("movie_data") as? Movie


        selectedMovie?.let { movie ->
            val btnContinue = view.findViewById<Button>(R.id.btnContinue)
            val btnPlay = view.findViewById<Button>(R.id.btnPlay)
            val btnBookmark = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBookmark)
            val txtTitle = view.findViewById<TextView>(R.id.txtTitle)
            val txtDescription = view.findViewById<TextView>(R.id.txtDescription)


            RecyclerFocusMemory(this, btnContinue.takeIf { it.visibility == View.VISIBLE } ?: btnPlay)

            txtTitle.text = movie.name_fa
            txtDescription.bindDetailSynopsis(movie.description_fa)
            view.hideEmptyDetailTags()
            view.findViewById<TextView>(R.id.txtRating).text = detailRatingText(movie.voteAverage)
            viewModel.languagesList.observe(viewLifecycleOwner) { view.bindLocalDetailTags(movie, it.orEmpty(), viewModel.qualitiesList.value.orEmpty()) }
            viewModel.qualitiesList.observe(viewLifecycleOwner) { view.bindLocalDetailTags(movie, viewModel.languagesList.value.orEmpty(), it.orEmpty()) }
            viewModel.movieDetailsMap.value?.get(metadataKey("movie", movie.tmdb_id))?.let {
                updateUI(it, null)
            }

            // Bookmark Logic
            val emptyIcon = if (com.example.kelkin.BuildConfig.IS_TV) R.drawable.ic_detail_add else R.drawable.ic_bookmark
            val savedIcon = if (com.example.kelkin.BuildConfig.IS_TV) R.drawable.ic_detail_added else R.drawable.ic_bookmark_filled
            btnBookmark.setIconResource(if (viewModel.isInMyList(movie)) savedIcon else emptyIcon)
            if (com.example.kelkin.BuildConfig.IS_TV) btnBookmark.text = if (viewModel.isInMyList(movie)) "حذف از لیست" else "افزودن به لیست"
            btnBookmark.setOnClickListener {
                if (viewModel.isInMyList(movie)) {
                    viewModel.removeFromMyList(movie)
                    btnBookmark.setIconResource(emptyIcon)
                    if (com.example.kelkin.BuildConfig.IS_TV) btnBookmark.text = "افزودن به لیست"
                } else {
                    viewModel.addToMyList(movie)
                    btnBookmark.setIconResource(savedIcon)
                    if (com.example.kelkin.BuildConfig.IS_TV) btnBookmark.text = "حذف از لیست"
                }
            }

            viewModel.continueWatchingList.observe(viewLifecycleOwner) { history ->
                val saved = history.orEmpty().find { it.identity() == movie.identity() }
                btnContinue.visibility = if (saved != null && saved.lastPosition >= 2000) View.VISIBLE else View.GONE
                if (com.example.kelkin.BuildConfig.IS_TV) {
                    btnPlay.visibility = if (btnContinue.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
                btnContinue.text = "ادامه تماشا"
                btnContinue.setOnClickListener {
                    val position = viewModel.continueWatchingList.value.orEmpty().find { it.identity() == movie.identity() }?.lastPosition ?: 0L
                    launchPlayer(movie, position)
                }
            }

            btnPlay.setOnClickListener { launchPlayer(movie, 0L) }
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
        if (movie.videoUrl1.isBlank()) {
            android.widget.Toast.makeText(context, "لینک پخش موجود نیست", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        playerLauncher.launch(intent)
    }

    private fun fetchFullDetails(tmdbId: String) {
        if (tmdbId.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val (details, credits) = viewModel.getMovieFullDetails(tmdbId)
            if (details != null) updateUI(details, credits)
            else if (credits != null) bindCast(credits)
        }
    }

    private fun bindCast(credits: com.example.kelkin.DataClass.MovieCreditsResponse) {
        view?.findViewById<RecyclerView>(R.id.rvCast)?.apply {
            if (com.example.kelkin.BuildConfig.IS_TV) itemAnimator = null
            val manager = layoutManager as? LinearLayoutManager
            if (manager == null || manager.orientation != LinearLayoutManager.HORIZONTAL) {
                layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            }
            adapter = CastAdapter(credits.cast.take(7), if (com.example.kelkin.BuildConfig.IS_TV) 68 else 80)
        }
    }

    private fun updateUI(details: com.example.kelkin.DataClass.TmdbMovieDetails, credits: com.example.kelkin.DataClass.MovieCreditsResponse?) {
        val view = view ?: return
        val movie = selectedMovie ?: return

        if (credits != null) bindCast(credits)

        view.findViewById<TextView>(R.id.txtTitleEnglish)?.text = details.title

        view.findViewById<ImageView>(R.id.imgBackdrop)?.let {
            Glide.with(this)
                .load(ImageUrls.backdrop(details.backdrop_path))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .into(it)
        }


        val yearStr = details.release_date.take(4).toPersianDigits()
        view.findViewById<TextView>(R.id.txtYear)?.text = yearStr


        val ratingStr = String.format(Locale.ENGLISH, "%.1f", details.vote_average).toPersianDigits()
        view.findViewById<TextView>(R.id.txtRating)?.text = detailRatingText(details.vote_average)


        val limitedGenres = details.genres.joinToString("، ") { it.name }
        val genresStr = TranslationHelper.translateDetailGenres(limitedGenres)
        view.findViewById<TextView>(R.id.txtGenres)?.text = genresStr


        val langName = viewModel.languagesList.value?.find {
            it.id.toString() == movie.language || "lan_%02d".format(it.id) == movie.language || it.name == movie.language
        }?.name?.toPersianDigits()

        view.findViewById<TextView>(R.id.txtLanguage)?.apply {
            if (!langName.isNullOrEmpty()) {
                text = langName
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }



    }


}
