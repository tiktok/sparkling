// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.inspector

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tiktok.sparkling.debugtool.R
import com.tiktok.sparkling.debugtool.console.ConsoleLogAdapter
import com.tiktok.sparkling.debugtool.console.ConsoleLogStore
import com.tiktok.sparkling.debugtool.console.LynxConsoleLogcatBridge
import com.tiktok.sparkling.debugtool.inspect.SparklingDebugAutoWiring

/**
 * Console-tab body for [SparklingInspectorFragment]. Behaves identically to
 * the standalone `SparklingConsoleFragment` content but is hosted inline so
 * the inspector can swap tabs without recreating dialogs.
 */
internal class SparklingConsolePanelView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : LinearLayout(context, attrs),
        ConsoleLogStore.Listener {
        private val adapter = ConsoleLogAdapter(context)
        private val recycler: RecyclerView
        private var autoScrollToEnd = true

        init {
            orientation = VERTICAL
            setBackgroundResource(R.color.sparkling_console_background)
            LayoutInflater.from(context).inflate(R.layout.view_sparkling_console_panel, this, true)

            recycler = findViewById(R.id.console_recycler)
            recycler.layoutManager = LinearLayoutManager(context)
            recycler.adapter = adapter
            recycler.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(
                        rv: RecyclerView,
                        newState: Int,
                    ) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            val lm = rv.layoutManager as LinearLayoutManager
                            val last = lm.findLastVisibleItemPosition()
                            autoScrollToEnd = last >= adapter.itemCount - 1
                        }
                    }
                },
            )

            val filterInput = findViewById<EditText>(R.id.console_filter_input)
            SearchInputSupport.install(filterInput)
            filterInput.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) = Unit

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) = Unit

                    override fun afterTextChanged(s: Editable?) {
                        adapter.keyword = s?.toString().orEmpty()
                        refresh()
                    }
                },
            )

            findViewById<RadioGroup>(R.id.console_level_group).setOnCheckedChangeListener { _, checkedId ->
                adapter.minLevel =
                    when (checkedId) {
                        R.id.console_level_debug -> Log.DEBUG
                        R.id.console_level_info -> Log.INFO
                        R.id.console_level_warn -> Log.WARN
                        R.id.console_level_error -> Log.ERROR
                        else -> Log.VERBOSE
                    }
                refresh()
            }

            findViewById<View>(R.id.console_clear_button).setOnClickListener {
                ConsoleLogStore.clear()
            }

            adapter.submit(ConsoleLogStore.snapshot())
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            SparklingDebugAutoWiring.ensureConsoleAttachedToLiveKitViews()
            LynxConsoleLogcatBridge.flushRecent()
            ConsoleLogStore.addListener(this)
            refresh()
        }

        override fun onDetachedFromWindow() {
            ConsoleLogStore.removeListener(this)
            super.onDetachedFromWindow()
        }

        override fun onChanged() {
            post { refresh() }
        }

        private fun refresh() {
            adapter.submit(ConsoleLogStore.snapshot())
            if (autoScrollToEnd && adapter.itemCount > 0) {
                recycler.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }
