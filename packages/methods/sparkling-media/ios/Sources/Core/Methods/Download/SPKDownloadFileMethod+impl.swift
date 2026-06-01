// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Photos
import SparklingMethod
import UIKit

extension SPKDownloadFileMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard let typedParamModel = paramModel as? SPKDownloadFileMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }

        guard let url = typedParamModel.url, !url.isEmpty else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The URL should not be empty."), result: nil)
            return
        }

        let fileName = UUID().uuidString
        let fileExtension = typedParamModel.extensions ?? ""
        let fullFileName = fileExtension.isEmpty ? fileName : "\(fileName).\(fileExtension)"
        let tmpFilePath = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(fullFileName).path
        let tmpFileURL = URL(fileURLWithPath: tmpFilePath)

        let wrappedCompletionHandler: SPKBridgeDownloadFileCompletionHandler = { [weak self] response, fileURL, error in
            guard let self = self else { return }

            let httpResponse = response as? SPKHttpResponseChromium
            self.finishDownloadFile(
                httpCode: httpResponse?.statusCode,
                header: httpResponse?.allHeaderFields as? [String: String],
                fileURL: fileURL,
                error: error,
                completionHandler: completionHandler
            )
        }

        let task = TTNetworkManager.shared.downloadTaskWithRequest(
            url,
            parameters: typedParamModel.params,
            headerField: typedParamModel.header as? [String: Any],
            needCommonParams: typedParamModel.needCommonParams,
            progress: nil,
            destination: tmpFileURL,
            autoResume: false
        ) { [weak self] response, fileURL, error in
            guard let self = self else { return }

            if let error = error {
                wrappedCompletionHandler(response, fileURL, error)
                return
            }

            guard let fileURL = fileURL else {
                wrappedCompletionHandler(response, nil, NSError(domain: "SPK", code: -1, userInfo: [NSLocalizedDescriptionKey: "File URL is nil"]))
                return
            }

            if typedParamModel.saveToAlbum == "image" {
                self.saveImageToAlbumWithURL(fileURL, response: response, spkBridgeDownloadFileCompletionHandler: wrappedCompletionHandler)
            } else if typedParamModel.saveToAlbum == "video" {
                self.saveVideoToAlbumWithURL(fileURL, response: response, spkBridgeDownloadFileCompletionHandler: wrappedCompletionHandler)
            } else {
                wrappedCompletionHandler(response, fileURL, nil)
            }
        }

        if typedParamModel.timeoutInterval > 0 {
            task.timeoutInterval = typedParamModel.timeoutInterval
            task.protectTimeout = typedParamModel.timeoutInterval
        }

        task.resume()
    }

    func finishDownloadFile(
        httpCode: Int?,
        header: [String: String]?,
        fileURL: URL?,
        error: Error?,
        completionHandler: CompletionHandlerProtocol
    ) {
        let resultModel = SPKDownloadFileMethodResultModel()
        resultModel.clientCode = 0
        var status: SPKBridgeStatus?

        if let httpCode = httpCode {
            resultModel.httpCode = httpCode
            resultModel.header = header
        }

        if let error = error {
            resultModel.clientCode = error._code
            status = SPKBridgeStatus(statusCode: .failed, message: error.localizedDescription)
        } else if httpCode == nil {
            status = SPKBridgeStatus(statusCode: .malformedResponse, message: "The response returned from server is malformed.")
        } else if let filePath = fileURL?.path {
            resultModel.filePath = filePath.spk_stringByStrippingSandboxPath()
        }

        if let status = status {
            let errorInfo: [String: Any] = [
                "code": status.statusCode.rawValue,
                "message": status.message ?? "",
            ]
            completionHandler.handleCompletion(status: .failed(message: errorInfo["message"] as? String), result: resultModel)
        } else {
            completionHandler.handleCompletion(status: .succeeded(), result: resultModel)
        }
    }

    private func saveImageToAlbumWithURL(_ fileURL: URL, response: SPKHttpResponse?, spkBridgeDownloadFileCompletionHandler: @escaping SPKBridgeDownloadFileCompletionHandler) {
        requestPHAuthorization { [weak self] success in
            guard let self = self else { return }

            if success {
                do {
                    let imageData = try Data(contentsOf: fileURL)
                    if let image = UIImage(data: imageData) {
                        UIImageWriteToSavedPhotosAlbum(image, self, #selector(self.image(_:didFinishSavingWithError:contextInfo:)), nil)
                        DispatchQueue.main.async {
                            spkBridgeDownloadFileCompletionHandler(response, fileURL, nil)
                        }
                    } else {
                        self.failedHandlerWithURL(
                            fileURL, response: response, completionHandler: spkBridgeDownloadFileCompletionHandler, message: "Failed to save the image to album.")
                    }
                } catch {
                    self.failedHandlerWithURL(fileURL, response: response, completionHandler: spkBridgeDownloadFileCompletionHandler, message: error.localizedDescription)
                }
            } else {
                self.failedHandlerWithURL(fileURL, response: response, completionHandler: spkBridgeDownloadFileCompletionHandler, message: "No album access.")
            }
        }
    }

    private func saveVideoToAlbumWithURL(_ fileURL: URL, response: SPKHttpResponse?, spkBridgeDownloadFileCompletionHandler: @escaping SPKBridgeDownloadFileCompletionHandler) {
        requestPHAuthorization { [weak self] success in
            guard let self = self else { return }

            if success {
                PHPhotoLibrary.shared().performChanges {
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: fileURL)
                } completionHandler: { success, error in
                    DispatchQueue.main.async {
                        if let error = error {
                            spkBridgeDownloadFileCompletionHandler(response, fileURL, error)
                        } else {
                            spkBridgeDownloadFileCompletionHandler(response, fileURL, nil)
                        }
                    }
                }
            } else {
                self.failedHandlerWithURL(fileURL, response: response, completionHandler: spkBridgeDownloadFileCompletionHandler, message: "No album access.")
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
                        // Fallback on earlier versions
                    }
                }
            }
        default:
            completionHandler(false)
        }
    }

    // Image save completion callback
    @objc private func image(_ image: UIImage, didFinishSavingWithError error: Error?, contextInfo: UnsafeRawPointer?) {
        // Additional handling logic can be added here
    }

    private func failedHandlerWithURL(_ fileURL: URL, response: SPKHttpResponse?, completionHandler: @escaping SPKBridgeDownloadFileCompletionHandler, message: String) {
        let error = NSError(domain: "SPKDownloadFileErrorDomain", code: 0, userInfo: [NSLocalizedDescriptionKey: message])
        completionHandler(response, fileURL, error)
    }
}
