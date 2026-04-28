package com.example.kelkin.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kelkin.Adapter.ChannelAdapter
import com.example.kelkin.PlayerActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentTvBinding
import java.text.Collator
import java.util.Locale

class TvFragment : Fragment() {

    private var _binding: FragmentTvBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var channelAdapter: ChannelAdapter

    private var isFirstFocusSet = false

    // وضعیت فیلترها
    private var currentCategory: Long = 0 // 0: همه، 1: ورزشی، 2: ایران، 3: افغانستان
    private var currentSort: String = "default" // default یا alpha

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.progressBar.visibility = View.VISIBLE

        setupRecyclerView()
        setupFilterButtons()
        observeChannels()

        viewModel.fetchChannels()
    }

    private fun setupFilterButtons() {
        binding.btnTvCategory.setOnClickListener { showCategoryMenu() }
        binding.btnTvSort.setOnClickListener { showSortMenu() }

        binding.btnTvFavorites.setOnClickListener {
            // منطق لیست دلخواه تلویزیون
        }
    }

    private fun showCategoryMenu() {
        val categories = listOf("همه شبکه‌ها", "ورزشی", "ایران", "افغانستان")
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val listPopupWindow = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnTvCategory
        listPopupWindow.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories))

        // تنظیمات طلایی برای Android TV
        listPopupWindow.isModal = true

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            currentCategory = position.toLong()
            binding.btnTvCategory.text = categories[position]
            applyFilters()
            listPopupWindow.dismiss()
        }

        listPopupWindow.show()

        // جابجایی فوکوس به داخل لیست
        listPopupWindow.listView?.apply {
            id = R.id.radio_menu_list // استفاده از همان ID عمومی که در ids.xml تعریف کردیم
            requestFocus()
        }
    }

    private fun showSortMenu() {
        val sortOptions = listOf("لیست پیش‌فرض", "ترتیب الفبا")
        val wrapper = ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Widget_AppCompat_PopupMenu)
        val listPopupWindow = ListPopupWindow(wrapper, null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnTvSort
        listPopupWindow.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions))

        listPopupWindow.isModal = true

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            currentSort = if (position == 0) "default" else "alpha"
            binding.btnTvSort.text = sortOptions[position]
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
        val allChannels = viewModel.channels.value ?: return

        // ۱. فیلتر کردن
        var filteredList = allChannels.filter { channel ->
            if (currentCategory == 0L) true else channel.category == currentCategory
        }

        // ۲. مرتب‌سازی
        val persianCollator = Collator.getInstance(Locale("fa"))
        filteredList = if (currentSort == "alpha") {
            filteredList.sortedWith { c1, c2 ->
                persianCollator.compare(c1.name_fa ?: "", c2.name_fa ?: "")
            }
        } else {
            filteredList.sortedBy { it.id }
        }

        // ارسال به آداپتر (تبدیل به لیست جدید برای DiffUtil)
        channelAdapter.submitList(filteredList.toList())
    }

    private fun observeChannels() {
        viewModel.channels.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = View.GONE
            applyFilters()

            if (!it.isNullOrEmpty() && !isFirstFocusSet) {
                setFirstFocus()
            }
        }
    }

    private fun setFirstFocus() {
        binding.rvChannels.postDelayed({
            if (_binding != null) {
                val firstItem = binding.rvChannels.layoutManager?.findViewByPosition(0)
                firstItem?.let {
                    it.requestFocus()
                    isFirstFocusSet = true
                }
            }
        }, 500)
    }

    private fun setupRecyclerView() {
        binding.rvChannels.layoutManager = GridLayoutManager(requireContext(), 6)
        channelAdapter = ChannelAdapter { channel ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("video_url", channel.videoUrl)
                putExtra("channel_name", channel.name_fa)
                putExtra("is_live", true)
            }
            startActivity(intent)
        }
        binding.rvChannels.adapter = channelAdapter
        binding.rvChannels.setHasFixedSize(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}