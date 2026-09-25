// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

@objc(SPKDownloadFileParams)
public final class SPKDownloadFileParams: SPKMethodModel {
    @objc public var url: String?
    @objc public var fileExtension: String?
    @objc public var header: NSDictionary?
    @objc public var params: NSDictionary?
    @objc public var saveToAlbum: String?
    @objc public var timeoutInterval: NSNumber? = 0
    // Kept for API compatibility. The public URLSession transport has no common-parameter provider.
    @objc public var needCommonParams = true

    public override class func requiredKeyPaths() -> Set<String>? { ["url"] }
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["url": "url", "fileExtension": "extension", "header": "header", "params": "params",
         "saveToAlbum": "saveToAlbum", "timeoutInterval": "timeoutInterval", "needCommonParams": "needCommonParams"]
    }
}

@objc(SPKDownloadFileMethod)
public final class SPKDownloadFileMethod: SPKMethod {
    public override class func methodName() -> String? { "media.downloadFile" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { SPKDownloadFileParams.self }
    public override var resultModelClass: AnyClass? { SPKDownloadResult.self }

    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        guard let params = paramModel as? SPKDownloadFileParams,
              let address = params.url, var components = URLComponents(string: address),
              ["https", "http"].contains(components.scheme?.lowercased() ?? ""),
              !(params.fileExtension ?? "").contains("/") else {
            completionHandler?(nil, SPKMedia.error(.invalidParameter, "Invalid URL or file extension."))
            return
        }
        components.queryItems = (components.queryItems ?? []) + (params.params as? [String: Any] ?? [:]).map {
            URLQueryItem(name: $0.key, value: String(describing: $0.value))
        }
        guard let url = components.url else {
            completionHandler?(nil, SPKMedia.error(.invalidParameter, "Invalid URL."))
            return
        }
        var request = URLRequest(url: url)
        (params.header as? [String: String])?.forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }
        if let timeout = params.timeoutInterval?.doubleValue, timeout > 0 { request.timeoutInterval = timeout }
        URLSession.shared.downloadTask(with: request) { location, response, failure in
            guard let result = SPKDownloadResult() else { return }
            let http = response as? HTTPURLResponse
            result.httpCode = NSNumber(value: http?.statusCode ?? 0)
            result.header = SPKMedia.headers(http)
            if let failure {
                result.clientCode = NSNumber(value: (failure as NSError).code)
                completionHandler?(result, SPKMedia.error(.failed, failure.localizedDescription))
                return
            }
            guard let location, http != nil else {
                completionHandler?(result, SPKMedia.error(.failed, "Download failed."))
                return
            }
            let destination = SPKMedia.temporaryURL(params.fileExtension ?? "")
            do {
                try FileManager.default.moveItem(at: location, to: destination)
                result.filePath = destination.path
                SPKMedia.saveToAlbum(destination, kind: params.saveToAlbum) { status in
                    if status != nil { result.filePath = nil }
                    completionHandler?(result, status)
                }
            } catch {
                completionHandler?(result, SPKMedia.error(.failed, error.localizedDescription))
            }
        }.resume()
    }
}

@SPKGlobalMethod
extension SPKDownloadFileMethod {}

@objc(SPKUploadParams)
public class SPKUploadParams: SPKMethodModel {
    @objc public var url: String?
    @objc public var filePath: String?
    @objc public var name: String?
    @objc public var fileName: String?
    @objc public var mimeType: String?
    @objc public var header: NSDictionary?
    @objc public var params: NSDictionary?
    @objc public var timeoutInterval: NSNumber? = 0
    // Kept for API compatibility. The public URLSession transport has no common-parameter provider.
    @objc public var needCommonParams = true

    public override class func requiredKeyPaths() -> Set<String>? { ["url", "filePath"] }
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["url": "url", "filePath": "filePath", "name": "name", "fileName": "fileName", "mimeType": "mimeType",
         "header": "header", "params": "params", "timeoutInterval": "timeoutInterval", "needCommonParams": "needCommonParams"]
    }
}

