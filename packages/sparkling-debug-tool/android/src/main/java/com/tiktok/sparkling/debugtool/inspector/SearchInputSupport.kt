// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.inspector

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.EditText
import com.tiktok.sparkling.debugtool.R

internal object SearchInputSupport {
    @SuppressLint("ClickableViewAccessibility")
    fun install(input: EditText) {
        input.setBackgroundResource(R.drawable.sparkling_chip_bg)
        input.setTextColor(input.context.getColor(R.color.sparkling_panel_text_primary))
        input.setHintTextColor(input.context.getColor(R.color.sparkling_panel_text_hint))
        input.compoundDrawablePadding = dp(input, 4)

        val clearDrawable =
            input.context.getDrawable(R.drawable.sparkling_ic_close)?.mutate()?.apply {
                val size = dp(input, 18)
                setBounds(0, 0, size, size)
                setTint(input.context.getColor(R.color.sparkling_panel_text_hint))
            }

        fun updateClearIcon() {
            input.setCompoundDrawablesRelative(
                input.compoundDrawablesRelative[0],
                null,
                if (input.text.isNullOrEmpty()) null else clearDrawable,
                null,
            )
        }

        input.addTextChangedListener(
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
                    updateClearIcon()
                }
            },
        )
        input.setOnTouchListener { _, event ->
            val drawable = input.compoundDrawablesRelative[2] ?: return@setOnTouchListener false
            if (event.action == MotionEvent.ACTION_UP) {
                val clearStart = input.width - input.paddingEnd - drawable.bounds.width()
                if (event.x >= clearStart) {
                    input.text?.clear()
                    input.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
        updateClearIcon()
    }

    private fun dp(
        input: EditText,
        value: Int,
    ): Int = (value * input.resources.displayMetrics.density).toInt()
}
