// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Photos
import UIKit
import SparklingMethod

@objc(SPKSaveDataURLParams)
public final class SPKSaveDataURLParams: SPKMethodModel {
    @objc public var dataURL: String?
    @objc public var filename: String?
    @objc public var fileExtension: String?
    @objc public var saveToAlbum: String?

    public override class func requiredKeyPaths() -> Set<String>? { ["dataURL", "filename", "extension"] }
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["dataURL": "dataURL", "filename": "filename", "fileExtension": "extension", "saveToAlbum": "saveToAlbum"]
    }
}

@objc(SPKSaveDataURLMethod)
public final class SPKSaveDataURLMethod: SPKMethod {
    public override class func methodName() -> String? { "media.saveDataURL" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { SPKSaveDataURLParams.self }
    public override var resultModelClass: AnyClass? { SPKSaveDataURLResult.self }

    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        let completionHandler = completionHandler ?? { _, _ in }
        guard let typedParamModel = paramModel as? SPKSaveDataURLParams else {
            completionHandler(nil, SPKMedia.error(.invalidParameter, "Invalid parameter model type"))
            return
        }

        guard let dataURL = typedParamModel.dataURL, !dataURL.isEmpty else {
            completionHandler(nil, SPKMedia.error(.invalidParameter, "The dataURL should not be empty."))
            return
        }

        guard let filename = typedParamModel.filename, !filename.isEmpty else {
            completionHandler(nil, SPKMedia.error(.invalidParameter, "The filename should not be empty."))
            return
        }

        guard let fileExtension = typedParamModel.fileExtension, !fileExtension.isEmpty else {
            completionHandler(nil, SPKMedia.error(.invalidParameter, "The extension should not be empty."))
            return
        }

        var base64Data = dataURL

        if let range = dataURL.range(of: ";base64,") {
            base64Data = String(dataURL[range.upperBound...])
        } else if let range = dataURL.range(of: "base64,") {
            base64Data = String(dataURL[range.upperBound...])
        }

        guard let data = Data(base64Encoded: base64Data, options: .ignoreUnknownCharacters) else {
            completionHandler(nil, SPKMedia.error(.invalidParameter, "Invalid base64 data in dataURL."))
            return
        }

        let fullFileName = "\(filename).\(fileExtension)"
        let tmpFilePath = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(fullFileName)

        do {
            try data.write(to: tmpFilePath, options: .atomic)

            guard let resultModel = SPKSaveDataURLResult() else { return }
            resultModel.filePath = tmpFilePath.path

            if let saveToAlbum = typedParamModel.saveToAlbum {
                if saveToAlbum == "image" {
                    saveImageToAlbum(data: data, resultModel: resultModel, completionHandler: completionHandler)
                } else if saveToAlbum == "video" {
                    saveVideoToAlbum(fileURL: tmpFilePath, resultModel: resultModel, completionHandler: completionHandler)
                } else {
                    completionHandler(resultModel, nil)
                }
            } else {
                completionHandler(resultModel, nil)
            }

        } catch {
            completionHandler(nil, SPKMedia.error(.failed, "Failed to write data to file: \(error.localizedDescription)"))
        }
    }

    private func saveImageToAlbum(data: Data, resultModel: SPKSaveDataURLResult, completionHandler: @escaping SPKMethodCompletionHandler) {
        requestPHAuthorization { success in
            if success {
                if let image = UIImage(data: data) {
                    UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                    DispatchQueue.main.async {
                        completionHandler(resultModel, nil)
                    }
                } else {
                    DispatchQueue.main.async {
                        completionHandler(resultModel, SPKMedia.error(.failed, "Failed to create image from data."))
                    }
                }
            } else {
                DispatchQueue.main.async {
                    completionHandler(resultModel, SPKMedia.error(.failed, "No album access."))
                }
            }
        }
    }

    private func saveVideoToAlbum(fileURL: URL, resultModel: SPKSaveDataURLResult, completionHandler: @escaping SPKMethodCompletionHandler) {
        requestPHAuthorization { success in
            if success {
                PHPhotoLibrary.shared().performChanges {
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: fileURL)
                } completionHandler: { success, error in
                    DispatchQueue.main.async {
                        if let error = error {
                            completionHandler(resultModel, SPKMedia.error(.failed, error.localizedDescription))
                        } else {
                            completionHandler(resultModel, nil)
                        }
                    }
                }
            } else {
                DispatchQueue.main.async {
                    completionHandler(resultModel, SPKMedia.error(.failed, "No album access."))
                }
            }
        }
    }

    private func requestPHAuthorization(_ completionHandler: @escaping (Bool) -> Void) {
        let authorizationStatus = PHPhotoLibrary.authorizationStatus()

        switch authorizationStatus {
        case .authorized, .limited:
            completionHandler(true)
        case .notDetermined:
            PHPhotoLibrary.requestAuthorization { status in
                DispatchQueue.main.async {
                    completionHandler(status == .authorized || status == .limited)
                }
            }
        default:
            completionHandler(false)
        }
    }
}

@SPKGlobalMethod
extension SPKSaveDataURLMethod {}
