package com.example.kelkin

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kelkin.Adapter.MovieAdapter
import com.example.kelkin.DataClass.Movie

/** Static visual checks of production layouts, with local sample data only. */
class UiPreviewActivity : AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val layout = intent.getIntExtra("layout", R.layout.fragment_home)
        setContentView(layout)
        if (layout == R.layout.fragment_home) {
            findViewById<TextView>(R.id.hero_title).text = "کلکین؛ پنجره‌ای به داستان‌های تازه"
            findViewById<TextView>(R.id.hero_description).text = "این متن نمونه برای بررسی خوانایی و فاصله‌های صفحهٔ تلویزیون است."
            findViewById<TextView>(R.id.hero_quality).text = "HD"
            findViewById<TextView>(R.id.hero_language).text = "فارسی"
            findViewById<TextView>(R.id.hero_year).text = "۲۰۲۶"
            val cards = (1..12).map { Movie(id = it.toLong(), name_fa = "عنوان نمونهٔ $it", remoteKey = "sample-$it") }
            for (id in listOf(R.id.rv_trending, R.id.rv_continue, R.id.rv_mylist)) {
                findViewById<RecyclerView>(id).apply {
                    visibility = View.VISIBLE
                    layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                    adapter = MovieAdapter {}.also { it.submitList(cards) }
                }
            }
            findViewById<View>(R.id.empty_continue_text).visibility = View.GONE
            findViewById<View>(R.id.empty_mylist_text).visibility = View.GONE
            findViewById<View>(R.id.btn_play).requestFocus()
        } else if (layout == R.layout.activity_login) {
            findViewById<View>(R.id.materialCardGoogle).requestFocus()
        } else {
            findViewById<TextView>(R.id.txtTitle)?.text = "عنوان نمونه برای بررسی صفحهٔ جزئیات کلکین"
            findViewById<TextView>(R.id.txtTitleEnglish)?.text = "Kelkin preview"
        }
    }
}
