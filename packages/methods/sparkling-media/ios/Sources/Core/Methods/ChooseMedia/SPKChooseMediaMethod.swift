// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

@objc(SPKChooseMediaMethodResultTempFileModel)
class SPKChooseMediaMethodResultTempFileModel: NSObject {
    @objc public var tempFilePath: String?
    @objc public var tempFileAbsolutePath: String?
    @objc public var base64Data: String?
    @objc public var fileName: String?
    @objc public var mimeType: String?
    @objc public var size: Int64 = 0
    @objc public var width: Int = 0
    @objc public var height: Int = 0
    @objc public var mediaType: Int = 0

    @objc public class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "tempFilePath": "tempFilePath",
            "tempFileAbsolutePath": "tempFileAbsolutePath",
            "base64Data": "base64Data",
            "fileName": "fileName",
            "mimeType": "mimeType",
            "size": "size",
            "width": "width",
            "height": "height",
            "mediaType": "mediaType",
        ]
    }
}

@objc(SPKChooseMediaMethodParamModel)
public class SPKChooseMediaMethodParamModel: SPKMethodModel {
    @objc public override static func requiredKeyPaths() -> Set<String>? {
        return nil
    }

    @objc public var mediaTypes: [String]?
    @objc public var sourceType: String = SPKChooseMediaParamValue.album
    @objc public var cameraType: String = SPKChooseMediaParamValue.back
    @objc public var maxCount: Int = 1
    @objc public var quality: Float = 1.0
    @objc public var videoMaxDuration: Double = 60.0
    @objc public var compressOption: Int = SPKChooseMediaCompressOption.default.rawValue
    @objc public var needPreview: Bool = true

    @objc public var cameraPermissionDenyAction: Int = SPKChooseMediaPermissionDenyAction.default.rawValue
    @objc public var albumPermissionDenyAction: Int = SPKChooseMediaPermissionDenyAction.default.rawValue

    @objc public var compressWidth: CGFloat = 0
    @objc public var compressHeight: CGFloat = 0
    @objc public var compressionQuality: CGFloat = 0.8
    @objc public var type: String? = nil

    @objc public var needTempFilePath: Bool = true
    @objc public var needBase64: Bool = false
    @objc public var needSaveToAlbum: Bool = false

    @objc public var compressImage: Bool = false
    @objc public var needBase64Data: Bool = false
    @objc public var saveToPhotoAlbum: Bool = false

    @objc public var showTakePhotoButton: Bool = true
    @objc public var showCameraRoll: Bool = true
    @objc public var showPhotoLibrary: Bool = true

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "mediaTypes": "mediaTypes",
            "sourceType": "sourceType",
            "cameraType": "cameraType",
            "maxCount": "maxCount",
            "quality": "quality",
            "videoMaxDuration": "videoMaxDuration",
            "compressOption": "compressOption",
            "needPreview": "needPreview",
            "cameraPermissionDenyAction": "cameraPermissionDenyAction",
            "albumPermissionDenyAction": "albumPermissionDenyAction",
            "needTempFilePath": "needTempFilePath",
            "needBase64": "needBase64",
            "needSaveToAlbum": "needSaveToAlbum",
            "showTakePhotoButton": "showTakePhotoButton",
            "showCameraRoll": "showCameraRoll",
            "showPhotoLibrary": "showPhotoLibrary",
            "compressWidth": "compressWidth",
            "compressHeight": "compressHeight",
            "compressionQuality": "compressionQuality",
            "type": "type",
        ]
    }
}

@objc(SPKChooseMediaMethodResultModel)
class SPKChooseMediaMethodResultModel: SPKMethodModel {
    @objc public var tempFiles: [SPKChooseMediaMethodResultTempFileModel]?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "tempFiles": "tempFiles"
        ]
    }
}

@objc(SPKChooseMediaMethod)
public class SPKChooseMediaMethod: PipeMethod {
    @objc public override var paramsModelClass: AnyClass {
        return SPKChooseMediaMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return SPKChooseMediaMethodResultModel.self
    }

    public override var methodName: String {
        return "media.chooseMedia"
    }

    public override class func methodName() -> String {
        return "media.chooseMedia"
    }
}
