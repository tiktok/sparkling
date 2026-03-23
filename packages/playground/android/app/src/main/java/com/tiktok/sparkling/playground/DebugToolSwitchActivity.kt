// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.lynx.devtool.LynxDevtoolEnv
import com.lynx.tasm.LynxEnv

class DebugToolSwitchActivity : AppCompatActivity() {

    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_tool_switch)

        val switchLynxDebug = findViewById<SwitchCompat>(R.id.switch_lynx_debug)
        val switchDevtool = findViewById<SwitchCompat>(R.id.switch_devtool)
        val switchLogbox = findViewById<SwitchCompat>(R.id.switch_logbox)
        val switchLongPress = findViewById<SwitchCompat>(R.id.switch_long_press)
        val btnClose = findViewById<Button>(R.id.btn_debug_tool_close)

        switchLynxDebug.isChecked = prefs.getBoolean(KEY_LYNX_DEBUG, true)
        switchDevtool.isChecked = prefs.getBoolean(KEY_DEVTOOL, true)
        switchLogbox.isChecked = prefs.getBoolean(KEY_LOGBOX, true)
        switchLongPress.isChecked = prefs.getBoolean(KEY_LONG_PRESS, true)

        switchLynxDebug.setOnCheckedChangeListener { _, checked ->
            LynxEnv.inst().enableLynxDebug(checked)
            prefs.edit().putBoolean(KEY_LYNX_DEBUG, checked).apply()
        }
        switchDevtool.setOnCheckedChangeListener { _, checked ->
            LynxEnv.inst().enableDevtool(checked)
            prefs.edit().putBoolean(KEY_DEVTOOL, checked).apply()
        }
        switchLogbox.setOnCheckedChangeListener { _, checked ->
            LynxEnv.inst().enableLogBox(checked)
            prefs.edit().putBoolean(KEY_LOGBOX, checked).apply()
        }
        switchLongPress.setOnCheckedChangeListener { _, checked ->
            LynxDevtoolEnv.inst().enableLongPressMenu(checked)
            prefs.edit().putBoolean(KEY_LONG_PRESS, checked).apply()
        }

        btnClose.setOnClickListener { finish() }
    }

    companion object {
        private const val PREFS_NAME = "sparkling_debug_tool_switches"
        private const val KEY_LYNX_DEBUG = "lynx_debug"
        private const val KEY_DEVTOOL = "devtool"
        private const val KEY_LOGBOX = "logbox"
        private const val KEY_LONG_PRESS = "long_press_menu"
    }
}
