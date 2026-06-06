// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.inspector

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.tiktok.sparkling.debugtool.R
import com.tiktok.sparkling.debugtool.inspect.GlobalPropsRegistry
import com.tiktok.sparkling.debugtool.ui.applyHalfSheetWindow

/**
 * Unified Sparkling debug inspector. Half-sheet dialog with three tabs
 * (`Console` / `GlobalProps` / `Method`), opened by tapping the debugTag
 * or via [com.tiktok.sparkling.debugtool.SparklingDebugTool.openInspectorPanel].
 *
 * Each tab is a self-contained view that lazily attaches store listeners on
 * window attach, so switching tabs doesn't trigger heavyweight re-creation.
 */
class SparklingInspectorFragment : DialogFragment() {
    enum class Tab { CONSOLE, GLOBAL_PROPS, METHODS }

    private var initialTab: Tab = Tab.CONSOLE
    private var consolePanel: SparklingConsolePanelView? = null
    private var globalPropsPanel: SparklingGlobalPropsPanelView? = null
    private var methodPanel: SparklingMethodPanelView? = null
    private var currentTab: Tab = Tab.CONSOLE

    fun setInitialTab(tab: Tab) {
        initialTab = tab
        if (view != null) selectTab(tab)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tabName = arguments?.getString(ARG_INITIAL_TAB)
        if (tabName != null) {
            runCatching { Tab.valueOf(tabName) }.getOrNull()?.let { initialTab = it }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_sparkling_inspector, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val close = view.findViewById<ImageButton>(R.id.btn_inspector_close)
        close.setOnClickListener { dismissAllowingStateLoss() }
        view.findViewById<TextView>(R.id.txt_inspector_title).text =
            "Sparkling Debug Tool - ${resolvePageName()}"

        val tabConsole = view.findViewById<TextView>(R.id.tab_console)
        val tabProps = view.findViewById<TextView>(R.id.tab_global_props)
        val tabMethods = view.findViewById<TextView>(R.id.tab_methods)
        tabConsole.setOnClickListener { selectTab(Tab.CONSOLE) }
        tabProps.setOnClickListener { selectTab(Tab.GLOBAL_PROPS) }
        tabMethods.setOnClickListener { selectTab(Tab.METHODS) }

        val container = view.findViewById<FrameLayout>(R.id.inspector_content)
        consolePanel =
            SparklingConsolePanelView(requireContext()).also {
                it.layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                container.addView(it)
            }
        globalPropsPanel =
            SparklingGlobalPropsPanelView(requireContext()).also {
                it.layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                container.addView(it)
            }
        methodPanel =
            SparklingMethodPanelView(requireContext()).also {
                it.layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                container.addView(it)
            }
        selectTab(initialTab)
    }

    override fun onStart() {
        super.onStart()
        applyHalfSheetWindow(heightFraction = 0.78f)
    }

    private fun selectTab(tab: Tab) {
        currentTab = tab
        val rootView = view ?: return
        val tabConsole = rootView.findViewById<TextView>(R.id.tab_console)
        val tabProps = rootView.findViewById<TextView>(R.id.tab_global_props)
        val tabMethods = rootView.findViewById<TextView>(R.id.tab_methods)
        tabConsole.isSelected = tab == Tab.CONSOLE
        tabProps.isSelected = tab == Tab.GLOBAL_PROPS
        tabMethods.isSelected = tab == Tab.METHODS

        consolePanel?.visibility = if (tab == Tab.CONSOLE) View.VISIBLE else View.GONE
        globalPropsPanel?.visibility = if (tab == Tab.GLOBAL_PROPS) View.VISIBLE else View.GONE
        methodPanel?.visibility = if (tab == Tab.METHODS) View.VISIBLE else View.GONE
        if (tab == Tab.GLOBAL_PROPS) {
            globalPropsPanel?.refresh()
        }
    }

    private fun resolvePageName(): String {
        val url =
            GlobalPropsRegistry
                .collectAll()
                .firstOrNull()
                ?.templateUrl
                .orEmpty()
        if (url.isBlank()) return "Unknown"
        val fromQuery =
            runCatching { Uri.parse(url).getQueryParameter("bundle") }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        if (fromQuery != null) return fromQuery
        val withoutQuery = url.substringBefore('?').trimEnd('/')
        return withoutQuery.substringAfterLast('/').ifBlank { withoutQuery.ifBlank { "Unknown" } }
    }

    companion object {
        const val FRAGMENT_TAG = "sparkling-inspector-panel"
        private const val ARG_INITIAL_TAB = "sparkling_inspector_initial_tab"

        @JvmStatic
        fun newInstance(initialTab: Tab = Tab.CONSOLE): SparklingInspectorFragment {
            val f = SparklingInspectorFragment()
            f.arguments = Bundle().apply { putString(ARG_INITIAL_TAB, initialTab.name) }
            f.initialTab = initialTab
            return f
        }
    }
}
