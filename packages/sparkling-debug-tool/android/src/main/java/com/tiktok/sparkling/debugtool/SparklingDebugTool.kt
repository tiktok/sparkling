// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.lynx.devtool.LynxDevtoolEnv
import com.lynx.service.devtool.LynxDevToolService
import com.lynx.service.log.LynxLogService
import com.lynx.tasm.LynxEnv
import com.lynx.tasm.LynxView
import com.lynx.tasm.service.LynxServiceCenter
import com.tiktok.sparkling.debugtool.console.ConsoleLog
import com.tiktok.sparkling.debugtool.console.ConsoleLogStore
import com.tiktok.sparkling.debugtool.console.LynxConsoleLogDelegate
import com.tiktok.sparkling.debugtool.console.LynxConsoleLogcatBridge
import com.tiktok.sparkling.debugtool.console.LynxConsoleSink
import com.tiktok.sparkling.debugtool.floating.SparklingFloatingBallManager
import com.tiktok.sparkling.debugtool.inspect.GlobalPropsRegistry
import com.tiktok.sparkling.debugtool.inspect.GlobalPropsSnapshot
import com.tiktok.sparkling.debugtool.inspect.MethodInvocation
import com.tiktok.sparkling.debugtool.inspect.MethodInvocationStore
import com.tiktok.sparkling.debugtool.inspect.SparklingDebugAutoWiring
import com.tiktok.sparkling.debugtool.inspector.SparklingInspectorFragment

/**
 * Entry point for the Sparkling debug tool. The host typically only needs to
 * call [init] once during `Application.onCreate`; everything else
 * (Lynx debug flags, JS console capture, GlobalProps tracking, Sparkling
 * Method observation, debugTag entry) is wired up automatically when
 * `enableFloatingBall = true`.
 */
object SparklingDebugTool {
    data class Config(
        /**
         * When `true` the host app is expected to call [attachConsoleSink] for each
         * created `LynxView` so JS console messages are captured. When the
         * debugTag entry is enabled the debug tool wires this up itself via
         * `KitViewManager`, so this flag is mainly here for explicit override.
         */
        val enableJsConsole: Boolean = false,
        val enableInNonDebuggableApp: Boolean = false,
        /** Maximum number of console messages kept in the in-memory ring buffer. */
        val consoleBufferSize: Int = 300,
        /**
         * When `true` SparklingView shows its bottom-left debugTag entry. Tapping
         * the tag opens the unified inspector; turning this on also auto-enables
         * every Lynx debug capability and wires up the
         * console / globalProps / method tracers.
         */
        val enableFloatingBall: Boolean = false,
    )

    /**
     * @deprecated Switches are no longer surfaced in the inspector. The tool
     * applies the all-on configuration whenever the debugTag entry is enabled.
     * The class is kept for binary compatibility with hosts that still call
     * [getFlags] / [setFlags] directly.
     */
    data class DebugFlags(
        val lynxDebugEnabled: Boolean = true,
        val devtoolEnabled: Boolean = true,
        val logboxEnabled: Boolean = true,
        val longPressMenuEnabled: Boolean = true,
    )

    private const val PREFS_NAME = "sparkling_debug_tool"
    private const val KEY_DEV_URL = "dev_url"
    private const val KEY_LYNX_DEBUG = "lynx_debug"
    private const val KEY_DEVTOOL = "devtool"
    private const val KEY_LOGBOX = "logbox"
    private const val KEY_LONG_PRESS = "long_press_menu"

    private var currentConfig = Config()

    @JvmStatic
    fun init(
        application: Application,
        config: Config = Config(),
    ) {
        currentConfig = config
        ConsoleLogStore.setCapacity(config.consoleBufferSize)
        if (!isDebuggableApp(application) && !config.enableInNonDebuggableApp) {
            return
        }
        // Always apply the all-on debug flag set when running under the debug
        // tool: switches are no longer user-visible. Hosts that need a custom
        // subset can still call [setFlags] explicitly.
        applyFlags(DebugFlags())
        SparklingFloatingBallManager.install(application)
        SparklingFloatingBallManager.setEnabled(config.enableFloatingBall)
        if (config.enableFloatingBall || config.enableJsConsole) {
            LynxConsoleLogDelegate.installOnce()
            LynxConsoleLogcatBridge.startOnce()
            SparklingDebugAutoWiring.installOnce()
        }
    }

