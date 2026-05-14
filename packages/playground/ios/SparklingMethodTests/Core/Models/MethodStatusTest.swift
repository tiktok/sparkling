// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Testing

@testable import SparklingMethod

@Suite
struct MethodStatusTest {

    // MARK: - Code Property Tests

    @Test func testSucceededCode() {
        let status = MethodStatus.succeeded()
        #expect(status.code.rawValue == 1)
    }

    @Test func testFailedCode() {
        let status = MethodStatus.failed()
        #expect(status.code.rawValue == 0)
    }

    @Test func testUnregisteredMethodCode() {
        let status = MethodStatus.unregisteredMethod()
        #expect(status.code.rawValue == -2)
    }

    @Test func testInvalidParameterCode() {
        let status = MethodStatus.invalidParameter()
        #expect(status.code.rawValue == -3)
    }

    @Test func testInvalidNamespaceCode() {
        let status = MethodStatus.invalidNamespace()
        #expect(status.code.rawValue == -4)
    }

    @Test func testInvalidResultCode() {
        let status = MethodStatus.invalidResult()
        #expect(status.code.rawValue == -5)
    }

    @Test func testUnauthorizedAccessCode() {
        let status = MethodStatus.unauthorizedAccess()
        #expect(status.code.rawValue == -6)
    }

    @Test func testOperationCancelledCode() {
        let status = MethodStatus.operationCancelled()
        #expect(status.code.rawValue == -7)
    }

    @Test func testOperationTimeoutCode() {
        let status = MethodStatus.operationTimeout()
        #expect(status.code.rawValue == -8)
    }

    @Test func testNotFoundCode() {
        let status = MethodStatus.notFound()
        #expect(status.code.rawValue == -9)
    }

    @Test func testNotImplementedCode() {
        let status = MethodStatus.notImplemented()
        #expect(status.code.rawValue == -10)
    }

    @Test func testAlreadyExistsCode() {
        let status = MethodStatus.alreadyExists()
        #expect(status.code.rawValue == -11)
    }

    @Test func testResultModelTypeWrongCode() {
        let status = MethodStatus.resultModelTypeWrong()
        #expect(status.code.rawValue == -801)
    }

    @Test func testUnknownCode() {
        let status = MethodStatus.unknown()
        #expect(status.code.rawValue == -1000)
    }

    @Test func testNetworkUnreachableCode() {
        let status = MethodStatus.networkUnreachable()
        #expect(status.code.rawValue == -1001)
    }

    @Test func testNetworkTimeoutCode() {
        let status = MethodStatus.networkTimeout()
        #expect(status.code.rawValue == -1002)
    }

    @Test func testMalformedResponseCode() {
        let status = MethodStatus.malformedResponse()
        #expect(status.code.rawValue == -1003)
    }

    // MARK: - Message Property Tests

    @Test func testSucceededMessage() {
        let status = MethodStatus.succeeded()
        #expect(status.message == "JSB_SUCCESS")
    }

    @Test func testFailedMessageDefault() {
        let status = MethodStatus.failed()
        #expect(status.message == "JSB_FAILED")
    }

    @Test func testFailedMessageCustom() {
        let customMessage = "Custom failure message"
        let status = MethodStatus.failed(message: customMessage)
        #expect(status.message == customMessage)
    }

    @Test func testInvalidParameterMessageDefault() {
        let status = MethodStatus.invalidParameter()
        #expect(status.message == "JSB_PARAM_ERROR")
    }

    @Test func testInvalidParameterMessageCustom() {
        let customMessage = "Invalid parameter provided"
        let status = MethodStatus.invalidParameter(message: customMessage)
        #expect(status.message == customMessage)
    }

    @Test func testUnregisteredMethodMessageDefault() {
        let status = MethodStatus.unregisteredMethod()
        #expect(status.message == "The pipe method is not found, please register")
    }

    @Test func testUnregisteredMethodMessageCustom() {
        let customMessage = "Method not registered"
        let status = MethodStatus.unregisteredMethod(message: customMessage)
        #expect(status.message == customMessage)
    }

    @Test func testInvalidNamespaceMessageDefault() {
        let status = MethodStatus.invalidNamespace()
        #expect(status.message == "JSB_NAMESPACE_ERROR")
    }

    @Test func testInvalidNamespaceMessageCustom() {
        let customMessage = "Namespace is invalid"
        let status = MethodStatus.invalidNamespace(message: customMessage)
        #expect(status.message == customMessage)
    }

    @Test func testUnknownMessageDefault() {
        let status = MethodStatus.unknown()
        #expect(status.message == "JSB_UNKNOW_ERROR")
    }

    @Test func testUnknownMessageCustom() {
        let customMessage = "Unknown error occurred"
        let status = MethodStatus.unknown(message: customMessage)
        #expect(status.message == customMessage)
    }

    @Test func testOtherStatusesDefaultMessage() {
        let statuses: [MethodStatus] = [
            .invalidResult(),
            .unauthorizedAccess(),
            .operationCancelled(),
            .operationTimeout(),
            .notFound(),
            .notImplemented(),
            .alreadyExists(),
            .paramModelTypeWrong(),
            .resultModelTypeWrong(),
            .networkUnreachable(),
            .networkTimeout(),
            .malformedResponse(),
        ]

        for status in statuses {
            #expect(status.message == "JSB_UNKNOW_ERROR")
        }
    }

