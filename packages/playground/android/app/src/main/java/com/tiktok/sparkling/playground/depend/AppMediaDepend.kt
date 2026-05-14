// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground.depend

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tiktok.sparkling.method.media.depend.ChooseMediaParams
import com.tiktok.sparkling.method.media.depend.ChooseMediaResults
import com.tiktok.sparkling.method.media.depend.IChooseMediaResultCallback
import com.tiktok.sparkling.method.media.depend.IHostMediaDepend
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Playground implementation of IHostMediaDepend.
 *
 * Uses Application.ActivityLifecycleCallbacks to hook into onActivityResult
 * and onRequestPermissionsResult without modifying SparklingActivity.
 *
 * Call [register] once from Application.onCreate to wire everything up.
 */
class AppMediaDepend : IHostMediaDepend {
    companion object {
        private const val TAG = "AppMediaDepend"
        private const val REQUEST_CODE_ALBUM = 0x600
        private const val REQUEST_CODE_CAMERA = 0x601
        private const val REQUEST_CODE_CAMERA_PERMISSION = 0x602

        /**
         * Create and register the depend with the application lifecycle.
         */
        fun register(app: Application): AppMediaDepend {
            val depend = AppMediaDepend()
            // Use ActivityLifecycleCallbacks to intercept onActivityResult
            // from any SparklingActivity without modifying the SDK class.
            app.registerActivityLifecycleCallbacks(depend.lifecycleCallbacks)
            return depend
        }
    }

    private var pendingCallback: IChooseMediaResultCallback? = null
    private var pendingParams: ChooseMediaParams? = null
    private var pendingContext: Context? = null
    private var cameraOutputFile: File? = null

