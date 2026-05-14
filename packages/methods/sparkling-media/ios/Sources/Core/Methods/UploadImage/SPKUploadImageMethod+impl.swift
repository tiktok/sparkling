// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

enum SPKUploadImageStatusCode: Int {
    case succeeded = 0
    case failed = -1
    case invalidParameter = -2
    case malformedResponse = -3
}

class SPKUploadImageStatus: NSObject {
    var statusCode: SPKUploadImageStatusCode = .succeeded
    var message: String?

    init(statusCode: SPKUploadImageStatusCode, message: String? = nil) {
        self.statusCode = statusCode
        self.message = message
    }
}

typealias SPKUploadImageCompletionHandler = (SPKHttpResponse?, Any?, Error?) -> Void

extension SPKUploadImageMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard let typedParamModel = paramModel as? SPKUploadImageMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }

        guard let url = typedParamModel.url, !url.isEmpty else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The URL should not be empty."), result: nil)
            return
        }

        guard let filePath = typedParamModel.filePath, !filePath.isEmpty else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The filePath should not be empty."), result: nil)
            return
        }

        let fileURL = URL(fileURLWithPath: filePath)
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The file does not exist at the specified path."), result: nil)
            return
        }

        let wrappedCompletionHandler: SPKUploadImageCompletionHandler = { [weak self] response, responseData, error in
            guard let self = self else { return }

            let httpResponse = response as? SPKHttpResponseChromium
            let resultModel = SPKUploadImageMethodResultModel()
            resultModel.clientCode = 0
            var status: SPKUploadImageStatus?

            if let httpResponse = httpResponse {
                resultModel.httpCode = httpResponse.statusCode
                resultModel.header = httpResponse.allHeaderFields as? [String: String]
            }

            if let error = error {
                resultModel.clientCode = error._code
                status = SPKUploadImageStatus(statusCode: .failed, message: error.localizedDescription)
            } else if httpResponse == nil {
                status = SPKUploadImageStatus(statusCode: .malformedResponse, message: "The response returned from server is malformed.")
            } else {
                resultModel.responseData = responseData as? [String: Any]

                if let responseDict = responseData as? [String: Any] {
                    resultModel.url = responseDict["url"] as? String
                    resultModel.uri = responseDict["uri"] as? String
                }
            }

            if let status = status {
                completionHandler.handleCompletion(status: .failed(message: status.message), result: resultModel)
            } else {
                completionHandler.handleCompletion(status: .succeeded(), result: resultModel)
            }
        }

        let uploadName = typedParamModel.name ?? "file"
        let uploadFileName = typedParamModel.fileName ?? fileURL.lastPathComponent
        let uploadMimeType = guessMimeType(for: fileURL)

        let task = TTNetworkManager.shared.uploadTaskWithRequest(
            url,
            fileURL: fileURL,
            name: uploadName,
            fileName: uploadFileName,
            mimeType: uploadMimeType,
            parameters: typedParamModel.params,
            headerField: typedParamModel.header as? [String: Any],
            needCommonParams: typedParamModel.needCommonParams,
            progress: nil
        ) { response, responseObject, error in
            wrappedCompletionHandler(response, responseObject, error)
        }

        if typedParamModel.timeoutInterval > 0 {
            task.timeoutInterval = typedParamModel.timeoutInterval
            task.protectTimeout = typedParamModel.timeoutInterval
        }

        task.resume()
    }

    private func guessMimeType(for fileURL: URL) -> String {
        let pathExtension = fileURL.pathExtension.lowercased()

        switch pathExtension {
        case "jpg", "jpeg":
            return "image/jpeg"
        case "png":
            return "image/png"
        case "gif":
            return "image/gif"
        case "webp":
            return "image/webp"
        case "heic":
            return "image/heic"
        case "heif":
            return "image/heif"
        case "bmp":
            return "image/bmp"
        case "tiff", "tif":
            return "image/tiff"
        case "svg":
            return "image/svg+xml"
        default:
            return "image/jpeg"
        }
    }
}
