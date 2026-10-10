package com.example.kelkin.Fragments

import androidx.navigation.fragment.findNavController

import com.example.kelkin.utils.*

import android.content.Intent
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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kelkin.Adapter.ChannelAdapter
import com.example.kelkin.DataClass.TvCategory
import com.example.kelkin.MainActivity
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
    private var favoritesOnly = false
    private var currentCategory: Long = 0
    private var currentSort: String = "default"
    private lateinit var focusMemory: RecyclerFocusMemory
    private lateinit var statusView: CatalogStatusView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.progressBar.visibility = View.VISIBLE

        val screenState = findNavController().currentBackStackEntry!!.savedStateHandle
        currentCategory = screenState.get<Long>("category") ?: 0L
        currentSort = screenState.get<String>("sort") ?: "default"
        setupRecyclerView()
        focusMemory = RecyclerFocusMemory(this, binding.btnTvCategory)
        focusMemory.track(binding.rvChannels)
        statusView = CatalogStatusView(binding.root) { viewModel.refresh() }
        viewModel.syncError.observe(viewLifecycleOwner) { applyFilters() }
        viewModel.catalogLoading.observe(viewLifecycleOwner) { applyFilters() }
        setupFilterButtons()
        observeData()
    }

    private fun setupFilterButtons() {
        binding.btnTvCategory.setOnClickListener { showCategoryMenu() }
        binding.btnTvSort.setOnClickListener { showSortMenu() }
        binding.btnTvFavorites.text = if (favoritesOnly) "همه کانال‌ها" else "علاقه‌مندی‌ها"
        binding.btnTvFavorites.setOnClickListener {
            favoritesOnly = !favoritesOnly
            binding.btnTvFavorites.text = if (favoritesOnly) "همه کانال‌ها" else "علاقه‌مندی‌ها"
            applyFilters()
        }
    }

    private fun observeData() {
        viewModel.tvCategoriesList.observe(viewLifecycleOwner) { categories ->
            this.tvCategoryList = categories ?: emptyList()
            binding.btnTvCategory.text = tvCategoryList.find { it.id.toLong() == currentCategory }?.name ?: "همه شبکه‌ها"
            binding.btnTvSort.text = if (currentSort == "default") "ترتیب: پیش‌فرض" else "ترتیب: عنوان (الفبا)"
        }

        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            if (channels != null) {
                binding.progressBar.visibility = View.GONE
                applyFilters()

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
        val sortOptions = listOf("ترتیب پیش‌فرض", "عنوان (الفبا)")
        showSortPicker(binding.btnTvSort, if (currentSort == "default") 0 else 1, true) { position ->
            currentSort = if (position == 0) "default" else "alpha"
            binding.btnTvSort.text = "ترتیب: ${sortOptions[position]}"
            applyFilters()
        }
    }

    private fun applyFilters() {
        if (!::statusView.isInitialized) return
        findNavController().currentBackStackEntry?.savedStateHandle?.apply {
            set("category", currentCategory); set("sort", currentSort)
        }
        val allChannels = viewModel.channels.value ?: return

        var filteredList = if (currentCategory == 0L) {
            allChannels
        } else {
            allChannels.filter { it.category == currentCategory }
        }

        if (favoritesOnly) {
            val favorites = requireContext().getSharedPreferences("kelkin_prefs", 0).getStringSet("favorite_channels", emptySet()).orEmpty()
            filteredList = filteredList.filter { it.id.toString() in favorites }
        }
        val persianCollator = Collator.getInstance(Locale("fa"))
        filteredList = if (currentSort == "alpha") {
            filteredList.sortedWith { c1, c2 -> persianCollator.compare(c1.name_fa, c2.name_fa) }
        } else {
            filteredList.sortedBy { it.id }
        }

        channelAdapter.submitList(filteredList) { focusMemory.restore() }
        val empty = filteredList.isEmpty()
        binding.rvChannels.visibility = if (empty) View.GONE else View.VISIBLE
        statusView.show(empty, viewModel.catalogLoading.value == true, viewModel.syncError.value)
        binding.progressBar.visibility = if (empty && viewModel.catalogLoading.value == true) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {


        binding.rvChannels.configureCatalogGrid()
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

    private fun restoreFocus() { focusMemory.restore() }


    override fun onResume() {
        super.onResume()
        if (::channelAdapter.isInitialized) applyFilters()
        (requireActivity() as? MainActivity)?.collapseSidebar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
