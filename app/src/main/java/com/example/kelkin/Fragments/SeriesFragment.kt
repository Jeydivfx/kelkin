package com.example.kelkin.Fragments

import android.os.Bundle
import android.util.Log
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
import com.example.kelkin.Adapter.SeriesGridAdapter
import com.example.kelkin.DataClass.SeriesCategory
import com.example.kelkin.MainActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentSeriesBinding
import com.example.kelkin.utils.GridSpacingItemDecoration
import java.text.Collator
import java.util.Locale

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var seriesAdapter: SeriesGridAdapter

    private var currentCategory: Int = 0
    private var currentSort: String = "date"
    private var seriesCategoryList = listOf<SeriesCategory>()

    private var isFirstFocusSet = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.progressBarSeries.visibility = View.VISIBLE

        viewModel.loadSeriesCategories()

        setupRecyclerView()
        setupFilterButtons()
        observeSeries()
    }

    private fun setupFilterButtons() {
        binding.btnSeriesGenreFilter.setOnClickListener { showGenreMenu() }
        binding.btnSeriesSortOrder.setOnClickListener { showSortMenu() }
    }

    private fun showGenreMenu() {
        if (seriesCategoryList.isEmpty()) return

        val genreNames = seriesCategoryList.map { it.name }
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val listPopupWindow = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnSeriesGenreFilter
        listPopupWindow.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, genreNames))
        listPopupWindow.isModal = true

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = seriesCategoryList[position]
            currentCategory = selectedCategory.id
            binding.btnSeriesGenreFilter.text = selectedCategory.name
            applyFilters()
            listPopupWindow.dismiss()
        }
        listPopupWindow.show()
    }

    private fun showSortMenu() {
        val sortOptions = listOf("تاریخ ساخت", "عنوان (الفبا)")
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val listPopupWindow = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnSeriesSortOrder
        listPopupWindow.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions))
        listPopupWindow.isModal = true

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            currentSort = if (position == 0) "date" else "title"
            binding.btnSeriesSortOrder.text = sortOptions[position]
            applyFilters()
            listPopupWindow.dismiss()
        }
        listPopupWindow.show()
    }

    private fun applyFilters() {
        val allSeries = viewModel.seriesList.value ?: emptyList()
        val detailsMap = viewModel.movieDetailsMap.value ?: emptyMap()

        val categoryFilter = currentCategory

        var filteredList = if (categoryFilter == 0) {
            allSeries
        } else {
            allSeries.filter { it.category.toInt() == categoryFilter }
        }


        val persianCollator = Collator.getInstance(Locale("fa"))
        filteredList = if (currentSort == "date") {
            filteredList.sortedByDescending { it.id }
        } else {
            filteredList.sortedWith { s1, s2 -> persianCollator.compare(s1.name_fa, s2.name_fa) }
        }

        val finalDisplayList = filteredList.map { seriesItem ->
            if (seriesItem.tmdb_id.isNotEmpty() && !detailsMap.containsKey(seriesItem.tmdb_id)) {
                viewModel.fetchSeriesTmdbDetails(seriesItem.tmdb_id)
            }

            val tmdbDetails = detailsMap[seriesItem.tmdb_id]

            seriesItem.copy().apply {
                if (posterUrl.isEmpty()) {
                    posterUrl = tmdbDetails?.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
                } else if (!posterUrl.startsWith("http")) {
                    val cleanPath = if (posterUrl.startsWith("/")) posterUrl else "/$posterUrl"
                    posterUrl = "https://image.tmdb.org/t/p/w500$cleanPath"
                }
                voteAverage = tmdbDetails?.vote_average ?: voteAverage
            }
        }

        seriesAdapter.submitList(finalDisplayList)
    }

    private fun observeSeries() {
        viewModel.seriesCategoriesList.observe(viewLifecycleOwner) { categories ->
            this.seriesCategoryList = categories ?: emptyList()
        }

        viewModel.seriesList.observe(viewLifecycleOwner) { series ->
            if (series != null) {
                binding.progressBarSeries.visibility = View.GONE
                applyFilters()
                tryAutoFocus()
            }
        }

        viewModel.movieDetailsMap.observe(viewLifecycleOwner) {
            applyFilters()
        }
    }

    private fun setupRecyclerView() {
        binding.rvSeriesAll.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.rvSeriesAll.addItemDecoration(GridSpacingItemDecoration(5, 20, true))

        seriesAdapter = SeriesGridAdapter { series ->
            val bundle = Bundle().apply { putSerializable("selected_series", series) }
            findNavController().navigate(R.id.action_seriesFragment_to_seriesDetailsFragment, bundle)
        }

        binding.rvSeriesAll.adapter = seriesAdapter
    }

    private fun tryAutoFocus() {
        if (isFirstFocusSet || seriesAdapter.itemCount == 0) return
        binding.rvSeriesAll.postDelayed({
            if (_binding != null && !isFirstFocusSet) {
                val firstItem = binding.rvSeriesAll.layoutManager?.findViewByPosition(0)
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

    override fun onResume() {
        super.onResume()
        (requireActivity() as? MainActivity)?.collapseSidebar()
    }
}