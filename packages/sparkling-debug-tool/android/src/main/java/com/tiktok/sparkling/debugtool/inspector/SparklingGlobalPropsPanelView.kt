// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.inspector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tiktok.sparkling.debugtool.R
import com.tiktok.sparkling.debugtool.inspect.GlobalPropsRegistry
import com.tiktok.sparkling.debugtool.inspect.GlobalPropsSnapshot
import org.json.JSONArray
import org.json.JSONObject

/**
 * GlobalProps + queryItems body for [SparklingInspectorFragment]. The
 * `templateUrl` (scheme) for each live LynxView is rendered as a section
 * header so the user gets the scheme info inside the same tab.
 */
internal class SparklingGlobalPropsPanelView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : LinearLayout(context, attrs) {
        private val listView: RecyclerView
        private val emptyView: TextView
        private val filterInput: EditText
        private val adapter = GlobalPropsAdapter()
        private val expandedQueryItems = mutableSetOf<String>()
        private var rawSnapshots: List<GlobalPropsSnapshot> = emptyList()
        private var filterText: String = ""
        private val registryListener: () -> Unit = { post { refresh() } }

        init {
            orientation = VERTICAL
            setBackgroundResource(R.color.sparkling_panel_background)
            LayoutInflater.from(context).inflate(R.layout.view_sparkling_global_props_panel, this, true)

            listView = findViewById(R.id.list_global_props)
            emptyView = findViewById(R.id.txt_global_props_empty)
            filterInput = findViewById(R.id.input_global_props_filter)
            listView.layoutManager = LinearLayoutManager(context)
            listView.adapter = adapter

            findViewById<Button>(R.id.btn_global_props_refresh).setOnClickListener { refresh() }
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
                        filterText = s?.toString().orEmpty()
                        applyFilter()
                    }
                },
            )
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            GlobalPropsRegistry.addListener(registryListener)
            refresh()
        }

        override fun onDetachedFromWindow() {
            GlobalPropsRegistry.removeListener(registryListener)
            super.onDetachedFromWindow()
        }

        fun refresh() {
            rawSnapshots = GlobalPropsRegistry.collectAll().takeLast(1)
            applyFilter()
        }

        private fun applyFilter() {
            val rows = mutableListOf<Row>()
            val needle = filterText.trim().lowercase()
            rawSnapshots.forEach { snapshot ->
                val matchedGlobal = snapshot.globalProps.filter { (k, v) -> matches(k, v, needle) }
                val matchedQuery = snapshot.queryItems.filter { (k, v) -> matches(k, v, needle) }
                if (needle.isNotEmpty() && matchedGlobal.isEmpty() && matchedQuery.isEmpty()) return@forEach

                val containerKey = snapshot.containerId.ifEmpty { snapshot.templateUrl.orEmpty() }
                rows +=
                    Row.Section(
                        title = snapshot.containerId.takeIf { it.isNotEmpty() } ?: "(no container)",
                        subtitle = snapshot.templateUrl ?: "",
                    )
                if (matchedGlobal.isNotEmpty() || matchedQuery.isNotEmpty()) {
                    rows += Row.Header("globalProps")
                    matchedGlobal.entries
                        .sortedBy { it.key }
                        .forEach { (k, v) -> rows += Row.KV(k, formatValue(v)) }
                    if (matchedQuery.isNotEmpty()) {
                        val expanded = containerKey in expandedQueryItems || needle.isNotEmpty()
                        rows +=
                            Row.QueryItems(
                                containerKey = containerKey,
                                count = snapshot.queryItems.size,
                                expanded = expanded,
                            )
                        if (expanded) {
                            matchedQuery.entries
                                .sortedBy { it.key }
                                .forEach { (k, v) -> rows += Row.KV("  $k", formatValue(v)) }
                        }
                    }
                }
            }
            adapter.submit(rows)
            emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }

        private fun matches(
            key: String,
            value: Any?,
            needle: String,
        ): Boolean {
            if (needle.isEmpty()) return true
            if (key.lowercase().contains(needle)) return true
            val v = value?.toString()?.lowercase() ?: return false
            return v.contains(needle)
        }

        private fun formatValue(value: Any?): String =
            when (value) {
                null -> {
                    "null"
                }

                is Map<*, *> -> {
                    try {
                        JSONObject(value).toString(2)
                    } catch (_: Throwable) {
                        value.toString()
                    }
                }

                is List<*> -> {
                    try {
                        JSONArray(value).toString(2)
                    } catch (_: Throwable) {
                        value.toString()
                    }
                }

                is Array<*> -> {
                    value.toList().toString()
                }

                else -> {
                    value.toString()
                }
            }

        private sealed class Row {
            data class Section(
                val title: String,
                val subtitle: String,
            ) : Row()

            data class Header(
                val name: String,
            ) : Row()

            data class KV(
                val key: String,
                val value: String,
            ) : Row()

            data class QueryItems(
                val containerKey: String,
                val count: Int,
                val expanded: Boolean,
            ) : Row()
        }

        private inner class GlobalPropsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private val rows = mutableListOf<Row>()

            fun submit(items: List<Row>) {
                rows.clear()
                rows.addAll(items)
                notifyDataSetChanged()
            }

            override fun getItemViewType(position: Int): Int =
                when (rows[position]) {
                    is Row.Section -> VT_SECTION
                    is Row.Header -> VT_HEADER
                    is Row.QueryItems -> VT_KV
                    is Row.KV -> VT_KV
                }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
            ): RecyclerView.ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                return when (viewType) {
                    VT_SECTION -> SectionHolder(inflater.inflate(R.layout.item_sparkling_global_props_section, parent, false))
                    VT_HEADER -> HeaderHolder(inflater.inflate(R.layout.item_sparkling_global_props_section, parent, false))
                    else -> KVHolder(inflater.inflate(R.layout.item_sparkling_global_props_kv, parent, false))
                }
            }

            override fun getItemCount(): Int = rows.size

            override fun onBindViewHolder(
                holder: RecyclerView.ViewHolder,
                position: Int,
            ) {
                when (val row = rows[position]) {
                    is Row.Section -> {
                        val h = holder as SectionHolder
                        h.title.text = "container: ${row.title}"
                        h.subtitle.visibility = if (row.subtitle.isEmpty()) View.GONE else View.VISIBLE
                        h.subtitle.text = row.subtitle
                        h.itemView.setBackgroundResource(R.color.sparkling_panel_section_bg)
                    }

                    is Row.Header -> {
                        val h = holder as HeaderHolder
                        h.title.text = row.name
                        h.subtitle.visibility = View.GONE
                        h.itemView.setBackgroundResource(R.color.sparkling_panel_section_bg)
                    }

                    is Row.KV -> {
                        val h = holder as KVHolder
                        h.key.text = row.key
                        h.value.text = row.value
                        h.itemView.setOnLongClickListener {
                            val cm = it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("sparkling-prop", "${row.key}=${row.value}"))
                            Toast.makeText(it.context, "Copied", Toast.LENGTH_SHORT).show()
                            true
                        }
                        h.itemView.setOnClickListener(null)
                        h.key.setOnClickListener(null)
                        h.value.setOnClickListener(null)
                    }

                    is Row.QueryItems -> {
                        val h = holder as KVHolder
                        h.key.text = if (row.expanded) "queryItems ▾" else "queryItems ▸"
                        h.value.text = "Array(${row.count})"
                        val toggle =
                            View.OnClickListener {
                                if (row.containerKey in expandedQueryItems) {
                                    expandedQueryItems.remove(row.containerKey)
                                } else {
                                    expandedQueryItems.add(row.containerKey)
                                }
                                applyFilter()
                            }
                        h.itemView.isClickable = true
                        h.itemView.setOnClickListener(toggle)
                        h.key.setOnClickListener(toggle)
                        h.value.setOnClickListener(toggle)
                        h.itemView.setOnLongClickListener(null)
                    }
                }
            }
        }

        private class SectionHolder(
            view: View,
        ) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.txt_section_title)
            val subtitle: TextView = view.findViewById(R.id.txt_section_subtitle)
        }

        private class HeaderHolder(
            view: View,
        ) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.txt_section_title)
            val subtitle: TextView = view.findViewById(R.id.txt_section_subtitle)
        }

        private class KVHolder(
            view: View,
        ) : RecyclerView.ViewHolder(view) {
            val key: TextView = view.findViewById(R.id.txt_kv_key)
            val value: TextView = view.findViewById(R.id.txt_kv_value)
        }

        companion object {
            private const val VT_SECTION = 1
            private const val VT_HEADER = 2
            private const val VT_KV = 3
        }
    }