    /**
     * ActivityLifecycleCallbacks that intercepts activity results.
     * We use onActivityResumed to detect when our activity comes back from
     * camera/gallery picker, and we override the activity via a fragment
     * to receive onActivityResult.
     */
    val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) {
                // Attach a headless fragment to each SparklingActivity to receive
                // onActivityResult and onRequestPermissionsResult callbacks.
                if (activity.javaClass.name == "com.tiktok.sparkling.SparklingActivity") {
                    val fm = (activity as? androidx.appcompat.app.AppCompatActivity)?.supportFragmentManager ?: return
                    if (fm.findFragmentByTag(MediaResultFragment.TAG) == null) {
                        fm
                            .beginTransaction()
                            .add(MediaResultFragment(this@AppMediaDepend), MediaResultFragment.TAG)
                            .commitAllowingStateLoss()
                    }
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle,
            ) {}

            override fun onActivityDestroyed(activity: Activity) {}
        }

    /**
     * Headless fragment that receives onActivityResult and onRequestPermissionsResult.
     * This is the standard Android pattern for receiving results without modifying
     * the host Activity.
     */
    class MediaResultFragment(
        private val depend: AppMediaDepend,
    ) : androidx.fragment.app.Fragment() {
        companion object {
            const val TAG = "MediaResultFragment"
        }

        // No-arg constructor required by the framework for re-creation
        constructor() : this(AppMediaDepend())

        override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?,
        ) {
            super.onActivityResult(requestCode, resultCode, data)
            depend.handleActivityResult(requestCode, resultCode, data)
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray,
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            depend.handlePermissionResult(requestCode, permissions, grantResults)
        }
    }

    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (requestCode != REQUEST_CODE_ALBUM && requestCode != REQUEST_CODE_CAMERA) return
        val callback = pendingCallback ?: return
        val params = pendingParams ?: return
        val context = pendingContext ?: return
        pendingCallback = null
        pendingParams = null
        pendingContext = null

        if (resultCode != Activity.RESULT_OK) {
            callback.onFailure(-1, "User cancelled")
            return
        }

        try {
            when (requestCode) {
                REQUEST_CODE_ALBUM -> handleAlbumResult(data, params, callback, context)
                REQUEST_CODE_CAMERA -> handleCameraResult(params, callback, context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleActivityResult error", e)
            callback.onFailure(-1, e.message ?: "Unknown error")
        }
    }

    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode != REQUEST_CODE_CAMERA_PERMISSION) return
        val activity = pendingContext as? Activity
        val callback = pendingCallback
        val params = pendingParams

        if (activity == null || callback == null || params == null) return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCameraIntent(activity, params, callback)
        } else {
            pendingCallback = null
            pendingParams = null
            pendingContext = null
            callback.onFailure(-1, "Camera permission denied")
        }
    }

    private fun handleAlbumResult(
        data: Intent?,
        params: ChooseMediaParams,
        callback: IChooseMediaResultCallback,
        context: Context,
    ) {
        val uri = data?.data
        if (uri == null) {
            callback.onFailure(-1, "No media selected")
            return
        }
        processUri(uri, params, callback, context)
    }

    private fun handleCameraResult(
        params: ChooseMediaParams,
        callback: IChooseMediaResultCallback,
        context: Context,
    ) {
        val file = cameraOutputFile
        if (file == null || !file.exists()) {
            callback.onFailure(-1, "Camera capture failed")
            return
        }
        processUri(Uri.fromFile(file), params, callback, context)
    }

    private fun processUri(
        uri: Uri,
        params: ChooseMediaParams,
        callback: IChooseMediaResultCallback,
        context: Context,
    ) {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            callback.onFailure(-1, "Cannot read file")
            return
        }

        val cacheDir = File(context.cacheDir, "sparkling_media")
        cacheDir.mkdirs()
        val tempFile = File(cacheDir, "media_${System.currentTimeMillis()}.tmp")
        FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
        inputStream.close()

        val mediaType = if (params.mediaTypes.contains("video")) "video" else "image"
        val fileInfo =
            ChooseMediaResults.FileInfo(
                tempFilePath = tempFile.absolutePath,
                size = tempFile.length(),
                mediaType = mediaType,
            )
        fileInfo.mimeType = context.contentResolver.getType(uri)

        if (params.needBase64Data && mediaType == "image") {
            try {
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                if (bitmap != null) {
                    val baos = ByteArrayOutputStream()
                    val format = if (fileInfo.mimeType?.contains("png") == true) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    bitmap.compress(format, params.compressQuality.coerceIn(1, 100), baos)
                    fileInfo.base64Data = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Base64 encoding failed", e)
            }
        }

        val results = ChooseMediaResults()
        results.tempFiles = listOf(fileInfo)
        callback.onSuccess(results, "ok")
    }

    override fun handleJsInvoke(
        context: Context,
        params: ChooseMediaParams,
        callback: IChooseMediaResultCallback,
    ) {
        val activity = context as? Activity
        if (activity == null) {
            callback.onFailure(-1, "Context is not an Activity, it is: ${context.javaClass.name}")
            return
        }

        pendingCallback = callback
        pendingParams = params
        pendingContext = context

        // Get the headless fragment to use for startActivityForResult
        val fragment = getMediaFragment(activity)

        when (params.sourceType.lowercase()) {
            "album" -> {
                val intent = Intent(Intent.ACTION_PICK)
                val hasVideo = params.mediaTypes.contains("video")
                val hasImage = params.mediaTypes.contains("image")
                intent.type =
                    when {
                        hasVideo && hasImage -> "*/*"
                        hasVideo -> "video/*"
                        else -> "image/*"
                    }
                if (hasVideo && hasImage) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                }
                if (fragment != null) {
                    fragment.startActivityForResult(intent, REQUEST_CODE_ALBUM)
                } else {
                    activity.startActivityForResult(intent, REQUEST_CODE_ALBUM)
                }
            }

            "camera" -> {
                // Check runtime camera permission before launching
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    if (fragment != null) {
                        fragment.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAMERA_PERMISSION)
                    } else {
                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAMERA_PERMISSION)
                    }
                } else {
                    launchCameraIntent(activity, params, callback)
                }
            }

            else -> {
                pendingCallback = null
                pendingParams = null
                pendingContext = null
                callback.onFailure(-1, "Unknown sourceType: ${params.sourceType}")
            }
        }
    }

    private fun getMediaFragment(activity: Activity): MediaResultFragment? {
        val appCompatActivity = activity as? androidx.appcompat.app.AppCompatActivity ?: return null
        return appCompatActivity.supportFragmentManager.findFragmentByTag(MediaResultFragment.TAG) as? MediaResultFragment
    }

    private fun launchCameraIntent(
        activity: Activity,
        params: ChooseMediaParams,
        callback: IChooseMediaResultCallback,
    ) {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val cacheDir = File(activity.cacheDir, "sparkling_media")
            cacheDir.mkdirs()
            val outputFile = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            cameraOutputFile = outputFile

            val photoUri =
                FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    outputFile,
                )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            if (params.cameraType.lowercase() == "front") {
                intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
            val fragment = getMediaFragment(activity)
            if (fragment != null) {
                fragment.startActivityForResult(intent, REQUEST_CODE_CAMERA)
            } else {
                activity.startActivityForResult(intent, REQUEST_CODE_CAMERA)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera launch failed", e)
            pendingCallback = null
            pendingParams = null
            pendingContext = null
            callback.onFailure(-1, "Camera launch failed: ${e.message}")
        }
    }
}
