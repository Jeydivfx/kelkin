package com.example.kelkin.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kelkin.Adapter.ChannelAdapter
import com.example.kelkin.DataClass.TvCategory
import com.example.kelkin.PlayerActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentTvBinding
import com.google.gson.Gson
import java.text.Collator
import java.util.Locale

class TvFragment : Fragment() {

    private var _binding: FragmentTvBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var channelAdapter: ChannelAdapter

    private var tvCategoryList = listOf<TvCategory>()
    private var currentCategory: Long = 0
    private var currentSort: String = "default"
    private var isFirstFocusSet = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.progressBar.visibility = View.VISIBLE

        setupRecyclerView()
        setupFilterButtons()
        observeData()
    }

    private fun setupFilterButtons() {
        binding.btnTvCategory.setOnClickListener { showCategoryMenu() }
        binding.btnTvSort.setOnClickListener { showSortMenu() }
        binding.btnTvFavorites.setOnClickListener {  }
    }

    private fun observeData() {
        viewModel.tvCategoriesList.observe(viewLifecycleOwner) { categories ->
            this.tvCategoryList = categories ?: emptyList()
        }

        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            if (channels != null) {
                binding.progressBar.visibility = View.GONE
                applyFilters()
                if (channels.isNotEmpty() && !isFirstFocusSet) setFirstFocus()
            }
        }
    }

    private fun showCategoryMenu() {
        if (tvCategoryList.isEmpty()) return

        val names = tvCategoryList.map { it.name }
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val popup = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        popup.anchorView = binding.btnTvCategory
        popup.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names))
        popup.isModal = true

        popup.setOnItemClickListener { _, _, position, _ ->
            val selected = tvCategoryList[position]
            currentCategory = selected.id.toLong()
            binding.btnTvCategory.text = selected.name
            applyFilters()
            popup.dismiss()
        }
        popup.show()
    }

    private fun showSortMenu() {
        val sortOptions = listOf("لیست پیش‌فرض", "ترتیب الفبا")
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val popup = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        popup.anchorView = binding.btnTvSort
        popup.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions))
        popup.isModal = true

        popup.setOnItemClickListener { _, _, position, _ ->
            currentSort = if (position == 0) "default" else "alpha"
            binding.btnTvSort.text = sortOptions[position]
            applyFilters()
            popup.dismiss()
        }
        popup.show()
    }

    private fun applyFilters() {
        val allChannels = viewModel.channels.value ?: return

        var filteredList = if (currentCategory == 0L) {
            allChannels
        } else {
            allChannels.filter { it.category == currentCategory }
        }

        val persianCollator = Collator.getInstance(Locale("fa"))
        filteredList = if (currentSort == "alpha") {
            filteredList.sortedWith { c1, c2 -> persianCollator.compare(c1.name_fa, c2.name_fa) }
        } else {
            filteredList.sortedBy { it.id }
        }

        channelAdapter.submitList(filteredList)
    }

    private fun setupRecyclerView() {


        binding.rvChannels.layoutManager = GridLayoutManager(requireContext(), 5)
        channelAdapter = ChannelAdapter { channel ->
            val channelJson = Gson().toJson(channel)
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("channel_json", channelJson)
                putExtra("video_url", channel.videoUrl)
                putExtra("channel_name", channel.name_fa)
                putExtra("is_live", true)
                putExtra("selected_category_id", currentCategory)
            }
            startActivity(intent)
        }
        binding.rvChannels.adapter = channelAdapter
    }

    private fun setFirstFocus() {
        binding.rvChannels.postDelayed({
            if (_binding != null) {
                binding.rvChannels.layoutManager?.findViewByPosition(0)?.requestFocus()
                isFirstFocusSet = true
            }
        }, 300)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}