@objc(SPKUploadImageParams)
public final class SPKUploadImageParams: SPKUploadParams {
    // Declared but unused in the original uploadImage implementation.
    @objc public var paramsOption = 0
    public override class func requiredKeyPaths() -> Set<String>? { ["url"] }
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        var paths = super.jsonKeyPathsByPropertyKey()
        paths.removeValue(forKey: "mimeType")
        paths["paramsOption"] = "paramsOption"
        return paths
    }
}

private enum SPKUpload {
    static func run(_ params: SPKUploadParams, image: Bool = false, completion: SPKMethodCompletionHandler?) {
        guard let address = params.url, let url = URL(string: address),
              ["https", "http"].contains(url.scheme?.lowercased() ?? ""),
              let path = params.filePath, !path.isEmpty else {
            completion?(nil, SPKMedia.error(.invalidParameter, "A URL and filePath are required."))
            return
        }
        let file = SPKMedia.fileURL(path)
        guard FileManager.default.fileExists(atPath: file.path) else {
            completion?(nil, SPKMedia.error(.invalidParameter, "The upload file does not exist."))
            return
        }
        let boundary = "SPK-\(UUID().uuidString)"
        var body = Data()
        let fields = (params.params as? [String: Any] ?? [:]).map { ($0.key, String(describing: $0.value)) }

        for (key, value) in fields {
            body.append(Data("--\(boundary)\r\nContent-Disposition: form-data; name=\"\(key)\"\r\n\r\n\(value)\r\n".utf8))
        }
        let name = params.name ?? "file"
        let filename = params.fileName ?? file.lastPathComponent
        let mime = image ? SPKMedia.mimeType(for: file, image: true) : (params.mimeType ?? SPKMedia.mimeType(for: file))
        body.append(Data("--\(boundary)\r\nContent-Disposition: form-data; name=\"\(name)\"; filename=\"\(filename)\"\r\nContent-Type: \(mime)\r\n\r\n".utf8))
        do { body.append(try Data(contentsOf: file)) }
        catch { completion?(nil, SPKMedia.error(.failed, error.localizedDescription)); return }
        body.append(Data("\r\n--\(boundary)--\r\n".utf8))
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        (params.header as? [String: String])?.forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }
        if let timeout = params.timeoutInterval?.doubleValue, timeout > 0 { request.timeoutInterval = timeout }
        URLSession.shared.uploadTask(with: request, from: body) { data, response, failure in
            guard let result = (image ? SPKUploadImageResult() : SPKUploadFileResult()) else { return }
            let http = response as? HTTPURLResponse
            result.httpCode = NSNumber(value: http?.statusCode ?? 0)
            result.header = SPKMedia.headers(http)
            if failure == nil, http != nil, let data, let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                result.responseData = json as NSDictionary
                result.url = json["url"] as? String
                result.uri = json["uri"] as? String
            }
            if let failure {
                result.clientCode = NSNumber(value: (failure as NSError).code)
                completion?(result, SPKMedia.error(.failed, failure.localizedDescription))
            } else if http != nil {
                result.clientCode = 0
                completion?(result, nil)
            } else {
                completion?(result, SPKMedia.error(.failed, "The response returned from server is malformed."))
            }
        }.resume()
    }
}

@objc(SPKUploadFileMethod)
public final class SPKUploadFileMethod: SPKMethod {
    public override class func methodName() -> String? { "media.uploadFile" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { SPKUploadParams.self }
    public override var resultModelClass: AnyClass? { SPKUploadFileResult.self }
    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        guard let params = paramModel as? SPKUploadParams else {
            completionHandler?(nil, SPKMedia.error(.invalidParameter, "Invalid upload parameters."))
            return
        }
        SPKUpload.run(params, completion: completionHandler)
    }
}

@SPKGlobalMethod
extension SPKUploadFileMethod {}

@objc(SPKUploadImageMethod)
public final class SPKUploadImageMethod: SPKMethod {
    public override class func methodName() -> String? { "media.uploadImage" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { SPKUploadImageParams.self }
    public override var resultModelClass: AnyClass? { SPKUploadImageResult.self }
    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        guard let params = paramModel as? SPKUploadParams else {
            completionHandler?(nil, SPKMedia.error(.invalidParameter, "Invalid upload parameters."))
            return
        }
        SPKUpload.run(params, image: true, completion: completionHandler)
    }
}

@SPKGlobalMethod
extension SPKUploadImageMethod {}
