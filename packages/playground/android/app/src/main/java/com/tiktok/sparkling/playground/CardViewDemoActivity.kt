// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingView

class CardViewDemoActivity : AppCompatActivity() {

    private var sparklingView: SparklingView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_view_demo)

        val container = findViewById<FrameLayout>(R.id.card_view_demo_container)
        val btnShowCard = findViewById<Button>(R.id.btn_show_card)
        val btnClose = findViewById<Button>(R.id.btn_close)

        btnShowCard.setOnClickListener {
            loadSparklingCardView(container)
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun loadSparklingCardView(container: FrameLayout) {
        sparklingView?.let {
            it.getKitView()?.destroy()
            container.removeView(it)
        }

        val view = SparklingView(this)
        sparklingView = view
        container.addView(view)

        val context = SparklingContext()
        context.scheme =
            "hybrid://lynxview_card?bundle=card-view-demo.lynx.bundle&hide_nav_bar=1&screen_orientation=portrait"
        context.withInitData("{ \"initial_data\":{\"source\":\"native_button\",\"platform\":\"android\"}}")

        view.prepare(context)
        view.loadUrl()
    }

    override fun onDestroy() {
        super.onDestroy()
        sparklingView?.getKitView()?.destroy()
    }
}
