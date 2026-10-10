package com.example.kelkin.Fragments


import com.example.kelkin.utils.*

import com.example.kelkin.utils.showSortPicker
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kelkin.Adapter.MovieGridAdapter
import com.example.kelkin.DataClass.Category
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentMoviesBinding
import com.example.kelkin.utils.GridSpacingItemDecoration
import java.text.Collator
import java.util.Locale

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var movieAdapter: MovieGridAdapter

    private var currentCategory: Long = 0
    private var currentSort: String = "date"
    private var categoryList = listOf<Category>()

    private lateinit var focusMemory: RecyclerFocusMemory
    private lateinit var statusView: CatalogStatusView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.progressBarMovies.visibility = View.VISIBLE

        viewModel.loadCategories()

        val screenState = findNavController().currentBackStackEntry!!.savedStateHandle
        currentCategory = screenState.get<Long>("category") ?: 0L
        currentSort = screenState.get<String>("sort") ?: "date"
        setupRecyclerView()
        focusMemory = RecyclerFocusMemory(this, binding.btnGenreFilter)
        focusMemory.track(binding.rvMoviesAll)
        statusView = CatalogStatusView(binding.root) { viewModel.refresh() }
        viewModel.syncError.observe(viewLifecycleOwner) { applyFilters() }
        viewModel.catalogLoading.observe(viewLifecycleOwner) { applyFilters() }
        setupFilterButtons()
        observeMovies()
    }

    private fun setupFilterButtons() {
        binding.btnTypeFilter.visibility = View.GONE
        binding.btnGenreFilter.setOnClickListener { showGenreMenu() }
        binding.btnSortOrder.setOnClickListener { showSortMenu() }
    }

    private fun showGenreMenu() {
        if (categoryList.isEmpty()) return

        val genreNames = categoryList.map { it.name }
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val listPopupWindow = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnGenreFilter
        listPopupWindow.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, genreNames))
        listPopupWindow.isModal = true

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categoryList[position]
            currentCategory = selectedCategory.id.toLong()
            binding.btnGenreFilter.text = selectedCategory.name
            applyFilters()
            listPopupWindow.dismiss()
        }
        listPopupWindow.show()
    }

    private fun showSortMenu() {
        val sortOptions = listOf("تازه اضافه‌شده", "عنوان (الفبا)")
        showSortPicker(binding.btnSortOrder, if (currentSort == "date") 0 else 1, false) { position ->
            currentSort = if (position == 0) "date" else "title"
            binding.btnSortOrder.text = "ترتیب: ${sortOptions[position]}"
            applyFilters()
        }
    }

    private fun applyFilters() {
        if (!::statusView.isInitialized) return
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("category", currentCategory); set("sort", currentSort)
        }
        val allMovies = viewModel.moviesList.value ?: return
        val detailsMap = viewModel.movieDetailsMap.value ?: emptyMap()

        val categoryFilter = currentCategory

        var filteredList = if (categoryFilter == 0L) {
            allMovies
        } else {
            allMovies.filter { it.category == categoryFilter }
        }

        val persianCollator = Collator.getInstance(Locale("fa"))
        filteredList = if (currentSort == "date") {
            filteredList.sortedByDescending { it.id }
        } else {
            filteredList.sortedWith { m1, m2 -> persianCollator.compare(m1.name_fa, m2.name_fa) }
        }

        filteredList.take(30).forEach { viewModel.fetchTmdbDetails(it.tmdb_id) }
        val finalDisplayList = filteredList.map { movie ->
            val tmdbDetails = detailsMap[metadataKey("movie", movie.tmdb_id)]
            movie.copy(
                voteAverage = tmdbDetails?.vote_average ?: movie.voteAverage,
                posterUrl = tmdbDetails?.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" } ?: movie.posterUrl
            )
        }

        movieAdapter.submitList(finalDisplayList) { focusMemory.restore() }
        val empty = finalDisplayList.isEmpty()
        binding.rvMoviesAll.visibility = if (empty) View.GONE else View.VISIBLE
        statusView.show(empty, viewModel.catalogLoading.value == true, viewModel.syncError.value)
        binding.progressBarMovies.visibility = if (empty && viewModel.catalogLoading.value == true) View.VISIBLE else View.GONE
    }

    private fun observeMovies() {

        viewModel.categoriesList.observe(viewLifecycleOwner) { categories ->
            this.categoryList = categories ?: emptyList()
            binding.btnGenreFilter.text = categoryList.find { it.id.toLong() == currentCategory }?.name ?: "همه ژانرها"
            binding.btnSortOrder.text = if (currentSort == "date") "ترتیب: تازه اضافه‌شده" else "ترتیب: عنوان (الفبا)"
        }

        viewModel.moviesList.observe(viewLifecycleOwner) { movies ->
            if (movies != null) {
                binding.progressBarMovies.visibility = View.GONE
                applyFilters()

            }
        }

        viewModel.movieDetailsMap.observe(viewLifecycleOwner) {
            applyFilters()
        }
    }

    private fun setupRecyclerView() {
        binding.rvMoviesAll.configureCatalogGrid()

        movieAdapter = MovieGridAdapter { movie ->
            val bundle = Bundle().apply { putSerializable("selected_movie", movie) }
            findNavController().navigate(R.id.action_moviesFragment_to_movieDetailFragment, bundle)
        }
        movieAdapter.onVisible = { viewModel.fetchTmdbDetails(it.tmdb_id) }
        binding.rvMoviesAll.adapter = movieAdapter
    }

    private fun restoreFocus() { focusMemory.restore() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as? MainActivity)?.collapseSidebar()
    }
}
