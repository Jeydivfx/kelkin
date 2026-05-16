package com.example.kelkin.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kelkin.Adapter.RadioAdapter
import com.example.kelkin.PlayerActivity
import com.example.kelkin.R
import com.example.kelkin.ViewModels.HomeViewModel
import com.example.kelkin.databinding.FragmentRadioBinding
import java.text.Collator
import java.util.Locale

class RadioFragment : Fragment() {

    private var _binding: FragmentRadioBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var radioAdapter: RadioAdapter

    private var isFirstFocusSet = false

    // وضعیت فیلترها (دقیقا مثل بخش تی‌وی)
    private var currentCategory: Long = 0 // 0: همه، 1: موسیقی، 2: خبری، 3: مذهبی
    private var currentSort: String = "default" // default یا alpha

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRadioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        // نمایش لودینگ طلایی رادیو در شروع
        binding.progressBarRadio.visibility = View.VISIBLE

        setupRecyclerView()
        setupFilterButtons()
        observeRadioStations()

        viewModel.fetchRadios()
    }

    private fun setupFilterButtons() {
        // ۱. دکمه دسته‌بندی رادیوها
        binding.btnRadioCategory.setOnClickListener { showCategoryMenu() }

        // ۲. دکمه مرتب‌سازی
        binding.btnRadioSort.setOnClickListener { showSortMenu() }

        // ۳. دکمه علاقه‌مندی‌ها
        binding.btnRadioFavorites.setOnClickListener {
            // منطق لیست دلخواه رادیو اینجا قرار می‌گیرد
        }
    }

    private fun showCategoryMenu() {
        val categories = listOf("همه ایستگاه‌ها", "موسیقی", "خبری", "ورزشی")

        // استفاده از تم استایل در کانتکست برای نمایش درست در تی‌وی
        val listPopupWindow = androidx.appcompat.widget.ListPopupWindow(requireContext(), null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnRadioCategory
        listPopupWindow.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories))

        // تنظیمات حیاتی برای Android TV
        listPopupWindow.isModal = true // مسدود کردن لایه‌های زیرین
        listPopupWindow.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            currentCategory = position.toLong()
            binding.btnRadioCategory.text = categories[position]
            applyFilters()
            listPopupWindow.dismiss() // بستن منو بعد از انتخاب
        }

        listPopupWindow.show()

        // انتقال فوکوس به لیست باز شده
        listPopupWindow.listView?.apply {
            // اختصاص آیدی که در مرحله قبل تعریف کردیم (اختیاری)
            this.id = R.id.radio_menu_list

            // تنظیمات برای اینکه کلید OK ریموت کار کنه
            this.isFocusable = true
            this.isFocusableInTouchMode = true
            this.requestFocus()
        }
    }

    private fun showSortMenu() {
        val sortOptions = listOf("لیست پیش‌فرض", "ترتیب الفبا")
        val listPopupWindow = androidx.appcompat.widget.ListPopupWindow(requireContext(), null, androidx.appcompat.R.attr.listPopupWindowStyle)

        listPopupWindow.anchorView = binding.btnRadioSort
        listPopupWindow.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions))

        listPopupWindow.isModal = true

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            // تغییر وضعیت بر اساس انتخاب کاربر
            currentSort = if (position == 0) "default" else "alpha"

            // آپدیت متن دکمه برای بازخورد به کاربر
            binding.btnRadioSort.text = sortOptions[position]

            // اجرای عملیات فیلتر و مرتب‌سازی
            applyFilters()

            listPopupWindow.dismiss()
        }

        listPopupWindow.show()
        listPopupWindow.listView?.requestFocus()
    }

    private fun applyFilters() {
        val allStations = viewModel.radioStations.value ?: return

        // ۱. فیلتر کردن بر اساس دسته
        var filteredList = allStations.filter { station ->
            // اگه 0 (همه) انتخاب شده بود، همه رو برگردون، در غیر این صورت مقایسه کن
            if (currentCategory == 0L) true else station.category == currentCategory
        }

        // ۲. مرتب‌سازی هوشمند فارسی

        val persianCollator = Collator.getInstance(Locale("fa"))
        if (currentSort == "alpha") {
            filteredList = filteredList.sortedWith { r1, r2 -> persianCollator.compare(r1.name_fa, r2.name_fa) }
        }

        radioAdapter.submitList(filteredList)
    }

    private fun observeRadioStations() {
        viewModel.radioStations.observe(viewLifecycleOwner) { stations ->
            binding.progressBarRadio.visibility = View.GONE
            applyFilters()

            if (stations != null && stations.isNotEmpty() && !isFirstFocusSet) {
                setFirstFocus()
            }
        }
    }

    private fun setFirstFocus() {
        binding.rvRadioStations.postDelayed({
            if (_binding != null) {
                val firstItem = binding.rvRadioStations.layoutManager?.findViewByPosition(0)
                firstItem?.let {
                    it.requestFocus()
                    isFirstFocusSet = true
                }
            }
        }, 500)
    }

    private fun setupRecyclerView() {
        // اینجا هم مثل تی‌وی روی ۶ یا ۵ ست کن (هرجور راحتی)
        binding.rvRadioStations.layoutManager = GridLayoutManager(requireContext(), 5)
        radioAdapter = RadioAdapter { radio ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("video_url", radio.videoUrl)
                putExtra("channel_name", radio.name_fa)
                putExtra("is_live", true)
            }
            startActivity(intent)
        }
        binding.rvRadioStations.adapter = radioAdapter
        binding.rvRadioStations.setHasFixedSize(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}