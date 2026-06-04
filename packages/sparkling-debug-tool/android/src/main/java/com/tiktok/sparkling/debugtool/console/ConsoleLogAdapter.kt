// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.console

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tiktok.sparkling.debugtool.R

/** Recycler adapter that filters/sorts the [ConsoleLogStore] snapshot for display. */
class ConsoleLogAdapter(
    private val context: Context,
) : RecyclerView.Adapter<ConsoleLogAdapter.Holder>() {
    private var rawLogs: List<ConsoleLog> = emptyList()
    private var filtered: List<ConsoleLog> = emptyList()

    var minLevel: Int = Log.VERBOSE
        set(value) {
            if (field != value) {
                field = value
                applyFilter()
            }
        }

    var keyword: String = ""
        set(value) {
            if (field != value) {
                field = value
                applyFilter()
            }
        }

    var reverse: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                applyFilter()
            }
        }

    fun submit(logs: List<ConsoleLog>) {
        rawLogs = logs
        applyFilter()
    }

    private fun applyFilter() {
        val base = rawLogs.filter { it.level >= minLevel && it.matches(keyword) }
        filtered = if (reverse) base.asReversed() else base
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_sparkling_console, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = filtered.size

    override fun onBindViewHolder(
        holder: Holder,
        position: Int,
    ) {
        val log = filtered[position]
        holder.bind(log)
    }

    inner class Holder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.txt_console_message)
        private val copy: ImageButton = itemView.findViewById(R.id.btn_console_copy)

        fun bind(log: ConsoleLog) {
            val color = colorFor(log.level)
            text.setTextColor(color)
            // Wrap by default and cap at 4 lines collapsed; tap toggles full view.
            text.isSingleLine = false
            text.ellipsize = android.text.TextUtils.TruncateAt.END
            text.maxLines = if (log.expanded) Int.MAX_VALUE else 4
            text.text = formatLine(log)
            itemView.setOnClickListener {
                log.expanded = !log.expanded
                notifyItemChanged(bindingAdapterPosition)
            }
            itemView.setOnLongClickListener {
                copyToClipboard(text.text?.toString().orEmpty())
                true
            }
            copy.setOnClickListener { copyToClipboard(text.text?.toString().orEmpty()) }
        }

        private fun formatLine(log: ConsoleLog): String {
            val tagPart = if (log.tag.isNotEmpty()) "[${log.tag}]" else ""
            return "$tagPart${log.message}"
        }
    }

    private fun colorFor(level: Int): Int {
        val resId =
            when (level) {
                Log.DEBUG -> R.color.sparkling_console_debug
                Log.INFO -> R.color.sparkling_console_info
                Log.WARN -> R.color.sparkling_console_warn
                Log.ERROR -> R.color.sparkling_console_error
                else -> R.color.sparkling_console_verbose
            }
        return ContextCompat.getColor(context, resId)
    }

    private fun copyToClipboard(text: String) {
        val cm =
            context.applicationContext
                .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("sparkling-console", text))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
