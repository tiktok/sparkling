// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tiktok.sparkling.Sparkling.Companion.SPARKLING_CONTEXT_CONTAINER_ID
import com.tiktok.sparkling.Sparkling.Companion.SPARKLING_CONTEXT_INIT_DATA
import com.tiktok.sparkling.Sparkling.Companion.SPARKLING_CONTEXT_SCHEME
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import com.tiktok.sparkling.utils.SchemeParser

class SparklingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val containerId = intent.getStringExtra(SPARKLING_CONTEXT_CONTAINER_ID)
        val sparklingContext =
            SparklingContextTransferStation.getSparklingContext(containerId)
                ?: restoreSparklingContext(containerId)
        initStatusBar(sparklingContext)
        setContentView(R.layout.activity_sparkling)
        initToolBar(sparklingContext)
        initSparklingFragment(sparklingContext)
    }

    /**
     * Rebuilds the context for a container that came back without one.
     *
     * [SparklingContextTransferStation] is an in-memory map, so it is empty in a new process,
     * while the task Android restores around it is not. Every container in that task then looks up
     * an id that is no longer there, gets nothing, and renders as a blank page under the default
     * toolbar - the scheme said to hide it, and the scheme was in the context that is gone.
     *
     * [Sparkling.navigate] puts the scheme and the init data on the Intent, which is restored with
     * the task, so they are here to build a context from. What cannot cross the process - the
     * [SparklingContext.sparklingUIProvider] and [SparklingContext.lifecycleDelegate], which are
     * objects the host passed in - does not come back, and the container loads without them.
     *
     * Returns null when there is no scheme to rebuild from, which leaves the previous behaviour
     * for a container that was never opened through [Sparkling.navigate].
     */
    private fun restoreSparklingContext(containerId: String?): SparklingContext? {
        val scheme = intent.getStringExtra(SPARKLING_CONTEXT_SCHEME)
        if (containerId.isNullOrEmpty() || scheme.isNullOrEmpty()) {
            return null
        }
        val restored =
            SparklingContext().apply {
                this.containerId = containerId
                this.scheme = scheme
                intent.getStringExtra(SPARKLING_CONTEXT_INIT_DATA)?.let { withInitData(it) }
                hybridSchemeParam =
                    try {
                        SchemeParser.parseScheme(scheme)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse the restored scheme: ${e.message}")
                        null
                    }
            }
        SparklingContextTransferStation.saveSparklingContext(restored)
        return restored
    }

    private fun initStatusBar(sparklingContext: SparklingContext?) {
        val param = sparklingContext?.hybridSchemeParam ?: return
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        when {
            param.hideStatusBar -> {
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            param.transStatusBar -> {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.TRANSPARENT
            }
        }
    }

    fun initToolBar(sparklingContext: SparklingContext?) {
        val customToolbar = sparklingContext?.sparklingUIProvider?.getToolBar(this)
        if (customToolbar != null) {
            val defaultToolbar = findViewById<Toolbar>(R.id.toolbar)
            val parent = defaultToolbar.parent as? ViewGroup
            parent?.removeView(defaultToolbar)
            parent?.addView(customToolbar, 0)
            setSupportActionBar(customToolbar)
        } else {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = sparklingContext?.hybridSchemeParam?.title ?: getString(R.string.sparkling_page_title)

        val titleColorStr = sparklingContext?.hybridSchemeParam?.titleColor
        if (!titleColorStr.isNullOrEmpty()) {
            try {
                val titleColor = Color.parseColor(titleColorStr)
                val toolbar = (supportActionBar?.customView ?: findViewById<Toolbar>(R.id.toolbar)) as Toolbar
                toolbar.setTitleTextColor(titleColor)

                val customToolbar = sparklingContext?.sparklingUIProvider?.getToolBar(this)
                customToolbar?.setTitleTextColor(titleColor)
            } catch (e: IllegalArgumentException) {
            }
        }

        val navBarColorStr = sparklingContext?.hybridSchemeParam?.navBarColor
        if (!navBarColorStr.isNullOrEmpty()) {
            val navBarColor = ColorUtil.parseColorSafely(navBarColorStr)
            val activeToolbar =
                sparklingContext?.sparklingUIProvider?.getToolBar(this)
                    ?: findViewById<Toolbar>(R.id.toolbar)
            activeToolbar?.setBackgroundColor(navBarColor)
        }

        ((supportActionBar?.customView ?: findViewById<Toolbar>(R.id.toolbar)) as Toolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun initSparklingFragment(sparklingContext: SparklingContext?) {
        sparklingContext?.hybridSchemeParam?.let {
            if (it.hideNavBar || (it.transStatusBar && !it.showNavBarInTransStatusBar)) {
                supportActionBar?.hide()
            }
            requestedOrientation =
                when (it.screenOrientation) {
                    "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
        }

        val fragment = SparklingFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_view_container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
    }

    private var lastBackPressedTime: Long = 0
    private val DOUBLE_CLICK_EXIT_INTERVAL = 2000

    override fun onBackPressed() {
        if (isTaskRoot) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressedTime < DOUBLE_CLICK_EXIT_INTERVAL) {
                super.onBackPressed()
            } else {
                Toast.makeText(this, getString(R.string.click_again_to_exit), Toast.LENGTH_SHORT).show()
                lastBackPressedTime = currentTime
            }
        } else {
            super.onBackPressed()
        }
    }

    private companion object {
        const val TAG = "SparklingActivity"
    }
}
