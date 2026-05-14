// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.utils

import android.os.Handler
import android.os.Looper
import com.tiktok.sparkling.hybridkit.HybridContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * This is used to deal with view events, including viewAppeared, viewDisappeared,
 * viewDisappearedWithType. The reason we converge all the events here is to unify management.*
 */
object ViewEventUtils {
    const val VIEW_APPEARED = "viewAppeared"
    const val VIEW_DISAPPEARED = "viewDisappeared"

    private const val VIEW_DISAPPEARED_WITH_TYPE = "viewDisappearedWithType"
    private const val APP_BACKGROUND = "appResignActive"
    private const val HIDDEN = "covered"
    private const val DESTROY = "destroy"
    private const val TYPE = "type"
    private const val TIME_LIMIT = 500L

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Here we assign each container one state: paused, destroyed, resumed.
     * Paused and destroyed states are the same as Android's definition.
     * Resumed state is only assigned when container is paused before
     */
    private enum class State {
        PAUSED,
        DESTROYED,
        RESUMED,
    }

    private val containerStateMap: ConcurrentHashMap<String, State> = ConcurrentHashMap()

    private var isBackground = false

    fun onPause(hybridContext: HybridContext?) {
        val containerId = hybridContext?.containerId ?: return
        hybridContext.sendEvent(VIEW_DISAPPEARED, null)
        when (containerStateMap[containerId]) {
            State.DESTROYED, State.PAUSED -> {
                return
            }

            State.RESUMED -> {
                containerStateMap[containerId] = State.PAUSED
                return
            }

            else -> {}
        }
        containerStateMap[containerId] = State.PAUSED
        val isBackgroundNow = isBackground

        handler.postDelayed({
            val state = containerStateMap[containerId] ?: return@postDelayed
            when (state) {
                State.PAUSED -> {
                    if (isBackground) {
                        hybridContext.sendEvent(
                            VIEW_DISAPPEARED_WITH_TYPE,
                            JSONObject().apply {
                                put(TYPE, APP_BACKGROUND)
                            },
                        )
                    } else {
                        hybridContext.sendEvent(
                            VIEW_DISAPPEARED_WITH_TYPE,
                            JSONObject().apply {
                                put(TYPE, HIDDEN)
                            },
                        )
                    }
                }

                State.RESUMED -> {
                    if (isBackgroundNow) {
                        hybridContext.sendEvent(
                            VIEW_DISAPPEARED_WITH_TYPE,
                            JSONObject().apply {
                                put(TYPE, APP_BACKGROUND)
                            },
                        )
                    } else {
                        hybridContext.sendEvent(
                            VIEW_DISAPPEARED_WITH_TYPE,
                            JSONObject().apply {
                                put(TYPE, HIDDEN)
                            },
                        )
                    }
                    hybridContext.sendEvent(VIEW_APPEARED, null)
                }

                else -> {}
            }
            containerStateMap.remove(containerId)
        }, TIME_LIMIT)
    }

    fun onDestroy(hybridContext: HybridContext?) {
        val containerId = hybridContext?.containerId ?: return
        containerStateMap[containerId] = State.DESTROYED
        hybridContext.sendEvent(
            VIEW_DISAPPEARED_WITH_TYPE,
            JSONObject().apply {
                put(TYPE, DESTROY)
            },
        )
    }

    fun onShow(hybridContext: HybridContext?) {
        val containerId = hybridContext?.containerId ?: return
        when (containerStateMap[containerId]) {
            State.DESTROYED -> {
                return
            }

            State.PAUSED, State.RESUMED -> {
                containerStateMap[containerId] = State.RESUMED
            }

            else -> {
                hybridContext.sendEvent(VIEW_APPEARED, null)
            }
        }
    }
}
