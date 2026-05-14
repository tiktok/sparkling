// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.content.Intent
import android.util.Log
import com.tiktok.sparkling.utils.SchemeParser

class Sparkling private constructor(
    private val context: Context,
    private val sparklingContext: SparklingContext,
) {
    companion object {
        private const val TAG = "Sparkling"

        const val SPARKLING_CONTEXT_CONTAINER_ID = "SparklingContextContainerId"

        const val TYPE_PAGE = 1
        const val TYPE_POPUP = 2 // not implemented yet
        const val TYPE_CARD = 3

        /**
         * Build a Sparkling instance.
         * @param context Android context (must not be null)
         * @param sparklingContext Sparkling context configuration
         * @return Sparkling instance
         * @throws IllegalArgumentException if context is null
         */
        @JvmStatic
        fun build(
            context: Context?,
            sparklingContext: SparklingContext?,
        ): Sparkling {
            requireNotNull(context) { "Context must not be null" }
            requireNotNull(sparklingContext) { "SparklingContext must not be null" }
            return Sparkling(context, sparklingContext)
        }
    }

    /**
     * Navigate to a Sparkling activity.
     * @return true if navigation was successful, false otherwise
     */
    fun navigate(): Boolean =
        try {
            processSparklingContext(sparklingContext)
            val intent = Intent(context, SparklingActivity::class.java)
            intent.putExtra(SPARKLING_CONTEXT_CONTAINER_ID, sparklingContext.containerId)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            SparklingContextTransferStation.saveSparklingContext(sparklingContext)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate: ${e.message}")
            false
        }

    /**
     * Process the Sparkling context and parse the scheme if available.
     * @param context SparklingContext to process
     */
    fun processSparklingContext(context: SparklingContext) {
        val scheme = context.scheme
        if (!scheme.isNullOrBlank()) {
            try {
                context.hybridSchemeParam = SchemeParser.parseScheme(scheme)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse scheme: ${e.message}")
            }
        }
    }

    /**
     * Create a SparklingView.
     * @param withoutPrepare If true, skip the prepare step
     * @return SparklingView instance, or null if creation fails
     */
    fun createView(withoutPrepare: Boolean = false): SparklingView? =
        try {
            val view = SparklingView(context)
            if (!withoutPrepare) {
                view.prepare(sparklingContext)
            }
            view
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create view: ${e.message}")
            null
        }
}
