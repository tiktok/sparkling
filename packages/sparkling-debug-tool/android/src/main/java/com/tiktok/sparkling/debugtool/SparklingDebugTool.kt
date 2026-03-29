// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.lynx.devtool.LynxDevtoolEnv
import com.lynx.service.devtool.LynxDevToolService
import com.lynx.tasm.LynxEnv
import com.lynx.tasm.service.LynxServiceCenter

object SparklingDebugTool {
    private const val PREFS_NAME = "sparkling_debug_tool"
    private const val KEY_DEV_URL = "dev_url"

    @JvmStatic
    fun init(application: Application) {
        // Preset values must be set BEFORE LynxEnv flags so the DevTool
        // service picks them up during initialization.
        LynxDevToolService.INSTANCE.setLynxDebugPresetValue(true)
        LynxDevToolService.INSTANCE.setLogBoxPresetValue(true)
        LynxDevToolService.INSTANCE.setLoadJsBridge(true)

        LynxServiceCenter.inst().registerService(LynxDevToolService.INSTANCE)
        LynxEnv.inst().enableLynxDebug(true)
        LynxEnv.inst().enableDevtool(true)
        LynxEnv.inst().enableLogBox(true)
        LynxDevtoolEnv.inst().enableLongPressMenu(true)
    }

    @JvmStatic
    fun getDevUrl(context: Context, fallback: String): String {
        val stored = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEV_URL, null)
            ?.trim()
        return if (stored.isNullOrEmpty()) fallback else stored
    }

    @JvmStatic
    fun setDevUrl(context: Context, url: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEV_URL, url.trim())
            .apply()
    }

    @JvmStatic
    fun showDevUrlDialog(
        activity: Activity,
        initialUrl: String? = null,
        onSaved: ((String) -> Unit)? = null,
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            activity.runOnUiThread { showDevUrlDialog(activity, initialUrl, onSaved) }
            return
        }

        val input = EditText(activity).apply {
            setText(initialUrl ?: "")
            hint = "http://10.0.2.2:5969/main.lynx.bundle"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSelection(text.length)
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Set Sparkling Dev URL")
            .setMessage("Update the main Lynx bundle URL for debug mode.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val value = input.text?.toString()?.trim().orEmpty()
                if (value.isEmpty()) {
                    Toast.makeText(activity, "Dev URL cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                setDevUrl(activity, value)
                onSaved?.invoke(value)
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
