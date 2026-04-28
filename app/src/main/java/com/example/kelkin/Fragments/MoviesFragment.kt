package com.example.kelkin.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kelkin.Adapter.MovieGridAdapter
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentMoviesBinding
import java.text.Collator
import java.util.Locale

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var movieAdapter: MovieGridAdapter

    private var currentCategory: Long = 0
    private var currentSort: String = "date"

    private var isFirstFocusSet = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.progressBarMovies.visibility = View.VISIBLE

        setupRecyclerView()
        setupFilterButtons()
        observeMovies()

        viewModel.loadAllMovies()
    }

    private fun setupFilterButtons() {
        // ۱. استفاده از ListPopupWindow برای ژانرها
        binding.btnGenreFilter.setOnClickListener { showGenreMenu() }

        // ۲. استفاده از ListPopupWindow برای مرتب‌سازی
        binding.btnSortOrder.setOnClickListener { showSortMenu() }

        // دکمه نوع (فیلم/سریال) را هم می‌توانی به همین شکل اصلاح کنی
        binding.btnTypeFilter.setOnClickListener {
            // فعلاً برای شلوغ نشدن کد، منطق مشابه ژانر را اینجا هم می‌توانی بزنی
        }
    }

    private fun showGenreMenu() {
        val genres = listOf("همه ژانرها", "اکشن", "کمدی", "ترسناک", "عاشقانه", "درام", "کلاسیک")
        // استفاده از کانتکست با تم مناسب
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val listPopupWindow = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnGenreFilter
        listPopupWindow.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, genres))

        listPopupWindow.isModal = true // بسیار مهم برای اندروید تی‌وی

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            currentCategory = position.toLong()
            binding.btnGenreFilter.text = genres[position]
            applyFilters()
            listPopupWindow.dismiss()
        }

        listPopupWindow.show()
        listPopupWindow.listView?.apply {
            id = R.id.radio_menu_list // استفاده از همان آیدی که در ids.xml ساختیم
            requestFocus()
        }
    }

    private fun showSortMenu() {
        val sortOptions = listOf("تاریخ ساخت", "عنوان (الفبا)")
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val listPopupWindow = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnSortOrder
        listPopupWindow.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions))

        listPopupWindow.isModal = true

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            currentSort = if (position == 0) "date" else "title"
            binding.btnSortOrder.text = sortOptions[position]
            applyFilters()
            listPopupWindow.dismiss()
        }

        listPopupWindow.show()
        listPopupWindow.listView?.apply {
            id = R.id.radio_menu_list
            requestFocus()
        }
    }

    private fun applyFilters() {
        val allMovies = viewModel.movies.value ?: return
        val detailsMap = viewModel.movieDetailsMap.value ?: emptyMap()

        // ۱. فیلتر ژانر
        var filteredList = allMovies.filter { movie ->
            if (currentCategory == 0L) true else movie.category == currentCategory
        }

        // ۲. مرتب‌سازی هوشمند
        val persianCollator = Collator.getInstance(Locale("fa"))
        filteredList = if (currentSort == "date") {
            filteredList.sortedByDescending { it.id } // یا بر اساس تاریخ TMDB
        } else {
            filteredList.sortedWith { m1, m2 ->
                persianCollator.compare(m1.name_fa, m2.name_fa)
            }
        }

        // ۳. آپدیت پوسترها و ارسال به آداپتر
        val finalDisplayList = filteredList.map { movie ->
            movie.copy().apply {
                posterUrl = detailsMap[tmdb_id]?.poster_path ?: ""
            }
        }

        // استفاده از toList() برای اطمینان از آپدیت شدن ListAdapter
        movieAdapter.submitList(finalDisplayList.toList())
    }

    private fun observeMovies() {
        viewModel.movies.observe(viewLifecycleOwner) {
            binding.progressBarMovies.visibility = View.GONE
            applyFilters()
            tryAutoFocus()
        }
        viewModel.movieDetailsMap.observe(viewLifecycleOwner) {
            applyFilters()
        }
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

    private fun setupRecyclerView() {
        binding.rvMoviesAll.layoutManager = GridLayoutManager(requireContext(), 6)
        movieAdapter = MovieGridAdapter { movie ->
            val bundle = Bundle().apply { putSerializable("selected_movie", movie) }
            findNavController().navigate(R.id.action_moviesFragment_to_movieDetailFragment, bundle)
        }
        binding.rvMoviesAll.adapter = movieAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}