    @Test func testOtherStatusesCustomMessage() {
        let customMessage = "Custom error message"
        let statuses: [MethodStatus] = [
            .invalidResult(message: customMessage),
            .unauthorizedAccess(message: customMessage),
            .operationCancelled(message: customMessage),
            .operationTimeout(message: customMessage),
            .notFound(message: customMessage),
            .notImplemented(message: customMessage),
            .alreadyExists(message: customMessage),
            .paramModelTypeWrong(message: customMessage),
            .resultModelTypeWrong(message: customMessage),
            .networkUnreachable(message: customMessage),
            .networkTimeout(message: customMessage),
            .malformedResponse(message: customMessage),
        ]

        for status in statuses {
            #expect(status.message == customMessage)
        }
    }

    // MARK: - Equatable Tests

    @Test func testEqualityByCode() {
        let status1 = MethodStatus.succeeded()
        let status2 = MethodStatus.succeeded()
        #expect(status1 == status2)

        let status3 = MethodStatus.failed(message: "message1")
        let status4 = MethodStatus.failed(message: "message2")
        #expect(status3 == status4)  // Same code, different messages

        let status5 = MethodStatus.succeeded()
        let status6 = MethodStatus.failed()
        #expect(status5 != status6)  // Different codes
    }

    @Test func testStrictEqualityByCodeAndMessage() {
        let status1 = MethodStatus.failed(message: "same message")
        let status2 = MethodStatus.failed(message: "same message")
        #expect(status1 === status2)

        let status3 = MethodStatus.failed(message: "message1")
        let status4 = MethodStatus.failed(message: "message2")
        #expect(!(status3 === status4))  // Same code, different messages

        let status5 = MethodStatus.succeeded()
        let status6 = MethodStatus.failed()
        #expect(!(status5 === status6))  // Different codes
    }

    @Test func testEqualityWithNilMessages() {
        let status1 = MethodStatus.failed()
        let status2 = MethodStatus.failed(message: nil)
        #expect(status1 === status2)

        let status3 = MethodStatus.unknown()
        let status4 = MethodStatus.unknown(message: nil)
        #expect(status3 === status4)
    }

    // MARK: - CustomStringConvertible Tests

    @Test func testDescriptionFormat() {
        let status = MethodStatus.succeeded()
        let description = status.description
        let expected = "<MethodStatus - code: \(MethodStatusCode.succeeded), message: \"\">"
        #expect(description == expected)
    }

    @Test func testDescriptionWithCustomMessage() {
        let customMessage = "Custom error"
        let status = MethodStatus.failed(message: customMessage)
        let description = status.description
        let expected = "<MethodStatus - code: \(MethodStatusCode.failed), message: \"\(customMessage)\">"
        #expect(description == expected)
    }

    @Test func testDescriptionWithNilMessage() {
        let status = MethodStatus.invalidResult()
        let description = status.description
        let expected = "<MethodStatus - code: \(MethodStatusCode.invalidResult), message: \"\">"
        #expect(description == expected)
    }

    // MARK: - Constants Tests

    @Test func testErrorDomainConstant() {
        #expect(MethodStatus.Constants.ErrorDomain == "com.sparkling.error.domain")
    }

    // MARK: - Edge Cases Tests

    @Test func testAllStatusCodesAreUnique() {
        let allStatuses: [MethodStatus] = [
            .succeeded(),
            .failed(),
            .unregisteredMethod(),
            .invalidParameter(),
            .invalidNamespace(),
            .invalidResult(),
            .unauthorizedAccess(),
            .operationCancelled(),
            .operationTimeout(),
            .notFound(),
            .notImplemented(),
            .alreadyExists(),
            .paramModelTypeWrong(),
            .resultModelTypeWrong(),
            .unknown(),
            .networkUnreachable(),
            .networkTimeout(),
            .malformedResponse(),
        ]

        let codes = allStatuses.map { $0.code }
        let uniqueCodes = Set(codes)
        #expect(codes.count == uniqueCodes.count)
    }

    @Test func testEmptyStringMessage() {
        let status = MethodStatus.failed(message: "")
        #expect(status.message == "")

        let description = status.description
        #expect(description.contains("<MethodStatus - code: 0, message: \"\">"))
    }

    @Test func testMessageWithSpecialCharacters() {
        let specialMessage = "Error: \"quoted\" & <special> characters"
        let status = MethodStatus.failed(message: specialMessage)
        #expect(status.message == specialMessage)
    }

    // MARK: - Comprehensive Status Coverage Tests

    @Test func testAllStatusesHaveValidCodes() {
        let testCases: [(MethodStatus, Int)] = [
            (.succeeded(), 1),
            (.failed(), 0),
            (.unregisteredMethod(), -2),
            (.invalidParameter(), -3),
            (.invalidNamespace(), -4),
            (.invalidResult(), -5),
            (.unauthorizedAccess(), -6),
            (.operationCancelled(), -7),
            (.operationTimeout(), -8),
            (.notFound(), -9),
            (.notImplemented(), -10),
            (.alreadyExists(), -11),
            (.paramModelTypeWrong(), -800),
            (.resultModelTypeWrong(), -801),
            (.unknown(), -1000),
            (.networkUnreachable(), -1001),
            (.networkTimeout(), -1002),
            (.malformedResponse(), -1003),
        ]

        for (status, expectedCode) in testCases {
            #expect(status.code.rawValue == expectedCode)
        }
    }

    @Test func testAllStatusesDefaultToNilMessage() {
        let statuses: [MethodStatus] = [
            .succeeded(),
            .failed(),
            .unregisteredMethod(),
            .invalidParameter(),
            .invalidNamespace(),
            .invalidResult(),
            .unauthorizedAccess(),
            .operationCancelled(),
            .operationTimeout(),
            .notFound(),
            .notImplemented(),
            .alreadyExists(),
            .paramModelTypeWrong(),
            .resultModelTypeWrong(),
            .unknown(),
            .networkUnreachable(),
            .networkTimeout(),
            .malformedResponse(),
        ]

        for status in statuses {
            #expect(status.message == nil)
        }
    }
}
