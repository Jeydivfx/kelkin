package com.example.kelkin.Fragments

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

    private var isFirstFocusSet = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.progressBarMovies.visibility = View.VISIBLE

        viewModel.loadCategories()

        setupRecyclerView()
        setupFilterButtons()
        observeMovies()
    }

    private fun setupFilterButtons() {
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

        android.util.Log.d("CategoryDebug", "Category list size: ${categoryList.size}")
        categoryList.forEach {
            android.util.Log.d("CategoryDebug", "ID: ${it.id}, Name: ${it.name}")
        }

        if (categoryList.isEmpty()) {
            android.util.Log.e("CategoryDebug", "Category list is EMPTY!")
            return
        }


        val sortOptions = listOf("تاریخ ساخت", "عنوان (الفبا)")
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val listPopupWindow = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnSortOrder
        listPopupWindow.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions))
        listPopupWindow.isModal = true

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            currentSort = if (position == 0) "date" else "title"
            binding.btnSortOrder.text = sortOptions[position]
            applyFilters()
            listPopupWindow.dismiss()
        }
        listPopupWindow.show()
    }

    private fun applyFilters() {
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

        val finalDisplayList = filteredList.map { movie ->
            movie.copy().apply {
                posterUrl = detailsMap[tmdb_id]?.poster_path ?: posterUrl
            }
        }

        movieAdapter.submitList(finalDisplayList)
    }

    private fun observeMovies() {

        viewModel.categoriesList.observe(viewLifecycleOwner) { categories ->
            this.categoryList = categories ?: emptyList()
        }

        // مشاهده لیست فیلم‌ها
        viewModel.moviesList.observe(viewLifecycleOwner) { movies ->
            if (movies != null) {
                binding.progressBarMovies.visibility = View.GONE
                applyFilters()
                tryAutoFocus()
            }
        }

        viewModel.movieDetailsMap.observe(viewLifecycleOwner) {
            applyFilters()
        }
    }

    private fun setupRecyclerView() {
        binding.rvMoviesAll.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.rvMoviesAll.addItemDecoration(GridSpacingItemDecoration(5, 20, true)) // ۵ ستون، ۲۰ پیکسل فاصله

        movieAdapter = MovieGridAdapter { movie ->
            val bundle = Bundle().apply { putSerializable("selected_movie", movie) }
            findNavController().navigate(R.id.action_moviesFragment_to_movieDetailFragment, bundle)
        }
        binding.rvMoviesAll.adapter = movieAdapter
    }

    private fun tryAutoFocus() {
        if (isFirstFocusSet || movieAdapter.itemCount == 0) return
        binding.rvMoviesAll.postDelayed({
            if (_binding != null && !isFirstFocusSet) {
                val firstItem = binding.rvMoviesAll.layoutManager?.findViewByPosition(0)
                firstItem?.let {
                    it.requestFocus()
                    isFirstFocusSet = true
                }
            }
        }, 300)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}