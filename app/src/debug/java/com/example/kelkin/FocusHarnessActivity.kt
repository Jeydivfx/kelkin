package com.example.kelkin

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kelkin.utils.FocusKeyProvider
import com.example.kelkin.utils.RecyclerFocusMemory

/** Local test fixture: never included in release builds and never contacts Firebase. */
@android.annotation.SuppressLint("ResourceType")
class FocusHarnessActivity : FragmentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val container = androidx.fragment.app.FragmentContainerView(this).apply { id = 0x71001 }
        setContentView(container)
        run {
            val host = supportFragmentManager.findFragmentById(container.id) as? NavHostFragment ?: NavHostFragment().also {
                supportFragmentManager.beginTransaction().replace(container.id, it).setPrimaryNavigationFragment(it).commitNow()
            }
            val navigator = host.navController.navigatorProvider.getNavigator(FragmentNavigator::class.java)
            val graph = androidx.navigation.NavGraph(androidx.navigation.NavGraphNavigator(host.navController.navigatorProvider)).apply {
                id = 0x71002
                addDestination(navigator.createDestination().apply { id = 0x71003; setClassName(FocusGridFragment::class.java.name) })
                addDestination(navigator.createDestination().apply { id = 0x71004; setClassName(FocusDetailFragment::class.java.name) })
                setStartDestination(0x71003)
            }
            host.navController.graph = graph
        }
    }
}

@android.annotation.SuppressLint("ResourceType")
class FocusGridFragment : Fragment() {
    companion object { var delayNextLoad = false }
    lateinit var list: RecyclerView
    lateinit var cards: FocusCards
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        root.addView(Button(requireContext()).apply { id = 0x71005; text = "Filter" })
        list = RecyclerView(requireContext()).apply { id = 0x71006; layoutManager = GridLayoutManager(context, 4) }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        return root
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        cards = FocusCards { findNavController().navigate(0x71004) }
        list.adapter = cards
        RecyclerFocusMemory(this, view.findViewById(0x71005)).track(list)
        if (delayNextLoad) {
            delayNextLoad = false
            cards.keys = emptyList()
            list.visibility = View.GONE
            list.postDelayed({
                if (this.view != null) {
                    cards.keys = (0 until 60).map { "card-$it" }
                    list.visibility = View.VISIBLE
                    cards.notifyDataSetChanged()
                }
            }, 800)
        }
    }
}
class FocusDetailFragment : Fragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        Button(requireContext()).apply { text = "Back"; setOnClickListener { findNavController().popBackStack() } }
}
class FocusCards(private val click: () -> Unit) : RecyclerView.Adapter<FocusCards.Holder>(), FocusKeyProvider {
    var keys = (0 until 60).map { "card-$it" }
    init { setHasStableIds(true) }
    class Holder(val button: Button) : RecyclerView.ViewHolder(button)
    override fun getItemId(position: Int) = com.example.kelkin.utils.stableContentId(keys[position])
    override fun getItemCount() = keys.size
    override fun focusKey(position: Int) = keys.getOrNull(position)
    override fun positionOfKey(key: String) = keys.indexOf(key)
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(Button(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(-1, 100)
        setOnClickListener { click() }
    })
    override fun onBindViewHolder(holder: Holder, position: Int) { holder.button.text = keys[position]; holder.button.tag = keys[position] }
}
