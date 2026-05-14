// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tiktok.sparkling.Sparkling.Companion.SPARKLING_CONTEXT_CONTAINER_ID
import com.tiktok.sparkling.hybridkit.base.IPerformanceView

class SparklingFragment : Fragment() {
    companion object {
        fun newInstance() = SparklingFragment()
    }

    private var sparklingView: SparklingView? = null
    private var hasLoad = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val containerId = activity?.intent?.getStringExtra(SPARKLING_CONTEXT_CONTAINER_ID)
        val sparklingContext = SparklingContextTransferStation.getSparklingContext(containerId)
        if (sparklingContext != null && context != null) {
            sparklingView = SparklingView(requireContext())
            sparklingView?.prepare(sparklingContext)
            sparklingView?.loadStatus()?.let {
                hasLoad = it != IPerformanceView.LoadStatus.INIT
            }
            if (!hasLoad) {
                sparklingView?.loadUrl()
            }
            return sparklingView
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        sparklingView?.loadStatus()?.let {
            hasLoad = it != IPerformanceView.LoadStatus.INIT
        }
        if (!hasLoad) {
            sparklingView?.loadUrl()
        }
    }

    override fun onResume() {
        super.onResume()
        sparklingView?.getKitView()?.onShow()
    }

    override fun onPause() {
        super.onPause()
        sparklingView?.getKitView()?.onHide()
    }

    fun loadUrl() {
        sparklingView?.loadUrl()
    }
}
