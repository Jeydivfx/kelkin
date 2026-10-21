package com.example.kelkin.utils

import android.os.Parcelable
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

/** Stable content identity survives view recreation, sorting, metadata updates and process death. */
interface FocusKeyProvider {
    fun focusKey(position: Int): String?
    fun positionOfKey(key: String): Int
}

class RecyclerFocusMemory(private val fragment: Fragment, private val fallback: View) : DefaultLifecycleObserver {
    private val state = fragment.findNavController().currentBackStackEntry!!.savedStateHandle
    private val lists = mutableListOf<RecyclerView>()
    private var pending = true
    private var restoring = false
    private val root = fragment.requireView()
    private val viewOwner = fragment.viewLifecycleOwner
    private var registeredObserver: ViewTreeObserver? = null
    private val listener = ViewTreeObserver.OnGlobalFocusChangeListener { _, focused ->
        if (!restoring && !pending && focused != null && viewOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            val list = lists.firstOrNull { it.findContainingItemView(focused) != null }
            if (list != null) {
                val item = list.findContainingItemView(focused)!!
                val position = list.getChildAdapterPosition(item)
                val key = (list.adapter as? FocusKeyProvider)?.focusKey(position)
                if (key != null) {
                    state["focus_row"] = list.id
                    state["focus_key_${list.id}"] = key
                    state["focus_index_${list.id}"] = position
                    state["focus_child_${list.id}"] = focused.id
                    pending = false
                }
            } else if (focused === fallback || (focused.id != View.NO_ID || focused.tag is String) && isWithinRoot(focused)) {
                state["focus_control"] = focused.id
                state["focus_control_tag"] = focused.tag as? String
                state["focus_row"] = View.NO_ID
                pending = false
            }
        }
    }
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { restore() }
    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) { attachObservers() }
        override fun onViewDetachedFromWindow(view: View) { detachObservers() }
    }
    init {
        root.addOnAttachStateChangeListener(attachListener)
        if (root.isAttachedToWindow) attachObservers()
        viewOwner.lifecycle.addObserver(this)
    }
    private fun attachObservers() {
        detachObservers()
        registeredObserver = root.viewTreeObserver.also {
            it.addOnGlobalFocusChangeListener(listener)
            it.addOnGlobalLayoutListener(layoutListener)
        }
    }
    private fun detachObservers() {
        registeredObserver?.takeIf { it.isAlive }?.let {
            it.removeOnGlobalFocusChangeListener(listener)
            it.removeOnGlobalLayoutListener(layoutListener)
        }
        registeredObserver = null
    }
    fun track(list: RecyclerView) {
        lists += list
        list.preserveFocusAfterLayout = true
        list.adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        state.get<Parcelable>("layout_${list.id}")?.let { list.layoutManager?.onRestoreInstanceState(it) }
    }
    private fun isWithinRoot(view: View): Boolean {
        var current: android.view.ViewParent? = view.parent
        while (current != null) { if (current === root) return true; current = current.parent }
        return false
    }
    override fun onResume(owner: LifecycleOwner) {
        pending = true
        root.post { restore() }
    }
    override fun onPause(owner: LifecycleOwner) {
        lists.forEach { state["layout_${it.id}"] = it.layoutManager?.onSaveInstanceState() }
    }
    fun restore() {
        // Touch navigation restores scroll state without moving input focus or opening the IME.
        if (!com.example.kelkin.BuildConfig.IS_TV) return
        if (!pending || restoring || !fragment.isResumed || !root.isAttachedToWindow) return
        val row = state.get<Int>("focus_row") ?: View.NO_ID
        val list = lists.firstOrNull { it.id == row }
        if (list != null) {
            val adapter = list.adapter ?: return
            // A restored row may still be hidden while its asynchronous data arrives.
            // Keep the anchor instead of replacing it with the default button.
            if (adapter.itemCount == 0 || !list.isShown || list.isComputingLayout) return
            val key = state.get<String>("focus_key_${list.id}")
            val matched = key?.let { (adapter as? FocusKeyProvider)?.positionOfKey(it) } ?: -1
            val position = if (matched >= 0) matched else (state.get<Int>("focus_index_${list.id}") ?: 0).coerceIn(0, adapter.itemCount - 1)
            val child = list.layoutManager?.findViewByPosition(position)
            if (child == null) { list.scrollToPosition(position); return }
            restoring = true
            val childId = state.get<Int>("focus_child_${list.id}") ?: View.NO_ID
            val target = if (childId != View.NO_ID) child.findViewById<View>(childId) ?: child else child
            pending = !target.requestFocus()
            restoring = false
        } else {
            val control = state.get<String>("focus_control_tag")?.let { root.findViewWithTag<View>(it) }
                ?: state.get<Int>("focus_control")?.takeIf { it != View.NO_ID }?.let { root.findViewById<View>(it) }
            val target = control?.takeIf { it.visibility == View.VISIBLE && it.isFocusable } ?: fallback
            restoring = true
            pending = !target.requestFocus()
            restoring = false
        }
    }
    /** Return from the sidebar without letting spatial focus search choose the hero. */
    fun restoreFromSidebar() {
        pending = true
        restore()
    }
    /** Use only after a definitive data update removed every item from the saved row. */
    fun restoreAfterRemoval(list: RecyclerView) {
        if (list.adapter?.itemCount == 0 && state.get<Int>("focus_row") == list.id) {
            state["focus_row"] = View.NO_ID
            state["focus_control"] = fallback.id
            state["focus_control_tag"] = null as String?
            pending = true
        }
        restore()
    }
    override fun onDestroy(owner: LifecycleOwner) {
        detachObservers()
        root.removeOnAttachStateChangeListener(attachListener)
        lists.forEach { it.adapter = null }
        lists.clear()
    }
}
