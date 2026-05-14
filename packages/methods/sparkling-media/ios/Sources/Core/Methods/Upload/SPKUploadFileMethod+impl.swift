// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

extension SPKUploadFileMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard let typedParamModel = paramModel as? SPKUploadFileMethodParamModel else {
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

        let wrappedCompletionHandler: SPKUploadFileCompletionHandler = { [weak self] response, responseData, error in
            guard let self = self else { return }

            let httpResponse = response as? SPKHttpResponseChromium
            let resultModel = SPKUploadFileMethodResultModel()
            resultModel.clientCode = 0
            var status: SPKUploadStatus?

            if let httpResponse = httpResponse {
                resultModel.httpCode = httpResponse.statusCode
                resultModel.header = httpResponse.allHeaderFields as? [String: String]
            }

            if let error = error {
                resultModel.clientCode = error._code
                status = SPKUploadStatus(statusCode: .failed, message: error.localizedDescription)
            } else if httpResponse == nil {
                status = SPKUploadStatus(statusCode: .malformedResponse, message: "The response returned from server is malformed.")
            } else {
                resultModel.responseData = responseData as? [String: Any]
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

        let uploadName = typedParamModel.name ?? "file"
        let uploadFileName = typedParamModel.fileName ?? fileURL.lastPathComponent
        let uploadMimeType = typedParamModel.mimeType ?? self.guessMimeType(for: fileURL)

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
        // Image types
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

        // Video types
        case "mp4":
            return "video/mp4"
        case "mov":
            return "video/quicktime"
        case "avi":
            return "video/x-msvideo"
        case "mkv":
            return "video/x-matroska"
        case "wmv":
            return "video/x-ms-wmv"

        // Audio types
        case "mp3":
            return "audio/mpeg"
        case "wav":
            return "audio/wav"
        case "aac":
            return "audio/aac"
        case "m4a":
            return "audio/mp4"

        // Document types
        case "pdf":
            return "application/pdf"
        case "doc", "docx":
            return "application/msword"
        case "xls", "xlsx":
            return "application/vnd.ms-excel"
        case "ppt", "pptx":
            return "application/vnd.ms-powerpoint"
        case "txt":
            return "text/plain"

        // Archive types
        case "zip":
            return "application/zip"
        case "rar":
            return "application/x-rar-compressed"
        case "7z":
            return "application/x-7z-compressed"

        // Other common types
        case "json":
            return "application/json"
        case "xml":
            return "application/xml"
        case "html":
            return "text/html"
        case "css":
            return "text/css"
        case "js":
            return "text/javascript"

        // Default to binary data
        default:
            return "application/octet-stream"
        }
    }
}
