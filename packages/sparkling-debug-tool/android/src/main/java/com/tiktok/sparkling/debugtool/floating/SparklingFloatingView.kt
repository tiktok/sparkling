// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.floating

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.min

/**
 * A draggable, edge-snapping floating ball used by the Sparkling debug tool.
 * Adapted from the open-source `StarkFloatingView` reference implementation.
 *
 * The view supports single tap, long-press and drag-to-edge interactions and
 * exposes them through [Listener]. Touch handling is intentionally local so it
 * works equally well when attached to an Activity content view or to a
 * dedicated overlay [android.view.WindowManager] window.
 */
open class SparklingFloatingView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        /** Callbacks for tap and long-press gestures. */
        interface Listener {
            fun onClick(view: SparklingFloatingView)

            fun onLongClick(view: SparklingFloatingView)
        }

        private var listener: Listener? = null

        private var originalRawX = 0f
        private var originalRawY = 0f
        private var originalX = 0f
        private var originalY = 0f
        private var lastTouchDownTime = 0L
        private val moveAnimator = MoveAnimator()
        private val longClickRunnable = LongClickRunnable()
        private val scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        private var screenWidth = 0
        private var screenHeight = 0
        private var statusBarHeight = 0
        private var nearestLeft = true

        /** Snap to the nearest screen edge after the user finishes dragging. */
        var autoToEdge: Boolean = true

        init {
            statusBarHeight = readStatusBarHeight(context)
            isClickable = true
            isLongClickable = true
            updateScreenSize()
        }

        fun setFloatingListener(listener: Listener?) {
            this.listener = listener
        }

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (!isClickable || event == null) return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    captureOriginalTouch(event)
                    updateScreenSize()
                    moveAnimator.stop()
                }

                MotionEvent.ACTION_MOVE -> {
                    updateViewPosition(event)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longClickRunnable.stop()
                    if (autoToEdge) {
                        moveToEdge()
                    }
                    if (isClickEvent()) {
                        listener?.onClick(this)
                    }
                }
            }
            return true
        }

        override fun setVisibility(visibility: Int) {
            super.setVisibility(visibility)
            for (i in 0 until childCount) {
                getChildAt(i).visibility = visibility
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration?) {
            super.onConfigurationChanged(newConfig)
            updateScreenSize()
            if (autoToEdge) {
                moveToEdge(nearestLeft)
            }
        }

        private fun captureOriginalTouch(event: MotionEvent) {
            originalX = x
            originalY = y
            originalRawX = event.rawX
            originalRawY = event.rawY
            lastTouchDownTime = System.currentTimeMillis()
            longClickRunnable.start()
        }

        private fun updateViewPosition(event: MotionEvent) {
            x = originalX + event.rawX - originalRawX
            var desY = originalY + event.rawY - originalRawY
            if (desY < statusBarHeight) desY = statusBarHeight.toFloat()
            if (desY > screenHeight) desY = screenHeight.toFloat()
            y = desY
            if (abs(event.rawX - originalRawX) > scaledTouchSlop ||
                abs(event.rawY - originalRawY) > scaledTouchSlop
            ) {
                longClickRunnable.stop()
            }
        }

        private fun isClickEvent(): Boolean =
            System.currentTimeMillis() - lastTouchDownTime < CLICK_TIME_THRESHOLD &&
                abs(originalX - x) < scaledTouchSlop &&
                abs(originalY - y) < scaledTouchSlop

        @JvmOverloads
        fun moveToEdge(isLeft: Boolean = isNearestLeft()) {
            val targetX = if (isLeft) MARGIN_EDGE.toFloat() else (screenWidth - MARGIN_EDGE).toFloat()
            moveAnimator.start(targetX, y)
        }

        private fun isNearestLeft(): Boolean {
            val middle = screenWidth / 2
            nearestLeft = x < middle
            return nearestLeft
        }

        private fun updateScreenSize() {
            val metrics = context.resources.displayMetrics
            screenWidth = metrics.widthPixels - this.width
            screenHeight = metrics.heightPixels - this.height
        }

        private fun readStatusBarHeight(context: Context): Int {
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        }

        private inner class LongClickRunnable : Runnable {
            private val handler = Handler(Looper.getMainLooper())

            override fun run() {
                listener?.onLongClick(this@SparklingFloatingView)
            }

            fun start() {
                handler.postDelayed(this, ViewConfiguration.getLongPressTimeout().toLong())
            }

            fun stop() {
                handler.removeCallbacks(this)
            }
        }

        private inner class MoveAnimator : Runnable {
            private val handler = Handler(Looper.getMainLooper())
            private var destinationX = 0f
            private var destinationY = 0f
            private var startTime = 0L

            fun start(
                x: Float,
                y: Float,
            ) {
                destinationX = x
                destinationY = y
                startTime = System.currentTimeMillis()
                handler.post(this)
            }

            override fun run() {
                if (rootView == null || rootView.parent == null) return
                val progress = min(1f, (System.currentTimeMillis() - startTime) / 400f)
                val deltaX = (destinationX - x) * progress
                val deltaY = (destinationY - y) * progress
                x += deltaX
                y += deltaY
                if (progress < 1f) {
                    handler.post(this)
                }
            }

            fun stop() {
                handler.removeCallbacks(this)
            }
        }

        companion object {
            private const val MARGIN_EDGE = 0
            private const val CLICK_TIME_THRESHOLD = 200L
        }
    }
