// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.floating

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.tiktok.sparkling.debugtool.SparklingDebugTool
import com.tiktok.sparkling.debugtool.inspector.SparklingInspectorFragment
import java.lang.ref.WeakReference

/**
 * Manages the global debug gesture lifecycle. The visual `debugTag` is owned
 * by each SparklingView; this manager only keeps the two-finger long-press
 * fallback entry point for the foreground Activity.
 *
 * The manager is decoupled from any specific Activity; once enabled, it
 * follows the application's foreground activity automatically through
 * [Application.ActivityLifecycleCallbacks].
 */
object SparklingFloatingBallManager {
    /** Optional callbacks that override the default tap / long-press actions. */
    interface ActionHandler {
        /** Called on a tap. Return `true` to consume; default action is skipped. */
        fun onTap(activity: Activity): Boolean = false

        /** Called on long-press. Return `true` to consume; default action is skipped. */
        fun onLongPress(activity: Activity): Boolean = false
    }

    private var enabled = false
    private var attached = false
    private var actionHandler: ActionHandler? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private val debugEntryByActivity = mutableMapOf<Int, DebugEntry>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) = Unit

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityResumed(activity: Activity) {
                currentActivityRef = WeakReference(activity)
                if (enabled) attachTo(activity)
            }

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle,
            ) = Unit

            override fun onActivityDestroyed(activity: Activity) {
                detachFrom(activity)
                if (currentActivityRef?.get() === activity) {
                    currentActivityRef = null
                }
            }
        }

    @JvmStatic
    fun install(application: Application) {
        if (attached) return
        attached = true
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        val current = currentActivityRef?.get() ?: return
        if (enabled) attachTo(current) else detachFrom(current)
    }

    @JvmStatic
    fun isEnabled(): Boolean = enabled

    @JvmStatic
    fun setActionHandler(handler: ActionHandler?) {
        actionHandler = handler
    }

    private fun attachTo(activity: Activity) {
        val key = System.identityHashCode(activity)
        if (debugEntryByActivity.containsKey(key)) return
        val decor = activity.window.decorView
        val container = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val gestureListener = createDebugGestureListener(activity)
        decor.setOnTouchListener(gestureListener)
        container.setOnTouchListener(gestureListener)
        debugEntryByActivity[key] = DebugEntry(container, gestureListener)
    }

    private fun detachFrom(activity: Activity) {
        val key = System.identityHashCode(activity)
        val entry = debugEntryByActivity.remove(key) ?: return
        activity.window.decorView.setOnTouchListener(null)
        entry.container.setOnTouchListener(null)
    }

    private fun handleTap(activity: Activity) {
        val handler = actionHandler
        if (handler != null && handler.onTap(activity)) return
        defaultTap(activity)
    }

    private fun handleLongPress(activity: Activity) {
        val handler = actionHandler
        if (handler != null && handler.onLongPress(activity)) return
        defaultLongPress(activity)
    }

    private fun defaultTap(activity: Activity) {
        val fragmentActivity = activity as? FragmentActivity ?: return
        SparklingDebugTool.openInspectorPanel(
            fragmentActivity,
            SparklingInspectorFragment.Tab.CONSOLE,
        )
    }

    private fun defaultLongPress(activity: Activity) {
        val fragmentActivity = activity as? FragmentActivity ?: return
        SparklingDebugTool.openInspectorPanel(
            fragmentActivity,
            SparklingInspectorFragment.Tab.CONSOLE,
        )
    }

    /** Kept for binary compatibility; SparklingView owns visual visibility. */
    @Suppress("UNUSED")
    @JvmStatic
    fun setVisibilityForCurrentActivity(visible: Boolean) {
        val activity = currentActivityRef?.get() ?: return
        debugEntryByActivity[System.identityHashCode(activity)] ?: return
    }

    private fun createDebugGestureListener(activity: Activity): View.OnTouchListener {
        var pending = false
        var triggered = false
        val trigger =
            Runnable {
                if (pending && !triggered) {
                    triggered = true
                    handleLongPress(activity)
                }
            }
        return View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount >= 2 && !pending) {
                        pending = true
                        triggered = false
                        mainHandler.postDelayed(trigger, 500)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    if (event.pointerCount < 2 || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                        pending = false
                        mainHandler.removeCallbacks(trigger)
                    }
                }
            }
            false
        }
    }

    private data class DebugEntry(
        val container: View,
        val gestureListener: View.OnTouchListener,
    )
}
