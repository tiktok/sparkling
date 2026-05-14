// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

// Objective-C compatible enum base type
@objc public enum MethodStatusCode: Int {
    case succeeded = 1
    case failed = 0
    case unregisteredMethod = -2
    case invalidInputParameter = -3
    case invalidNamespace = -4
    case invalidResult = -5
    case unauthorizedAccess = -6
    case operationCancelled = -7
    case operationTimeout = -8
    case notFound = -9
    case notImplemented = -10
    case alreadyExists = -11
    case paramModelTypeWrong = -800
    case resultModelTypeWrong = -801
    case unknown = -1000
    case networkUnreachable = -1001
    case networkTimeout = -1002
    case malformedResponse = -1003
}

// Wrapper class for storing status code and message
@objc public class MethodStatus: NSObject {
    @objc public let code: MethodStatusCode
    @objc public let message: String?

    // Convenience constructor
    @objc public init(code: MethodStatusCode, message: String? = nil) {
        self.code = code
        self.message = message
    }

    // Convenience factory method
    @objc public static func succeeded() -> MethodStatus {
        return MethodStatus(code: .succeeded)
    }

    @objc public static func failed(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .failed, message: message)
    }

    @objc public static func invalidParameter(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .invalidInputParameter, message: message)
    }

    @objc public static func notImplemented(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .notImplemented, message: message)
    }

    // Convenience methods for other common statuses
    @objc public static func unregisteredMethod(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .unregisteredMethod, message: message)
    }

    @objc public static func invalidNamespace(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .invalidNamespace, message: message)
    }

    @objc public static func invalidResult(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .invalidResult, message: message)
    }

    @objc public static func unauthorizedAccess(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .unauthorizedAccess, message: message)
    }

    @objc public static func operationCancelled(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .operationCancelled, message: message)
    }

    @objc public static func operationTimeout(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .operationTimeout, message: message)
    }

    @objc public static func notFound(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .notFound, message: message)
    }

    @objc public static func alreadyExists(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .alreadyExists, message: message)
    }

    @objc public static func paramModelTypeWrong(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .paramModelTypeWrong, message: message)
    }

    @objc public static func resultModelTypeWrong(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .resultModelTypeWrong, message: message)
    }

    @objc public static func unknown(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .unknown, message: message)
    }

    @objc public static func networkUnreachable(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .networkUnreachable, message: message)
    }

    @objc public static func networkTimeout(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .networkTimeout, message: message)
    }

    @objc public static func malformedResponse(message: String? = nil) -> MethodStatus {
        return MethodStatus(code: .malformedResponse, message: message)
    }

    // For compatibility with original Int type code access
    @objc public var rawCode: Int {
        return code.rawValue
    }

    // Whether it's successful
    @objc public var isSuccess: Bool {
        return code == .succeeded
    }

    // Already have instance variable message, no need for duplicate computed property
}

extension MethodStatus {
    public override var description: String {
        let msg = self.message ?? ""
        return "<MethodStatus - code: \(self.code), message: \"\(msg)\">"
    }

    public static func == (lhs: MethodStatus, rhs: MethodStatus) -> Bool {
        return lhs.code == rhs.code
    }

    public static func === (lhs: MethodStatus, rhs: MethodStatus) -> Bool {
        return lhs.code == rhs.code && lhs.message == rhs.message
    }
}

extension MethodStatus {
    public struct Constants {
        static let ErrorDomain = "com.sparkling.error.domain"
    }
}
