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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tiktok.sparkling.debugtool.R
import com.tiktok.sparkling.debugtool.inspect.MethodInvocation
import com.tiktok.sparkling.debugtool.inspect.MethodInvocationStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Sparkling Method invocation list body for [SparklingInspectorFragment]. */
internal class SparklingMethodPanelView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : LinearLayout(context, attrs),
        MethodInvocationStore.Listener {
        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        private val adapter = MethodAdapter()
        private val expanded = HashSet<String>()
        private var filter = ""

        private val listView: RecyclerView
        private val emptyView: TextView

        init {
            orientation = VERTICAL
            setBackgroundResource(R.color.sparkling_panel_background)
            LayoutInflater.from(context).inflate(R.layout.view_sparkling_method_panel, this, true)

            listView = findViewById(R.id.list_method_invocations)
            emptyView = findViewById(R.id.txt_method_empty)
            listView.layoutManager = LinearLayoutManager(context)
            listView.adapter = adapter

            findViewById<View>(R.id.btn_method_clear).setOnClickListener { MethodInvocationStore.clear() }

            val filterInput = findViewById<EditText>(R.id.input_method_filter)
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
                        filter =
                            s
                                ?.toString()
                                .orEmpty()
                                .trim()
                                .lowercase()
                        refresh()
                    }
                },
            )
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            MethodInvocationStore.addListener(this)
            refresh()
        }

        override fun onDetachedFromWindow() {
            MethodInvocationStore.removeListener(this)
            super.onDetachedFromWindow()
        }

        override fun onMethodInvocationsChanged() {
            post { refresh() }
        }

        private fun refresh() {
            val rows =
                MethodInvocationStore.snapshot().asReversed().filter {
                    filter.isEmpty() ||
                        it.name.lowercase().contains(filter) ||
                        it.namespace?.lowercase()?.contains(filter) == true
                }
            adapter.submit(rows)
            emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }

        private inner class MethodAdapter : RecyclerView.Adapter<MethodHolder>() {
            private val items = mutableListOf<MethodInvocation>()

            fun submit(list: List<MethodInvocation>) {
                items.clear()
                items.addAll(list)
                notifyDataSetChanged()
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
            ): MethodHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_sparkling_method_invocation, parent, false)
                return MethodHolder(v)
            }

            override fun getItemCount(): Int = items.size

            override fun onBindViewHolder(
                holder: MethodHolder,
                position: Int,
            ) {
                val record = items[position]
                holder.name.text = record.name
                val isExpanded = expanded.contains(record.id)

                holder.duration.text =
                    when (record.endTimeMs) {
                        null -> "running"
                        else -> "${record.durationMs ?: 0}ms"
                    }
                val time = timeFormat.format(Date(record.startTimeMs))
                val meta =
                    buildString {
                        append(time)
                        append(" · ")
                        append(record.platform)
                        if (!record.namespace.isNullOrEmpty()) {
                            append(" · ")
                            append(record.namespace)
                        }
                        if (record.code != null) {
                            append(" · code=").append(record.code)
                        }
                    }
                holder.meta.text = meta

                val statusText: String
                val statusBg: Int
                when {
                    record.endTimeMs == null -> {
                        statusText = "·"
                        statusBg = 0xFFFFC857.toInt()
                    }

                    record.success == false || (record.code != null && record.code != 1) -> {
                        statusText = "ERR"
                        statusBg = 0xFFE5484D.toInt()
                    }

                    else -> {
                        statusText = "OK"
                        statusBg = 0xFF30A46C.toInt()
                    }
                }
                holder.status.text = statusText
                holder.status.setBackgroundColor(statusBg)

                holder.payload.visibility = if (isExpanded) View.VISIBLE else View.GONE
                if (isExpanded) {
                    holder.payload.text =
                        buildString {
                            append("» params\n")
                            append(record.params.ifBlank { "(empty)" })
                            append("\n\n« result\n")
                            append(record.result?.ifBlank { "(empty)" } ?: "(pending)")
                        }
                }

                holder.itemView.setOnClickListener {
                    if (isExpanded) expanded.remove(record.id) else expanded.add(record.id)
                    notifyItemChanged(position)
                }
                holder.itemView.setOnLongClickListener {
                    copyRecord(it, record)
                    true
                }
                holder.copy.setOnClickListener { copyRecord(it, record) }
            }

            private fun copyRecord(
                view: View,
                record: MethodInvocation,
            ) {
                val cm = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("sparkling-method", "${record.name}\n${record.params}\n${record.result.orEmpty()}"))
                Toast.makeText(view.context, "Copied", Toast.LENGTH_SHORT).show()
            }
        }

        private class MethodHolder(
            view: View,
        ) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.txt_method_name)
            val duration: TextView = view.findViewById(R.id.txt_method_duration)
            val meta: TextView = view.findViewById(R.id.txt_method_meta)
            val payload: TextView = view.findViewById(R.id.txt_method_payload)
            val status: TextView = view.findViewById(R.id.txt_method_status)
            val copy: ImageButton = view.findViewById(R.id.btn_method_copy)
        }
    }
