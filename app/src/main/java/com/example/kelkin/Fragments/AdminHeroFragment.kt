package com.example.kelkin.Fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kelkin.Adapter.MovieGridAdapter
import com.example.kelkin.DataClass.Movie
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentAdminHeroBinding
import com.example.kelkin.utils.*
import kotlinx.coroutines.*

class AdminHeroFragment : AdminFragment(R.layout.fragment_admin_hero) {
    private var _binding: FragmentAdminHeroBinding? = null
    private val binding get() = _binding!!
    private lateinit var model: HomeViewModel
    private lateinit var adapter: MovieGridAdapter
    private lateinit var focusMemory: RecyclerFocusMemory
    private var movies = emptyList<Movie>()
    private var currentKey: String? = null
    private var pendingKey: String? = null
    private var loading = true
    private var retryLoad = false
    private var saving = false
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminHeroBinding.bind(view)
        pendingKey = savedInstanceState?.getString("hero_pending")
        model = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        adapter = MovieGridAdapter { movie ->
            if (!saving) {
                pendingKey = movie.remoteKey
                binding.heroSearch.clearFocus()
                androidx.core.view.ViewCompat.getWindowInsetsController(binding.root)
                    ?.hide(androidx.core.view.WindowInsetsCompat.Type.ime())
                render()
            }
        }.apply { onVisible = { model.fetchTmdbDetails(it.tmdb_id) } }
        binding.heroMovies.configureCatalogGrid()
        binding.heroMovies.adapter = adapter
        focusMemory = RecyclerFocusMemory(this, binding.heroBack)
        focusMemory.track(binding.heroMovies)
        binding.heroSearch.setText(savedInstanceState?.getString("hero_query").orEmpty())
        binding.heroSearch.doAfterTextChanged { render() }
        binding.heroBack.setOnClickListener { findNavController().popBackStack() }
        binding.saveHero.setOnClickListener { if (retryLoad) load() else save() }
        model.movieDetailsMap.observe(viewLifecycleOwner) { render() }
        load()
    }

    private fun load() {
        if (job?.isActive == true) return
        loading = true; retryLoad = false; render()
        job = viewLifecycleOwner.lifecycleScope.launch {
            try {
                withTimeout(20_000) {
                    val catalogTask = AdminSession.database.getReference("movies").get()
                    val heroTask = AdminSession.database.getReference("app_config/home/hero_movie_key").get()
                    val snapshot = catalogTask.awaitResult()
                    currentKey = heroTask.awaitResult().value?.toString()?.trim()
                    movies = snapshot.children.map { child ->
                        fun text(key: String) = child.child(key).value?.toString().orEmpty()
                        Movie(id = text("id").toLongOrNull() ?: 0L,
                            name_fa = text("name_fa"), posterUrl = text("posterUrl"),
                            tmdb_id = text("tmdb_id"), remoteKey = child.key.orEmpty())
                    }.sortedWith(compareBy<Movie> { it.name_fa }.thenBy { it.remoteKey })
                    if (movies.none { it.remoteKey == pendingKey }) pendingKey = selectHeroMovie(movies, currentKey)?.remoteKey
                }
                loading = false; render()
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                loadFailed()
            } catch (e: Exception) { loadFailed() }
        }
    }
    private fun loadFailed() {
        loading = false; retryLoad = true; render()
        binding.heroStatus.text = "دریافت فیلم‌ها انجام نشد؛ اتصال و دسترسی مدیر را بررسی کنید."
    }
    private fun normalize(value: String) = value.replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ').trim()

    private fun render() {
        if (_binding == null) return
        binding.heroLoading.visibility = if (loading || saving) View.VISIBLE else View.GONE
        val current = selectHeroMovie(movies, currentKey)
        binding.currentHero.text = when {
            loading -> "در حال دریافت انتخاب فعلی…"
            retryLoad -> "انتخاب فعلی دریافت نشد"
            current == null -> "هنوز فیلمی در آرشیو موجود نیست"
            currentKey.isNullOrBlank() -> "هروی فعلی (جایگزین): ${current.name_fa}"
            else -> "هروی فعلی: ${current.name_fa}"
        }
        val selected = movies.find { it.remoteKey == pendingKey }
        val query = normalize(binding.heroSearch.text.toString())
        val visible = movies.filter { normalize(it.name_fa).contains(query, true) }
        val details = model.movieDetailsMap.value.orEmpty()
        adapter.submitList(visible.map { movie ->
            val metadata = details[metadataKey("movie", movie.tmdb_id)]
            movie.copy(name_fa = (if (movie.remoteKey == pendingKey) "✓  " else "") + movie.name_fa,
                posterUrl = ImageUrls.poster(metadata?.poster_path) ?: movie.posterUrl,
                voteAverage = metadata?.vote_average ?: 0.0)
        }) { if (_binding != null) focusMemory.restore() }
        binding.heroStatus.text = when {
            saving -> "در حال ذخیرهٔ انتخاب شما…"
            loading -> "در حال دریافت فیلم‌ها…"
            visible.isEmpty() && movies.isNotEmpty() -> "فیلمی با این نام پیدا نشد."
            selected != null -> "انتخاب شما: ${selected.name_fa}"
            else -> "فیلم را انتخاب کنید، سپس ذخیره را بزنید."
        }
        binding.saveHero.text = if (retryLoad) "تلاش دوباره" else "ذخیرهٔ هرو"
        binding.saveHero.isEnabled = !loading && !saving && (retryLoad || selected != null)
    }

    private fun save() {
        val movie = movies.find { it.remoteKey == pendingKey } ?: return
        if (loading || saving || job?.isActive == true) return
        saving = true; render()
        job = viewLifecycleOwner.lifecycleScope.launch {
            try {
                withTimeout(20_000) {
                    check(AdminSession.verify()) { "اجازهٔ مدیریت ندارید." }
                    check(AdminSession.database.getReference("movies").child(movie.remoteKey).get().awaitResult().exists()) {
                        "این فیلم از آرشیو حذف شده؛ صفحه را دوباره باز کنید."
                    }
                    AdminSession.database.getReference("app_config/home/hero_movie_key").setValue(movie.remoteKey).awaitResult()
                }
                currentKey = movie.remoteKey
                Toast.makeText(requireContext(), "هرو بنر ذخیره شد", Toast.LENGTH_SHORT).show()
                saving = false; render()
                binding.heroStatus.text = "ذخیره شد: ${movie.name_fa}"
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                saving = false; render(); binding.heroStatus.text = "زمان ذخیره تمام شد؛ دوباره تلاش کنید."
            } catch (e: Exception) {
                saving = false; render()
                binding.heroStatus.text = "ذخیره انجام نشد؛ اتصال، دسترسی مدیر و موجود بودن فیلم را بررسی کنید."
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("hero_pending", pendingKey)
        outState.putString("hero_query", _binding?.heroSearch?.text?.toString())
    }
    override fun onDestroyView() {
        job?.cancel()
        _binding?.heroMovies?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
