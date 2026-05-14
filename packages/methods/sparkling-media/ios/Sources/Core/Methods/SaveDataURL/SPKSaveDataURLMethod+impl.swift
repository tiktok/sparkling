// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Photos
import SparklingMethod
import UIKit

extension SPKSaveDataURLMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard let typedParamModel = paramModel as? SPKSaveDataURLMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }

        guard let dataURL = typedParamModel.dataURL, !dataURL.isEmpty else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The dataURL should not be empty."), result: nil)
            return
        }

        guard let filename = typedParamModel.filename, !filename.isEmpty else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The filename should not be empty."), result: nil)
            return
        }

        guard let fileExtension = typedParamModel.extensions, !fileExtension.isEmpty else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The extension should not be empty."), result: nil)
            return
        }

        var base64Data = dataURL

        if let range = dataURL.range(of: ";base64,") {
            base64Data = String(dataURL[range.upperBound...])
        } else if let range = dataURL.range(of: "base64,") {
            base64Data = String(dataURL[range.upperBound...])
        }

        guard let data = Data(base64Encoded: base64Data, options: .ignoreUnknownCharacters) else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid base64 data in dataURL."), result: nil)
            return
        }

        let fullFileName = "\(filename).\(fileExtension)"
        let tmpFilePath = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(fullFileName)

        do {
            try data.write(to: tmpFilePath, options: .atomic)

            let resultModel = SPKSaveDataURLMethodResultModel()
            resultModel.filePath = tmpFilePath.path.spk_stringByStrippingSandboxPath()

            if let saveToAlbum = typedParamModel.saveToAlbum {
                if saveToAlbum == "image" {
                    saveImageToAlbum(data: data, resultModel: resultModel, completionHandler: completionHandler)
                } else if saveToAlbum == "video" {
                    saveVideoToAlbum(fileURL: tmpFilePath, resultModel: resultModel, completionHandler: completionHandler)
                } else {
                    completionHandler.handleCompletion(status: .succeeded(), result: resultModel)
                }
            } else {
                completionHandler.handleCompletion(status: .succeeded(), result: resultModel)
            }

        } catch {
            completionHandler.handleCompletion(status: .failed(message: "Failed to write data to file: \(error.localizedDescription)"), result: nil)
        }
    }

    private func saveImageToAlbum(data: Data, resultModel: SPKSaveDataURLMethodResultModel, completionHandler: CompletionHandlerProtocol) {
        requestPHAuthorization { success in
            if success {
                if let image = UIImage(data: data) {
                    UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                    DispatchQueue.main.async {
                        completionHandler.handleCompletion(status: .succeeded(), result: resultModel)
                    }
                } else {
                    DispatchQueue.main.async {
                        completionHandler.handleCompletion(status: .failed(message: "Failed to create image from data."), result: resultModel)
                    }
                }
            } else {
                DispatchQueue.main.async {
                    completionHandler.handleCompletion(status: .failed(message: "No album access."), result: resultModel)
                }
            }
        }
    }

    private func saveVideoToAlbum(fileURL: URL, resultModel: SPKSaveDataURLMethodResultModel, completionHandler: CompletionHandlerProtocol) {
        requestPHAuthorization { success in
            if success {
                PHPhotoLibrary.shared().performChanges {
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: fileURL)
                } completionHandler: { success, error in
                    DispatchQueue.main.async {
                        if let error = error {
                            completionHandler.handleCompletion(status: .failed(message: error.localizedDescription), result: resultModel)
                        } else {
                            completionHandler.handleCompletion(status: .succeeded(), result: resultModel)
                        }
                    }
                }
            } else {
                DispatchQueue.main.async {
                    completionHandler.handleCompletion(status: .failed(message: "No album access."), result: resultModel)
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
                    if #available(iOS 14, *) {
                        completionHandler(status == .authorized || status == .limited)
                    } else {
                        completionHandler(status == .authorized)
                    }
                }
            }
        default:
            completionHandler(false)
        }
    }
}
