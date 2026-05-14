// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

// Parameter model
@objc(SPKUploadImageMethodParamModel)
public class SPKUploadImageMethodParamModel: SPKMethodModel {
    @objc public override static func requiredKeyPaths() -> Set<String>? {
        return ["url"]
    }

    @objc public var url: String?
    @objc public var filePath: String?
    @objc public var name: String?
    @objc public var fileName: String?
    @objc public var header: [String: String]?
    @objc public var params: [String: Any]?
    @objc public var paramsOption: Int = 0
    @objc public var needCommonParams: Bool = true
    @objc public var timeoutInterval: Double = 0

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "url": "url",
            "filePath": "filePath",
            "name": "name",
            "fileName": "fileName",
            "header": "header",
            "params": "params",
            "paramsOption": "paramsOption",
            "needCommonParams": "needCommonParams",
            "timeoutInterval": "timeoutInterval",
        ]
    }
}

// Result model
@objc(SPKUploadImageMethodResultModel)
class SPKUploadImageMethodResultModel: SPKMethodModel {
    @objc public var clientCode: Int = 0
    @objc public var httpCode: Int = 0
    @objc public var url: String?
    @objc public var uri: String?
    @objc public var header: [String: String]?
    @objc public var responseData: [String: Any]?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "clientCode": "clientCode",
            "httpCode": "httpCode",
            "url": "url",
            "uri": "uri",
            "header": "header",
            "responseData": "response",
        ]
    }
}

// Main method class
@objc(SPKUploadImageMethod)
public class SPKUploadImageMethod: PipeMethod {
    @objc public override var paramsModelClass: AnyClass {
        return SPKUploadImageMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return SPKUploadImageMethodResultModel.self
    }

    public override var methodName: String {
        return "media.uploadImage"
    }

    public override class func methodName() -> String {
        return "media.uploadImage"
    }
}
