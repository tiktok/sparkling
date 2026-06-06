package com.tiktok.sparkling.method.media

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import com.tiktok.sparkling.method.media.choosemedia.AbsChooseMediaMethodIDL
import com.tiktok.sparkling.method.media.choosemedia.ChooseMediaMethod
import com.tiktok.sparkling.method.media.depend.ChooseMediaParams
import com.tiktok.sparkling.method.media.depend.ChooseMediaResults
import com.tiktok.sparkling.method.media.depend.IChooseMediaResultCallback
import com.tiktok.sparkling.method.media.depend.IHostMediaDepend
import com.tiktok.sparkling.method.media.downloadfile.AbsDownloadFileMethodIDL
import com.tiktok.sparkling.method.media.downloadfile.DownloadFileMethod
import com.tiktok.sparkling.method.media.savedataurl.AbsSaveDataURLMethodIDL
import com.tiktok.sparkling.method.media.savedataurl.SaveDataURLMethod
import com.tiktok.sparkling.method.media.uploadfile.AbsUploadFileMethodIDL
import com.tiktok.sparkling.method.media.uploadfile.UploadFileMethod
import com.tiktok.sparkling.method.media.uploadimage.AbsUploadImageMethodIDL
import com.tiktok.sparkling.method.media.uploadimage.UploadImageMethod
import com.tiktok.sparkling.method.media.utils.AppFileUtils
import com.tiktok.sparkling.method.media.utils.BDMediaFileUtils
import com.tiktok.sparkling.method.media.utils.BdFileUtils
import com.tiktok.sparkling.method.media.utils.FConstants
import com.tiktok.sparkling.method.media.utils.Md5Utils
import com.tiktok.sparkling.method.media.utils.MediaProvider
import com.tiktok.sparkling.method.media.utils.UploadImageResponse
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.runtime.depend.CommonDependsProvider
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import com.tiktok.sparkling.method.runtime.depend.common.IHostPermissionDepend
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionGrantCallback
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantCallback
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantResult
import com.tiktok.sparkling.method.runtime.depend.network.AbsStreamConnection
import com.tiktok.sparkling.method.runtime.depend.network.AbsStringConnection
import com.tiktok.sparkling.method.runtime.depend.network.HttpRequest
import com.tiktok.sparkling.method.runtime.depend.network.RequestMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class MediaMethodUnitTest {
    private lateinit var app: Application
    private lateinit var activity: Activity
    private lateinit var appBridgeContext: IBridgeContext
    private lateinit var activityBridgeContext: IBridgeContext

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        appBridgeContext = bridgeContext(app)
        activityBridgeContext = bridgeContext(activity)
        MediaProvider.hostMediaDepend = null
        CommonDependsProvider.hostNetworkDepend = null
        CommonDependsProvider.hostPermissionDepend = null
        ThreadPool.configExecutorService(DirectExecutorService())
    }

    @After
    fun tearDown() {
        MediaProvider.hostMediaDepend = null
        CommonDependsProvider.hostNetworkDepend = null
        CommonDependsProvider.hostPermissionDepend = null
    }

    @Test
    fun chooseMediaValidatesRequiredParamsBeforeHostCall() {
        val method = ChooseMediaMethod().apply { setBridgeContext(appBridgeContext) }

        val emptyMediaTypes = chooseParams(mediaTypes = emptyList(), sourceType = "album")
        val emptyMediaCallback = ChooseMediaCallbackRecorder()
        method.handle(emptyMediaTypes, emptyMediaCallback, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, emptyMediaCallback.failureCode)

        val missingCameraType = chooseParams(sourceType = "camera", cameraType = "")
        val cameraCallback = ChooseMediaCallbackRecorder()
        method.handle(missingCameraType, cameraCallback, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, cameraCallback.failureCode)
        assertTrue(cameraCallback.failureMsg?.contains("CameraType") == true)
    }

    @Test
    fun chooseMediaMapsParamsAndResultThroughHostDepend() {
        val capturedParams = slot<ChooseMediaParams>()
        val hostDepend = mockk<IHostMediaDepend>()
        every {
            hostDepend.handleJsInvoke(any(), capture(capturedParams), any())
        } answers {
            val callback = args[2] as IChooseMediaResultCallback
            callback.onSuccess(
                ChooseMediaResults().apply {
                    tempFiles =
                        listOf(
                            ChooseMediaResults
                                .FileInfo(
                                    tempFilePath = "/tmp/a.jpg",
                                    size = 12L,
                                    mediaType = "image",
                                ).apply {
                                    base64Data = "abc"
                                    mimeType = "image/jpeg"
                                },
                        )
                },
            )
        }
        MediaProvider.hostMediaDepend = hostDepend

        val method = ChooseMediaMethod().apply { setBridgeContext(appBridgeContext) }
        val callback = ChooseMediaCallbackRecorder()

        method.handle(
            chooseParams(
                mediaTypes = listOf("image", "video"),
                sourceType = "album",
                maxCount = 3,
                compressImage = true,
                saveToPhotoAlbum = true,
                compressWidth = 100,
                compressHeight = 200,
                compressQuality = 80,
                isNeedCut = true,
                cropRatioWidth = 1,
                cropRatioHeight = 2,
                needBase64Data = true,
                compressOption = 1,
                permissionDenyAction = 1,
                isMultiSelect = true,
                useNewCompressSolution = true,
            ),
            callback,
            BridgePlatformType.LYNX,
        )

        assertNotNull(callback.successResult)
        assertEquals(
            "/tmp/a.jpg",
            callback.successResult
                ?.tempFiles
                ?.firstOrNull()
                ?.tempFilePath,
        )
        assertEquals(
            "abc",
            callback.successResult
                ?.tempFiles
                ?.firstOrNull()
                ?.base64Data,
        )
        assertEquals(listOf("image", "video"), capturedParams.captured.mediaTypes)
        assertEquals(3, capturedParams.captured.maxCount)
        assertTrue(capturedParams.captured.compressImage)
        assertTrue(capturedParams.captured.saveToPhotoAlbum)
        assertTrue(capturedParams.captured.isNeedCut)
        assertTrue(capturedParams.captured.needBase64Data)
        assertTrue(capturedParams.captured.isMultiSelect)
        assertTrue(capturedParams.captured.useNewCompressSolution)
        verify(exactly = 1) { hostDepend.handleJsInvoke(app, any(), any()) }
    }

    @Test
    fun chooseMediaFailsWhenContextOrHostDependMissing() {
        val noContext = mockk<IBridgeContext>(relaxed = true)
        every { noContext.context } returns null
        val methodWithoutContext = ChooseMediaMethod().apply { setBridgeContext(noContext) }
        val noContextCallback = ChooseMediaCallbackRecorder()

        methodWithoutContext.handle(chooseParams(), noContextCallback, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.FAIL, noContextCallback.failureCode)
        assertTrue(noContextCallback.failureMsg?.contains("Context not provided") == true)

        val method = ChooseMediaMethod().apply { setBridgeContext(appBridgeContext) }
        val noHostCallback = ChooseMediaCallbackRecorder()
        method.handle(chooseParams(), noHostCallback, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.FAIL, noHostCallback.failureCode)
        assertTrue(noHostCallback.failureMsg?.contains("hostMediaDepend") == true)
    }

    @Test
    fun saveDataUrlValidatesInputsBeforePermissionRequest() {
        val method = SaveDataURLMethod().apply { setBridgeContext(activityBridgeContext) }

        val emptyDataURLCallback = SaveDataURLCallbackRecorder()
        method.handle(saveParams(dataURL = ""), emptyDataURLCallback, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, emptyDataURLCallback.failureCode)

        val emptyExtensionCallback = SaveDataURLCallbackRecorder()
        method.handle(saveParams(extension = ""), emptyExtensionCallback, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, emptyExtensionCallback.failureCode)

        val emptyFilenameCallback = SaveDataURLCallbackRecorder()
        method.handle(saveParams(filename = ""), emptyFilenameCallback, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, emptyFilenameCallback.failureCode)

        val invalidPrefixCallback = SaveDataURLCallbackRecorder()
        method.handle(saveParams(dataURL = "plain-base64"), invalidPrefixCallback, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, invalidPrefixCallback.failureCode)
    }

    @Test
    fun saveDataUrlWritesValidPngToCache() {
        val method = SaveDataURLMethod()
        val callback = SaveDataURLCallbackRecorder()
        val filename = "media-${System.nanoTime()}"

        method.handleSaveDataURL(
            app,
            saveParams(
                dataURL = "data:image/png;base64,${onePixelPngBase64()}",
                filename = filename,
                extension = "png",
                saveToAlbum = null,
            ),
            callback,
        )
        drainMainLooper()

        assertNotNull(callback.successResult)
        assertTrue(callback.successResult?.filePath?.endsWith("$filename.png") == true)
        assertTrue(File(callback.successResult?.filePath.orEmpty()).exists())
    }

    @Test
    fun saveDataUrlReportsUnsupportedExtensionAndDuplicateFile() {
        val method = SaveDataURLMethod()

        val unsupported = SaveDataURLCallbackRecorder()
        method.handleSaveDataURL(
            app,
            saveParams(dataURL = "data:image/png;base64,${onePixelPngBase64()}", extension = "gif"),
            unsupported,
        )
        assertEquals(IDLBridgeMethod.INVALID_PARAM, unsupported.failureCode)

        val duplicateName = "duplicate-${System.nanoTime()}"
        val cacheFile = File(requireNotNull(BDMediaFileUtils.getCacheDir(app)), "$duplicateName.png")
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText("existing")

        val duplicate = SaveDataURLCallbackRecorder()
        method.handleSaveDataURL(
            app,
            saveParams(dataURL = "data:image/png;base64,${onePixelPngBase64()}", filename = duplicateName, extension = "png"),
            duplicate,
        )
        assertEquals(IDLBridgeMethod.FAIL, duplicate.failureCode)
        assertTrue(duplicate.failureMsg?.contains("already exist") == true)
    }

    @Test
    fun saveDataUrlReportsPermissionDenied() {
        CommonDependsProvider.hostPermissionDepend =
            object : IHostPermissionDepend {
                override fun hasPermission(
                    activity: Activity,
                    permission: String,
                ) = false

                override fun requestPermission(
                    activity: Activity,
                    callback: OnPermissionGrantCallback,
                    permission: String,
                ) {
                    callback.onNotGranted()
                }

                override fun requestPermissions(
                    activity: Activity,
                    callback: OnPermissionsGrantCallback,
                    permissions: Array<String>,
                ) {
                    callback.onResult(arrayOf(OnPermissionsGrantResult(permissions.first(), PackageManager.PERMISSION_DENIED)))
                }
            }

        val method = SaveDataURLMethod().apply { setBridgeContext(activityBridgeContext) }
        val callback = SaveDataURLCallbackRecorder()
        method.handle(saveParams(dataURL = "data:image/png;base64,${onePixelPngBase64()}"), callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("permission denied") == true)
    }

    @Test
    fun uploadFileAndUploadImageValidateContextAndParams() {
        val noContext = mockk<IBridgeContext>(relaxed = true)
        every { noContext.context } returns null

        val uploadFileNoContext = UploadFileCallbackRecorder()
        UploadFileMethod()
            .apply { setBridgeContext(noContext) }
            .handle(uploadFileParams(), uploadFileNoContext, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.FAIL, uploadFileNoContext.failureCode)

        val uploadImageNoContext = UploadImageCallbackRecorder()
        UploadImageMethod()
            .apply { setBridgeContext(noContext) }
            .handle(uploadImageParams(), uploadImageNoContext, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.FAIL, uploadImageNoContext.failureCode)

        val uploadFileMissingPath = UploadFileCallbackRecorder()
        UploadFileMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(uploadFileParams(filePath = null, formDataBody = null), uploadFileMissingPath, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, uploadFileMissingPath.failureCode)

        val uploadImageMissingPath = UploadImageCallbackRecorder()
        UploadImageMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(uploadImageParams(filePath = null, formDataBody = null), uploadImageMissingPath, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, uploadImageMissingPath.failureCode)
    }

    @Test
    fun uploadFileAndUploadImageUseNetworkDependForSingleFile() {
        CommonDependsProvider.hostPermissionDepend = grantingPermissionDepend()
        val uploadBody = """{"data":{"uri":"avatar-uri","url_list":["https://cdn.example.com/avatar.jpg"]},"extra":1}"""
        val networkDepend = HostNetworkDepend(stringConnection = StringConnectionStub(uploadBody, code = 201, clientCode = 17))
        CommonDependsProvider.hostNetworkDepend = networkDepend
        val file =
            File(app.cacheDir, "upload-${System.nanoTime()}.jpg").apply {
                writeBytes(byteArrayOf(1, 2, 3))
            }
        file.deleteOnExit()

        val uploadFileCallback = UploadFileCallbackRecorder()
        UploadFileMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(uploadFileParams(filePath = file.absolutePath), uploadFileCallback, BridgePlatformType.LYNX)
        drainMainLooper()
        assertEquals(RequestMethod.POST, networkDepend.lastMethod)
        assertEquals("https://cdn.example.com/avatar.jpg", uploadFileCallback.successResult?.url)
        assertEquals("avatar-uri", uploadFileCallback.successResult?.uri)
        assertEquals(17, uploadFileCallback.successResult?.clientCode)
        assertEquals(1, uploadFileCallback.successResult?.response?.get("extra"))

        val uploadImageCallback = UploadImageCallbackRecorder()
        UploadImageMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(uploadImageParams(filePath = file.absolutePath), uploadImageCallback, BridgePlatformType.LYNX)
        drainMainLooper()
        assertEquals("https://cdn.example.com/avatar.jpg", uploadImageCallback.successResult?.url)
        assertEquals("avatar-uri", uploadImageCallback.successResult?.uri)
        assertEquals(17, uploadImageCallback.successResult?.clientCode)
    }

    @Test
    fun uploadFileAndUploadImageSupportFormDataBody() {
        CommonDependsProvider.hostPermissionDepend = grantingPermissionDepend()
        CommonDependsProvider.hostNetworkDepend = HostNetworkDepend()
        val file =
            File(app.cacheDir, "upload-form-${System.nanoTime()}.jpg").apply {
                writeBytes(byteArrayOf(1, 2, 3))
            }
        file.deleteOnExit()

        val fileBody = mockk<AbsUploadFileMethodIDL.BridgeBeanUploadFileFormDataBody>()
        every { fileBody.key } returns "avatar"
        every { fileBody.value } returns file.absolutePath
        val uploadFileCallback = UploadFileCallbackRecorder()
        UploadFileMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(
                uploadFileParams(filePath = null, formDataBody = listOf(fileBody), paramsOption = 2),
                uploadFileCallback,
                BridgePlatformType.LYNX,
            )
        drainMainLooper()
        assertEquals("https://cdn.example.com/a.jpg", uploadFileCallback.successResult?.url)

        val imageBody = mockk<AbsUploadImageMethodIDL.BridgeBeanUploadImageFormDataBody>()
        every { imageBody.key } returns "image"
        every { imageBody.value } returns file.absolutePath
        val uploadImageCallback = UploadImageCallbackRecorder()
        UploadImageMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(
                uploadImageParams(filePath = null, formDataBody = listOf(imageBody), paramsOption = 2),
                uploadImageCallback,
                BridgePlatformType.LYNX,
            )
        drainMainLooper()
        assertEquals("https://cdn.example.com/a.jpg", uploadImageCallback.successResult?.url)
    }

    @Test
    fun uploadMethodsReportNetworkAndPathFailures() {
        CommonDependsProvider.hostPermissionDepend = grantingPermissionDepend()
        val file =
            File(app.cacheDir, "upload-${System.nanoTime()}.jpg").apply {
                writeBytes(byteArrayOf(1, 2, 3))
            }
        file.deleteOnExit()

        val uploadFileNoNetwork = UploadFileCallbackRecorder()
        UploadFileMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(uploadFileParams(filePath = file.absolutePath), uploadFileNoNetwork, BridgePlatformType.LYNX)
        drainMainLooper()
        assertEquals(IDLBridgeMethod.FAIL, uploadFileNoNetwork.failureCode)
        assertEquals("networkDepend is null", uploadFileNoNetwork.failureMsg)

        val uploadImageNoNetwork = UploadImageCallbackRecorder()
        UploadImageMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(uploadImageParams(filePath = file.absolutePath), uploadImageNoNetwork, BridgePlatformType.LYNX)
        drainMainLooper()
        assertEquals(IDLBridgeMethod.FAIL, uploadImageNoNetwork.failureCode)
        assertEquals("networkDepend is null", uploadImageNoNetwork.failureMsg)

        val directory = File(app.cacheDir, "upload-dir-${System.nanoTime()}").apply { mkdirs() }
        val uploadFileDirectory = UploadFileCallbackRecorder()
        UploadFileMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(uploadFileParams(filePath = directory.absolutePath), uploadFileDirectory, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.NOT_FOUND, uploadFileDirectory.failureCode)

        val uploadImageDirectory = UploadImageCallbackRecorder()
        UploadImageMethod()
            .apply { setBridgeContext(activityBridgeContext) }
            .handle(uploadImageParams(filePath = directory.absolutePath), uploadImageDirectory, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.NOT_FOUND, uploadImageDirectory.failureCode)
    }

    @Test
    fun downloadFileValidatesParamsAndReportsMissingNetworkDepend() {
        val method = DownloadFileMethod().apply { setBridgeContext(appBridgeContext) }

        val emptyUrl = DownloadFileCallbackRecorder()
        method.handle(downloadParams(url = ""), emptyUrl, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, emptyUrl.failureCode)

        val emptyExtension = DownloadFileCallbackRecorder()
        method.handle(downloadParams(extension = ""), emptyExtension, BridgePlatformType.LYNX)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, emptyExtension.failureCode)

        val missingNetwork = DownloadFileCallbackRecorder()
        method.handle(downloadParams(), missingNetwork, BridgePlatformType.WEB)
        drainMainLooper()
        assertEquals(IDLBridgeMethod.FAIL, missingNetwork.failureCode)
        assertTrue(missingNetwork.failureMsg?.contains("hostNetworkDepend") == true)
    }

    @Test
    fun downloadFileWritesStreamToCacheAndReportsConnectionFailures() {
        CommonDependsProvider.hostNetworkDepend =
            HostNetworkDepend(
                streamConnection = StreamConnectionStub(body = "download-body".toByteArray(), code = 206, clientCode = 5),
            )
        val method = DownloadFileMethod().apply { setBridgeContext(appBridgeContext) }
        val success = DownloadFileCallbackRecorder()
        method.handle(downloadParams(url = "https://example.com/${System.nanoTime()}.txt", extension = "txt"), success, BridgePlatformType.LYNX)
        drainMainLooper()

        assertEquals(206, success.successResult?.httpCode)
        assertEquals(5, success.successResult?.clientCode)
        val savedFile = File(success.successResult?.filePath.orEmpty())
        assertTrue(savedFile.exists())
        assertEquals("download-body", savedFile.readText())

        CommonDependsProvider.hostNetworkDepend =
            HostNetworkDepend(
                streamConnection = StreamConnectionStub(body = null, errorMsg = "body missing", code = 500, clientCode = 99),
            )
        val bodyMissing = DownloadFileCallbackRecorder()
        method.handle(downloadParams(url = "https://example.com/${System.nanoTime()}.txt", extension = "txt"), bodyMissing, BridgePlatformType.LYNX)
        drainMainLooper()
        assertEquals(IDLBridgeMethod.FAIL, bodyMissing.failureCode)
        assertEquals("body missing", bodyMissing.failureMsg)
    }

    @Test
    fun utilityHelpersExposeDeterministicValues() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Md5Utils.hexDigest("abc"))
        assertEquals("/a/b/c", BDMediaFileUtils.removeDupSlash("/a//b///c/"))
        assertFalse(BDMediaFileUtils.removeFile(""))
        assertEquals(FConstants.URI_IMAGE, "image")
    }

    @Test
    fun fileUtilityHelpersResolveSimpleUrisAndModels() {
        val file =
            File(app.cacheDir, "uri-${System.nanoTime()}.txt").apply {
                writeText("uri")
            }
        file.deleteOnExit()

        assertNull(BdFileUtils.convertUriToPath(app, null))
        assertEquals(file.absolutePath, BdFileUtils.convertUriToPath(app, Uri.fromFile(file)))
        assertEquals("http://example.com/a.png", BdFileUtils.convertUriToPath(app, Uri.parse("http://example.com/a.png")))
        assertTrue(
            BdFileUtils
                .convertUriToPath(
                    app,
                    Uri.parse("content://com.android.fileexplorer.myprovider/root/DCIM/a.jpg"),
                )?.endsWith("/DCIM/a.jpg") == true,
        )
        assertEquals(file.absolutePath, AppFileUtils.getOrCopiedFilePath(app, file.absolutePath))
        assertEquals(file.absolutePath, AppFileUtils.getOrCopiedFilePath(app, Uri.fromFile(file).toString()))

        val response =
            UploadImageResponse().apply {
                data =
                    com.tiktok.sparkling.method.media.utils.AvatarUri().apply {
                        uri = "avatar"
                        urlList = listOf("https://cdn.example.com/a.jpg")
                    }
            }
        assertEquals("avatar", response.data?.uri)
        assertEquals("https://cdn.example.com/a.jpg", response.data?.urlList?.first())
    }

    @Test
    fun bdMediaFileUtilsCoverCopyAndUriHelpers() {
        val copied = ByteArrayOutputStream()
        BDMediaFileUtils.copyFile(ByteArrayInputStream("copy".toByteArray()), copied)
        assertEquals("copy", copied.toString())
        BDMediaFileUtils.closeQuietly(null)
        assertFalse(BDMediaFileUtils.isUriExists(null, Uri.parse("content://media/external/images/media/1")))
        assertFalse(BDMediaFileUtils.isUriExists(app, null))
        assertNotNull(BDMediaFileUtils.getCacheDir(app))

        val removable =
            File(app.cacheDir, "remove-${System.nanoTime()}.txt").apply {
                writeText("remove")
            }
        if (BDMediaFileUtils.isSdcardWritable) {
            assertTrue(BDMediaFileUtils.removeFile(removable.absolutePath))
        } else {
            assertFalse(BDMediaFileUtils.removeFile(removable.absolutePath))
        }

        assertNull(BDMediaFileUtils.createImageUri(null, "a.jpg", "image/jpeg", "Pictures"))
        assertNull(BDMediaFileUtils.createImageUri(app, "", "image/jpeg", "Pictures"))
        assertNull(BDMediaFileUtils.createVideoUri(null, "a.mp4", "video/mp4", "Movies"))
        assertNull(BDMediaFileUtils.createVideoUri(app, "", "video/mp4", "Movies"))
        val values = ContentValues()
        BDMediaFileUtils.createImageUri(app, "a-${System.nanoTime()}.jpg", "image/jpeg", "Pictures/Test", values)
        assertEquals("image/jpeg", values.getAsString("mime_type"))
        val videoValues = ContentValues()
        BDMediaFileUtils.createVideoUri(app, "a-${System.nanoTime()}.mp4", "video/mp4", "Movies/Test", videoValues)
        assertEquals("video/mp4", videoValues.getAsString("mime_type"))
    }

    private fun bridgeContext(context: Context): IBridgeContext {
        val bridgeContext = mockk<IBridgeContext>(relaxed = true)
        every { bridgeContext.context } returns context
        every { bridgeContext.ownerActivity } returns (context as? Activity)
        return bridgeContext
    }

    private fun chooseParams(
        mediaTypes: List<String> = listOf("image"),
        sourceType: String = "album",
        maxCount: Number = 1,
        compressImage: Boolean = false,
        saveToPhotoAlbum: Boolean = false,
        cameraType: String = "back",
        compressWidth: Number = 0,
        compressHeight: Number = 0,
        compressQuality: Number = 100,
        isNeedCut: Boolean = false,
        cropRatioWidth: Number = 0,
        cropRatioHeight: Number = 0,
        needBase64Data: Boolean = false,
        compressOption: Number = 0,
        permissionDenyAction: Number = 0,
        isMultiSelect: Boolean = false,
        useNewCompressSolution: Boolean = false,
    ): AbsChooseMediaMethodIDL.ChooseMediaInputModel {
        val params = mockk<AbsChooseMediaMethodIDL.ChooseMediaInputModel>(relaxed = true)
        every { params.mediaTypes } returns mediaTypes
        every { params.sourceType } returns sourceType
        every { params.maxCount } returns maxCount
        every { params.compressImage } returns compressImage
        every { params.saveToPhotoAlbum } returns saveToPhotoAlbum
        every { params.cameraType } returns cameraType
        every { params.compressWidth } returns compressWidth
        every { params.compressHeight } returns compressHeight
        every { params.compressQuality } returns compressQuality
        every { params.isNeedCut } returns isNeedCut
        every { params.cropRatioWidth } returns cropRatioWidth
        every { params.cropRatioHeight } returns cropRatioHeight
        every { params.needBase64Data } returns needBase64Data
        every { params.compressOption } returns compressOption
        every { params.permissionDenyAction } returns permissionDenyAction
        every { params.isMultiSelect } returns isMultiSelect
        every { params.useNewCompressSolution } returns useNewCompressSolution
        return params
    }

    private fun saveParams(
        dataURL: String = "data:image/png;base64,${onePixelPngBase64()}",
        filename: String = "media-${System.nanoTime()}",
        extension: String = "png",
        saveToAlbum: String? = null,
    ): AbsSaveDataURLMethodIDL.SaveDataURLInputModel {
        val params = mockk<AbsSaveDataURLMethodIDL.SaveDataURLInputModel>(relaxed = true)
        every { params.dataURL } returns dataURL
        every { params.filename } returns filename
        every { params.extension } returns extension
        every { params.saveToAlbum } returns saveToAlbum
        return params
    }

    private fun uploadFileParams(
        url: String = "https://example.com/upload",
        filePath: String? = "/tmp/image.jpg",
        formDataBody: List<AbsUploadFileMethodIDL.BridgeBeanUploadFileFormDataBody>? = null,
        paramsOption: Number? = 0,
    ): AbsUploadFileMethodIDL.UploadFileParamModel {
        val params = mockk<AbsUploadFileMethodIDL.UploadFileParamModel>(relaxed = true)
        every { params.url } returns url
        every { params.filePath } returns filePath
        every { params.formDataBody } returns formDataBody
        every { params.params } returns null
        every { params.header } returns null
        every { params.paramsOption } returns paramsOption
        return params
    }

    private fun uploadImageParams(
        url: String = "https://example.com/upload",
        filePath: String? = "/tmp/image.jpg",
        formDataBody: List<AbsUploadImageMethodIDL.BridgeBeanUploadImageFormDataBody>? = null,
        paramsOption: Number? = 0,
    ): AbsUploadImageMethodIDL.UploadImageParamModel {
        val params = mockk<AbsUploadImageMethodIDL.UploadImageParamModel>(relaxed = true)
        every { params.url } returns url
        every { params.filePath } returns filePath
        every { params.formDataBody } returns formDataBody
        every { params.params } returns null
        every { params.header } returns null
        every { params.paramsOption } returns paramsOption
        return params
    }

    private fun downloadParams(
        url: String = "https://example.com/file.png",
        extension: String = "png",
    ): AbsDownloadFileMethodIDL.DownloadFileInputModel {
        val params = mockk<AbsDownloadFileMethodIDL.DownloadFileInputModel>(relaxed = true)
        every { params.url } returns url
        every { params.extension } returns extension
        every { params.params } returns null
        every { params.header } returns null
        every { params.saveToAlbum } returns null
        every { params.needCommonParams } returns true
        every { params.timeoutInterval } returns null
        return params
    }

    private fun grantingPermissionDepend(): IHostPermissionDepend =
        object : IHostPermissionDepend {
            override fun hasPermission(
                activity: Activity,
                permission: String,
            ) = true

            override fun requestPermission(
                activity: Activity,
                callback: OnPermissionGrantCallback,
                permission: String,
            ) {
                callback.onAllGranted()
            }

            override fun requestPermissions(
                activity: Activity,
                callback: OnPermissionsGrantCallback,
                permissions: Array<String>,
            ) {
                callback.onResult(
                    permissions.map { OnPermissionsGrantResult(it, PackageManager.PERMISSION_GRANTED) }.toTypedArray(),
                )
            }
        }

    private fun onePixelPngBase64(): String {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP)
    }

    private fun drainMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private class StringConnectionStub(
        private val body: String? = """{"data":{"uri":"avatar","url_list":["https://cdn.example.com/a.jpg"]}}""",
        private val code: Int = 200,
        private val clientCode: Int = 0,
        private val errorMsg: String = "",
        private val exception: Throwable? = null,
    ) : AbsStringConnection() {
        override fun getStringResponseBody(): String? = body

        override fun getResponseHeader(): LinkedHashMap<String, String> = linkedMapOf("h" to "v")

        override fun getResponseCode(): Int = code

        override fun getClientCode(): Int = clientCode

        override fun getErrorMsg(): String = errorMsg

        override fun getException(): Throwable? = exception
    }

    private class StreamConnectionStub(
        private val body: ByteArray? = byteArrayOf(1, 2, 3),
        private val code: Int = 200,
        private val clientCode: Int = 0,
        private val errorMsg: String = "",
        private val exception: Throwable? = null,
    ) : AbsStreamConnection() {
        var cancelled = false

        override fun getInputStreamResponseBody() = body?.let { ByteArrayInputStream(it) }

        override fun getResponseHeader(): LinkedHashMap<String, String> = linkedMapOf("h" to "v")

        override fun getResponseCode(): Int = code

        override fun getClientCode(): Int = clientCode

        override fun getErrorMsg(): String = errorMsg

        override fun getException(): Throwable? = exception

        override fun cancel() {
            cancelled = true
        }
    }

    private class HostNetworkDepend(
        private val stringConnection: AbsStringConnection = StringConnectionStub(),
        private val streamConnection: AbsStreamConnection = StreamConnectionStub(),
    ) : IHostNetworkDepend {
        var lastMethod: RequestMethod? = null
        var lastRequest: HttpRequest? = null

        override fun requestForString(
            method: RequestMethod,
            request: HttpRequest,
        ): AbsStringConnection {
            lastMethod = method
            lastRequest = request
            return stringConnection
        }

        override fun requestForStream(
            method: RequestMethod,
            request: HttpRequest,
        ): AbsStreamConnection {
            lastMethod = method
            lastRequest = request
            return streamConnection
        }
    }

    private class DirectExecutorService : AbstractExecutorService() {
        private var shutdown = false

        override fun shutdown() {
            shutdown = true
        }

        override fun shutdownNow(): MutableList<Runnable> {
            shutdown = true
            return mutableListOf()
        }

        override fun isShutdown() = shutdown

        override fun isTerminated() = shutdown

        override fun awaitTermination(
            timeout: Long,
            unit: TimeUnit,
        ) = true

        override fun execute(command: Runnable) {
            command.run()
        }
    }

    private class ChooseMediaCallbackRecorder : CompletionBlock<AbsChooseMediaMethodIDL.ChooseMediaResultModel> {
        var successResult: AbsChooseMediaMethodIDL.ChooseMediaResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsChooseMediaMethodIDL.ChooseMediaResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsChooseMediaMethodIDL.ChooseMediaResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsChooseMediaMethodIDL.ChooseMediaResultModel?) = Unit
    }

    private class SaveDataURLCallbackRecorder : CompletionBlock<AbsSaveDataURLMethodIDL.SaveDataURLResultModel> {
        var successResult: AbsSaveDataURLMethodIDL.SaveDataURLResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsSaveDataURLMethodIDL.SaveDataURLResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsSaveDataURLMethodIDL.SaveDataURLResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsSaveDataURLMethodIDL.SaveDataURLResultModel?) = Unit
    }

    private class UploadFileCallbackRecorder : CompletionBlock<AbsUploadFileMethodIDL.UploadFileResultModel> {
        var successResult: AbsUploadFileMethodIDL.UploadFileResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsUploadFileMethodIDL.UploadFileResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsUploadFileMethodIDL.UploadFileResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsUploadFileMethodIDL.UploadFileResultModel?) = Unit
    }

    private class UploadImageCallbackRecorder : CompletionBlock<AbsUploadImageMethodIDL.UploadImageResultModel> {
        var successResult: AbsUploadImageMethodIDL.UploadImageResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsUploadImageMethodIDL.UploadImageResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsUploadImageMethodIDL.UploadImageResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsUploadImageMethodIDL.UploadImageResultModel?) = Unit
    }

    private class DownloadFileCallbackRecorder : CompletionBlock<AbsDownloadFileMethodIDL.DownloadFileResultModel> {
        var successResult: AbsDownloadFileMethodIDL.DownloadFileResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsDownloadFileMethodIDL.DownloadFileResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsDownloadFileMethodIDL.DownloadFileResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsDownloadFileMethodIDL.DownloadFileResultModel?) = Unit
    }
}