    /** Show or hide the bottom-left debugTag entry at runtime. */
    @JvmStatic
    fun setFloatingBallEnabled(enabled: Boolean) {
        SparklingFloatingBallManager.setEnabled(enabled)
        if (enabled) {
            applyFlags(DebugFlags())
            LynxConsoleLogDelegate.installOnce()
            LynxConsoleLogcatBridge.startOnce()
            SparklingDebugAutoWiring.installOnce()
        }
    }

    /** Legacy hook kept for compatibility; debugTag clicks use SparklingView's default action. */
    @JvmStatic
    fun setFloatingBallActionHandler(handler: SparklingFloatingBallManager.ActionHandler?) {
        SparklingFloatingBallManager.setActionHandler(handler)
    }

    /**
     * Build a fresh inspector fragment. Equivalent to invoking the debugTag's
     * default tap action; useful for hosts that want to surface the
     * inspector via a menu / button.
     */
    @JvmStatic
    fun createFragment(): DialogFragment = SparklingInspectorFragment.newInstance()

    @JvmStatic
    fun getFlags(context: Context): DebugFlags = resolveFlags(context)

    @JvmStatic
    fun setFlags(
        context: Context,
        flags: DebugFlags,
    ) {
        prefs(context)
            .edit()
            .putBoolean(KEY_LYNX_DEBUG, flags.lynxDebugEnabled)
            .putBoolean(KEY_DEVTOOL, flags.devtoolEnabled)
            .putBoolean(KEY_LOGBOX, flags.logboxEnabled)
            .putBoolean(KEY_LONG_PRESS, flags.longPressMenuEnabled)
            .apply()
        if (isDebuggableApp(context) || currentConfig.enableInNonDebuggableApp) {
            applyFlags(flags)
        }
    }

    @JvmStatic
    fun getDevUrl(
        context: Context,
        fallback: String,
    ): String {
        val stored = prefs(context).getString(KEY_DEV_URL, null)?.trim()
        return if (stored.isNullOrEmpty()) fallback else stored
    }

    @JvmStatic
    fun setDevUrl(
        context: Context,
        url: String,
    ) {
        prefs(context)
            .edit()
            .putString(KEY_DEV_URL, url.trim())
            .apply()
    }

    @JvmStatic
    fun isJsConsoleEnabled(): Boolean = currentConfig.enableJsConsole

    /**
     * Hook the Lynx inspector console delegate so that every `console.*` call from
     * the JS context is captured into the shared [ConsoleLogStore]. The debug
     * tool already wires this up automatically via `KitViewManager` when the
     * debugTag entry is enabled; this entry point is kept for hosts that don't
     * use `KitViewManager`.
     *
     * Returns `true` if the delegate was successfully installed.
     */
    @JvmStatic
    fun attachConsoleSink(lynxView: LynxView): Boolean = LynxConsoleSink.attach(lynxView)

    @JvmStatic
    fun detachConsoleSink(lynxView: LynxView) {
        LynxConsoleSink.detach(lynxView)
    }

