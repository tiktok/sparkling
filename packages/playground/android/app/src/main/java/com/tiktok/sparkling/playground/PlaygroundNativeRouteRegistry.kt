// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License, Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.playground

import android.content.Context
import android.content.Intent

/**
 * Playground-only registry for `router.open({ scheme: "hybrid://native_route=<id>" })`.
 * Add new native pages by calling [register] (e.g. from Application) or by extending [installDefaultRoutes].
 */
object PlaygroundNativeRouteRegistry {

    const val NATIVE_ROUTE_SCHEME_PREFIX = "hybrid://native_route="

    private val handlers = mutableMapOf<String, (Context) -> Unit>()

    init {
        installDefaultRoutes()
    }

    fun installDefaultRoutes() {
        register("card_demo") { ctx ->
            ctx.startActivity(
                Intent(ctx, CardViewDemoActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
        register("debug_tool_switch") { ctx ->
            ctx.startActivity(
                Intent(ctx, DebugToolSwitchActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    fun register(routeId: String, handler: (Context) -> Unit) {
        handlers[routeId] = handler
    }

    fun unregister(routeId: String) {
        handlers.remove(routeId)
    }

    /**
     * @return true if [scheme] is a native-route URL and the route id was registered (even if [context] is null).
     */
    fun openIfMatches(scheme: String, context: Context?): Boolean {
        if (!scheme.startsWith(NATIVE_ROUTE_SCHEME_PREFIX)) {
            return false
        }
        val routeId = scheme.removePrefix(NATIVE_ROUTE_SCHEME_PREFIX).substringBefore("&")
        val handler = handlers[routeId] ?: return false
        context?.let { handler(it) }
        return true
    }
}
