// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Intent
import android.util.Log
import com.tiktok.sparkling.Sparkling.Companion.SPARKLING_CONTEXT_CONTAINER_ID
import com.tiktok.sparkling.Sparkling.Companion.SPARKLING_CONTEXT_INIT_DATA
import com.tiktok.sparkling.Sparkling.Companion.SPARKLING_CONTEXT_SCHEME
import com.tiktok.sparkling.utils.SchemeParser

/**
 * Where a container's context waits between being built and being shown.
 *
 * The map is in memory, and that is the whole problem it also has to solve: it
 * dies with the process, while the task does not. When Android reclaims a
 * backgrounded app and the person comes back to it, the activity is recreated
 * from the task record - the same Intent, a new process, an empty map. Looking
 * the context up by id then answered null and the container rendered nothing,
 * which is the blank "Sparkling Page" a hot start used to land on.
 *
 * So the Intent carries what a context can be rebuilt from, and [restore] uses
 * it when the map cannot answer.
 */
object SparklingContextTransferStation {
    private const val TAG = "SparklingTransfer"

    private val sparklingContextMap = mutableMapOf<String, SparklingContext>()

    /**
     * Called with a context rebuilt after the process was restarted.
     *
     * A restored context carries what the Intent could hold - its id, its scheme
     * and its initial data - and not what only the host could give it: the UI
     * provider, the lifecycle delegate, the view-created listener. A host that
     * sets those puts them back here, and gets a restored page that behaves like
     * the one the person left rather than a plainer version of it.
     */
    @Volatile
    @JvmStatic
    var onRestore: ((SparklingContext) -> Unit)? = null

    fun saveSparklingContext(context: SparklingContext) {
        sparklingContextMap[context.containerId] = context
    }

    fun getSparklingContext(containerId: String?): SparklingContext? = sparklingContextMap[containerId]

    fun releaseSparklingContext(containerId: String?) {
        sparklingContextMap.remove(containerId)
    }

    /**
     * The context for an Intent, rebuilding it if the process has restarted.
     *
     * Returns null only when there is genuinely nothing to show - an Intent from
     * a build before the scheme travelled in it, or one that never named a
     * container - so a caller can close the container instead of presenting an
     * empty one.
     */
    @JvmStatic
    fun restore(intent: Intent?): SparklingContext? {
        val containerId = intent?.getStringExtra(SPARKLING_CONTEXT_CONTAINER_ID)
        getSparklingContext(containerId)?.let { return it }

        val scheme = intent?.getStringExtra(SPARKLING_CONTEXT_SCHEME)
        if (containerId.isNullOrBlank() || scheme.isNullOrBlank()) {
            Log.w(TAG, "No context for container $containerId and nothing in the Intent to rebuild it from")
            return null
        }

        val rebuilt =
            SparklingContext().apply {
                this.containerId = containerId
                this.scheme = scheme
                intent.getStringExtra(SPARKLING_CONTEXT_INIT_DATA)?.let { withInitData(it) }
                hybridSchemeParam =
                    runCatching { SchemeParser.parseScheme(scheme) }
                        .onFailure { Log.w(TAG, "Could not parse the restored scheme: ${it.message}") }
                        .getOrNull()
            }

        if (rebuilt.hybridSchemeParam == null) {
            Log.w(TAG, "Restored scheme names no container this SDK can render: $scheme")
            return null
        }

        saveSparklingContext(rebuilt)
        runCatching { onRestore?.invoke(rebuilt) }
            .onFailure { Log.w(TAG, "onRestore threw: ${it.message}") }
        Log.i(TAG, "Rebuilt the context for container $containerId after a process restart")
        return rebuilt
    }

    @JvmStatic
    internal fun clearAllContexts() {
        sparklingContextMap.clear()
    }
}
