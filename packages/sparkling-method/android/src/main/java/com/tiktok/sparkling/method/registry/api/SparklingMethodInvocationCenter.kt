// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Central hub the Sparkling Bridge implementation uses to broadcast every
 * method invocation lifecycle event so debug tools (or analytics) can observe
 * them without coupling directly to the bridge implementation.
 *
 * The hub is intentionally minimal: payloads are passed as-is, callers are
 * expected to deep-copy or stringify on the listener side if needed.
 */
object SparklingMethodInvocationCenter {
    /**
     * Snapshot describing one method invocation. `id` correlates [onStart]
     * with the matching [onEnd]. Most fields are nullable because earlier
     * lifecycle events may not know them yet.
     */
    data class Event(
        val id: String,
        val name: String,
        val namespace: String?,
        val platform: BridgePlatformType,
        val params: Any?,
        val result: Any?,
        val code: Int?,
        val startTimeMs: Long,
        val endTimeMs: Long?,
    )

    interface Observer {
        /** Invoked on the bridge dispatcher thread before the method runs. */
        fun onStart(event: Event) = Unit

        /** Invoked once the method has produced its final result. */
        fun onEnd(event: Event)
    }

    private val observers = CopyOnWriteArrayList<Observer>()

    @JvmStatic
    fun addObserver(observer: Observer) {
        if (!observers.contains(observer)) observers.add(observer)
    }

    @JvmStatic
    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    @JvmStatic
    fun hasObservers(): Boolean = observers.isNotEmpty()

    /**
     * Notify observers that an invocation has started. The implementation
     * is fire-and-forget: any observer exception is swallowed so the bridge
     * remains unaffected.
     */
    @JvmStatic
    fun notifyStart(
        id: String,
        call: BridgeCall,
        namespace: String?,
        platform: BridgePlatformType,
        startTimeMs: Long,
    ) {
        if (observers.isEmpty()) return
        val event =
            Event(
                id = id,
                name = call.bridgeName,
                namespace = namespace,
                platform = platform,
                params = call.params,
                result = null,
                code = null,
                startTimeMs = startTimeMs,
                endTimeMs = null,
            )
        observers.forEach { runCatching { it.onStart(event) } }
    }

    @JvmStatic
    fun notifyEnd(
        id: String,
        call: BridgeCall,
        namespace: String?,
        platform: BridgePlatformType,
        startTimeMs: Long,
        endTimeMs: Long,
        result: Any?,
        code: Int?,
    ) {
        if (observers.isEmpty()) return
        val event =
            Event(
                id = id,
                name = call.bridgeName,
                namespace = namespace,
                platform = platform,
                params = call.params,
                result = result,
                code = code,
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs,
            )
        observers.forEach { runCatching { it.onEnd(event) } }
    }

    @JvmStatic
    fun notifyNativeToJsEvent(
        name: String,
        params: Any?,
        containerId: String?,
    ) {
        if (observers.isEmpty()) return
        val now = System.currentTimeMillis()
        val event =
            Event(
                id =
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                name = name,
                namespace = "native-to-js",
                platform = BridgePlatformType.LYNX,
                params =
                    mapOf(
                        "event" to name,
                        "params" to params,
                        "containerID" to containerId,
                    ),
                result =
                    mapOf(
                        "status" to "sent",
                        "direction" to "Native -> JS",
                    ),
                code = 1,
                startTimeMs = now,
                endTimeMs = now,
            )
        observers.forEach { runCatching { it.onEnd(event) } }
    }
}
