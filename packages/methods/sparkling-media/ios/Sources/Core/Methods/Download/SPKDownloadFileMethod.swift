// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Photos
import SparklingMethod
import UIKit

// Completion handler types
typealias SPKBridgeMethodCompletionHandler = (Any?, Any?) -> Void
typealias SPKBridgeDownloadFileCompletionHandler = (SPKHttpResponse?, URL?, Error?) -> Void

// Status code enumeration
enum SPKBridgeStatusCode: Int {
    case succeeded = 0
    case failed = -1
    case invalidParameter = -2
    case malformedResponse = -3
}

// Status model
class SPKBridgeStatus: NSObject {
    var statusCode: SPKBridgeStatusCode = .succeeded
    var message: String?

    init(statusCode: SPKBridgeStatusCode, message: String? = nil) {
        self.statusCode = statusCode
        self.message = message
    }

    static func statusWithStatusCode(_ statusCode: SPKBridgeStatusCode, message: String?) -> SPKBridgeStatus {
        return SPKBridgeStatus(statusCode: statusCode, message: message)
    }
}

// Parameter model
@objc(SPKDownloadFileMethodParamModel)
public class SPKDownloadFileMethodParamModel: SPKMethodModel {
    @objc public override static func requiredKeyPaths() -> Set<String>? {
        return ["url"]
    }

    @objc public var url: String?
    @objc public var extensions: String?
    @objc public var header: [String: String]?
    @objc public var params: [String: Any]?
    @objc public var needCommonParams: Bool = true
    @objc public var saveToAlbum: String?
    @objc public var timeoutInterval: Double = 0

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "url": "url",
            "extensions": "extension",
            "header": "header",
            "params": "params",
            "needCommonParams": "needCommonParams",
            "saveToAlbum": "saveToAlbum",
            "timeoutInterval": "timeoutInterval",
        ]
    }
}

// Result model
@objc(SPKDownloadFileMethodResultModel)
class SPKDownloadFileMethodResultModel: SPKMethodModel {
    @objc public var clientCode: Int = 0
    @objc public var httpCode: Int = 0
    @objc public var header: [String: String]?
    @objc public var filePath: String?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "clientCode": "clientCode",
            "httpCode": "httpCode",
            "header": "header",
            "filePath": "filePath",
        ]
    }
}

// Main method class
@objc(SPKDownloadFileMethod)
public class SPKDownloadFileMethod: PipeMethod {
    @objc public override var paramsModelClass: AnyClass {
        return SPKDownloadFileMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return SPKDownloadFileMethodResultModel.self
    }

    public override var methodName: String {
        return "media.downloadFile"
    }

    public override class func methodName() -> String {
        return "media.downloadFile"
    }

}
