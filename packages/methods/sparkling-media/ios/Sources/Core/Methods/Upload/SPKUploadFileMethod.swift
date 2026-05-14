// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

// Completion handler types
typealias SPKUploadFileCompletionHandler = (SPKHttpResponse?, Any?, Error?) -> Void

// Status code enumeration (reusing the same as download)
enum SPKUploadStatusCode: Int {
    case succeeded = 0
    case failed = -1
    case invalidParameter = -2
    case malformedResponse = -3
}

// Status model (reusing the same as download)
class SPKUploadStatus: NSObject {
    var statusCode: SPKUploadStatusCode = .succeeded
    var message: String?

    init(statusCode: SPKUploadStatusCode, message: String? = nil) {
        self.statusCode = statusCode
        self.message = message
    }

    static func statusWithStatusCode(_ statusCode: SPKUploadStatusCode, message: String?) -> SPKUploadStatus {
        return SPKUploadStatus(statusCode: statusCode, message: message)
    }
}

// Parameter model
@objc(SPKUploadFileMethodParamModel)
public class SPKUploadFileMethodParamModel: SPKMethodModel {
    @objc public override static func requiredKeyPaths() -> Set<String>? {
        return ["url", "filePath"]
    }

    @objc public var url: String?
    @objc public var filePath: String?
    @objc public var name: String?
    @objc public var fileName: String?
    @objc public var mimeType: String?
    @objc public var header: [String: String]?
    @objc public var params: [String: Any]?
    @objc public var needCommonParams: Bool = true
    @objc public var timeoutInterval: Double = 0

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "url": "url",
            "filePath": "filePath",
            "name": "name",
            "fileName": "fileName",
            "mimeType": "mimeType",
            "header": "header",
            "params": "params",
            "needCommonParams": "needCommonParams",
            "timeoutInterval": "timeoutInterval",
        ]
    }
}

// Result model
@objc(SPKUploadFileMethodResultModel)
class SPKUploadFileMethodResultModel: SPKMethodModel {
    @objc public var clientCode: Int = 0
    @objc public var httpCode: Int = 0
    @objc public var header: [String: String]?
    @objc public var responseData: [String: Any]?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "clientCode": "clientCode",
            "httpCode": "httpCode",
            "header": "header",
            "responseData": "responseData",
        ]
    }
}

// Main method class
@objc(SPKUploadFileMethod)
public class SPKUploadFileMethod: PipeMethod {
    @objc public override var paramsModelClass: AnyClass {
        return SPKUploadFileMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return SPKUploadFileMethodResultModel.self
    }

    public override var methodName: String {
        return "media.uploadFile"
    }

    public override class func methodName() -> String {
        return "media.uploadFile"
    }
}