    /** Open the unified inspector dialog with the given initial tab. */
    @JvmStatic
    @JvmOverloads
    fun openInspectorPanel(
        activity: FragmentActivity,
        initialTab: SparklingInspectorFragment.Tab = SparklingInspectorFragment.Tab.CONSOLE,
    ) {
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(SparklingInspectorFragment.FRAGMENT_TAG) != null) return
        SparklingInspectorFragment
            .newInstance(initialTab)
            .show(fm, SparklingInspectorFragment.FRAGMENT_TAG)
    }

    /** Open the inspector with the Console tab pre-selected. */
    @JvmStatic
    fun openConsolePanel(activity: FragmentActivity) {
        openInspectorPanel(activity, SparklingInspectorFragment.Tab.CONSOLE)
    }

    /**
     * Open the inspector with the Sparkling Method invocations tab
     * pre-selected.
     */
    @JvmStatic
    fun openMethodInvocationPanel(activity: FragmentActivity) {
        openInspectorPanel(activity, SparklingInspectorFragment.Tab.METHODS)
    }

    /** Open the inspector with the GlobalProps tab pre-selected. */
    @JvmStatic
    fun openGlobalPropsPanel(activity: FragmentActivity) {
        openInspectorPanel(activity, SparklingInspectorFragment.Tab.GLOBAL_PROPS)
    }

    /** Append one method invocation record (already-formatted strings). */
    @JvmStatic
    fun recordMethodInvocation(record: MethodInvocation) {
        MethodInvocationStore.add(record)
    }

    /** Replace an in-flight invocation record (matched by id). */
    @JvmStatic
    fun updateMethodInvocation(record: MethodInvocation) {
        MethodInvocationStore.update(record)
    }

    @JvmStatic
    fun clearMethodInvocations() {
        MethodInvocationStore.clear()
    }

    /**
     * Override the GlobalProps supplier. The debug tool registers a default
     * provider that walks `KitViewManager` reflectively when `init` is called
     * with `enableFloatingBall = true`; hosts only need to call this if they
     * want to expose a different data source.
     */
    @JvmStatic
    fun setGlobalPropsProvider(provider: GlobalPropsRegistry.Provider?) {
        GlobalPropsRegistry.setProvider(provider)
    }

    /**
     * Push one externally-collected console line (e.g. from the host
     * `HybridLogger`) into the shared store. The native panel will display it
     * alongside JS console messages captured via [attachConsoleSink].
     */
    @JvmStatic
    fun recordConsoleLine(
        level: String,
        tag: String,
        message: String,
    ) {
        ConsoleLogStore.add(ConsoleLog.fromPlain(level, tag, message))
    }

    /**
     * Snapshot helper kept for backwards compatibility with previous tooling that
     * rendered the console as a single text dump.
     */
    @JvmStatic
    fun getConsoleLines(): List<String> = ConsoleLogStore.snapshot().map { "[${it.type}] ${if (it.tag.isEmpty()) "" else it.tag + ": "}${it.message}" }

    @JvmStatic
    fun clearConsoleLines() {
        ConsoleLogStore.clear()
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

        val input =
            EditText(activity).apply {
                setText(initialUrl ?: "")
                hint = "http://127.0.0.1:5969/main.lynx.bundle"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                setSelection(text.length)
            }

        val dialog =
            AlertDialog
                .Builder(activity)
                .setTitle("Set Sparkling Dev URL")
                .setMessage("Update the main Lynx bundle URL for debug mode.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val value =
                    input.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()
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

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun isDebuggableApp(context: Context): Boolean = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun resolveFlags(context: Context): DebugFlags {
        val prefs = prefs(context)
        return DebugFlags(
            lynxDebugEnabled = prefs.getBoolean(KEY_LYNX_DEBUG, true),
            devtoolEnabled = prefs.getBoolean(KEY_DEVTOOL, true),
            logboxEnabled = prefs.getBoolean(KEY_LOGBOX, true),
            longPressMenuEnabled = prefs.getBoolean(KEY_LONG_PRESS, true),
        )
    }

    private fun applyFlags(flags: DebugFlags) {
        LynxDevToolService.INSTANCE.setLynxDebugPresetValue(flags.lynxDebugEnabled)
        LynxDevToolService.INSTANCE.setLogBoxPresetValue(flags.logboxEnabled)
        LynxDevToolService.INSTANCE.setLoadQJSBridge(flags.devtoolEnabled)

        LynxServiceCenter.inst().registerService(LynxLogService)
        LynxServiceCenter.inst().registerService(LynxDevToolService.INSTANCE)

        LynxEnv.inst().enableLynxDebug(flags.lynxDebugEnabled)
        LynxEnv.inst().enableDevtool(flags.devtoolEnabled)
        LynxEnv.inst().enableLogBox(flags.logboxEnabled)
        LynxDevtoolEnv.inst().enableLongPressMenu(flags.longPressMenuEnabled)
    }
}
