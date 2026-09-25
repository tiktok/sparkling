// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Photos
import UIKit
import SparklingMethod

enum SPKMedia {
    static func error(_ code: SPKMethodStatusCode, _ message: String) -> SPKMethodStatus {
        let status = SPKMethodStatus(statusCode: code)
        status.message = message
        return status
    }

    static func fileURL(_ path: String) -> URL {
        if path.hasPrefix("file://"), let url = URL(string: path) { return url }
        return URL(fileURLWithPath: path)
    }

    static func temporaryURL(_ ext: String) -> URL {
        let name = UUID().uuidString
        return FileManager.default.temporaryDirectory.appendingPathComponent(ext.isEmpty ? name : "\(name).\(ext)")
    }

    static func mimeType(for file: URL, image: Bool = false) -> String {
        let types = [
            "jpg": "image/jpeg", "jpeg": "image/jpeg", "png": "image/png", "gif": "image/gif",
            "webp": "image/webp", "heic": "image/heic", "heif": "image/heif",
            "mp4": "video/mp4", "mov": "video/quicktime", "avi": "video/x-msvideo",
            "mkv": "video/x-matroska", "wmv": "video/x-ms-wmv", "mp3": "audio/mpeg",
            "wav": "audio/wav", "aac": "audio/aac", "m4a": "audio/mp4", "pdf": "application/pdf",
            "doc": "application/msword", "docx": "application/msword", "xls": "application/vnd.ms-excel",
            "xlsx": "application/vnd.ms-excel", "ppt": "application/vnd.ms-powerpoint",
            "pptx": "application/vnd.ms-powerpoint", "txt": "text/plain", "zip": "application/zip",
            "rar": "application/x-rar-compressed", "7z": "application/x-7z-compressed",
            "json": "application/json", "xml": "application/xml", "html": "text/html",
            "css": "text/css", "js": "text/javascript"
        ]
        let ext = file.pathExtension.lowercased()
        if image {
            let extra = ["bmp": "image/bmp", "tiff": "image/tiff", "tif": "image/tiff", "svg": "image/svg+xml"]
            return extra[ext] ?? types[ext].flatMap { $0.hasPrefix("image/") ? $0 : nil } ?? "image/jpeg"
        }
        return types[ext] ?? "application/octet-stream"
    }

    static func headers(_ response: HTTPURLResponse?) -> NSDictionary? {
        guard let response else { return nil }
        var fields: [String: String] = [:]
        response.allHeaderFields.forEach { fields[String(describing: $0.key)] = String(describing: $0.value) }
        return fields as NSDictionary
    }

    static func saveToAlbum(_ file: URL, kind: String?, completion: @escaping (SPKMethodStatus?) -> Void) {
        guard let kind, kind == "image" || kind == "video" else { completion(nil); return }
        let save = {
            if kind == "image" {
                do {
                    let data = try Data(contentsOf: file)
                    guard let image = UIImage(data: data) else {
                        completion(error(.failed, "Failed to save the image to album."))
                        return
                    }
                    UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                    DispatchQueue.main.async { completion(nil) }
                } catch {
                    completion(SPKMedia.error(.failed, error.localizedDescription))
                }
            } else {
                PHPhotoLibrary.shared().performChanges {
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: file)
                } completionHandler: { _, failure in
                    DispatchQueue.main.async {
                        completion(failure.map { error(.failed, $0.localizedDescription) })
                    }
                }
            }
        }
        switch PHPhotoLibrary.authorizationStatus() {
        case .authorized, .limited: save()
        case .notDetermined:
            PHPhotoLibrary.requestAuthorization { authorization in
                DispatchQueue.main.async {
                    if authorization == .authorized || authorization == .limited { save() }
                    else { completion(error(.failed, "No album access.")) }
                }
            }
        default: completion(error(.failed, "No album access."))
        }
    }
}

@objc(SPKMediaResult)
public class SPKMediaResult: SPKMethodModel {
    @objc public var filePath: String?
    @objc public var url: String?
    @objc public var uri: String?
    @objc public var responseData: NSDictionary?
    @objc public var header: NSDictionary?
    @objc public var tempFiles: NSArray?
    @objc public var httpCode: NSNumber? = 0
    @objc public var clientCode: NSNumber? = 0

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["filePath": "filePath", "url": "url", "uri": "uri", "responseData": "response",
         "header": "header", "tempFiles": "tempFiles", "httpCode": "httpCode", "clientCode": "clientCode"]
    }
}

// Keep each method's original response keys; unrelated fields must not leak as nulls.
@objc(SPKSaveDataURLResult)
public final class SPKSaveDataURLResult: SPKMediaResult {
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["filePath": "filePath"]
    }
}

@objc(SPKDownloadResult)
public final class SPKDownloadResult: SPKMediaResult {
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["filePath": "filePath", "header": "header", "httpCode": "httpCode", "clientCode": "clientCode"]
    }
}

@objc(SPKUploadFileResult)
public class SPKUploadFileResult: SPKMediaResult {
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["responseData": "responseData", "header": "header", "httpCode": "httpCode", "clientCode": "clientCode"]
    }
}

@objc(SPKUploadImageResult)
public final class SPKUploadImageResult: SPKUploadFileResult {
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["url": "url", "uri": "uri", "responseData": "response", "header": "header",
         "httpCode": "httpCode", "clientCode": "clientCode"]
    }
}

@objc(SPKChooseMediaResult)
public final class SPKChooseMediaResult: SPKMediaResult {
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] { ["tempFiles": "tempFiles"] }
}
