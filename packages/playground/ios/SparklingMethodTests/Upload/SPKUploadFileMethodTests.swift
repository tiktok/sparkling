// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod
import XCTest

@testable import Sparkling_Media
@testable import Sparkling_Router

class MockCompletionHandler: NSObject, PipeMethod.CompletionHandlerProtocol {
    var statusCode: Int = 0
    var statusMessage: String?
    var result: SPKMethodModel?

    func handleCompletion(status: MethodStatus, result: SPKMethodModel?) {
        self.statusCode = status.rawCode
        self.statusMessage = status.message
        self.result = result
    }
}

class SPKUploadFileMethodTests: XCTestCase {
    var uploadMethod: SPKUploadFileMethod!

    override func setUp() {
        super.setUp()
        uploadMethod = SPKUploadFileMethod()
    }

    override func tearDown() {
        uploadMethod = nil
        super.tearDown()
    }

    func testMethodName() {
        XCTAssertEqual(uploadMethod.methodName, "x.uploadFile")
        XCTAssertEqual(SPKUploadFileMethod.methodName(), "x.uploadFile")
    }

    func testParamsModelClass() {
        XCTAssertTrue(uploadMethod.paramsModelClass is SPKUploadFileMethodParamModel.Type)
    }

    func testResultModelClass() {
        XCTAssertTrue(uploadMethod.resultModelClass is SPKUploadFileMethodResultModel.Type)
    }

    func testParamModelCreationWithInvalidParams() {
        XCTAssertThrowsError(
            try SPKUploadFileMethodParamModel(dictionary: [
                "header": ["Content-Type": "application/json"],
                "params": ["key": "value"],
            ]))

        XCTAssertThrowsError(
            try SPKUploadFileMethodParamModel(dictionary: [
                "url": "https://example.com/upload"
            ]))
    }

    func testParamModelCreationWithValidParams() {
        do {
            let params = try SPKUploadFileMethodParamModel(dictionary: [
                "url": "https://example.com/upload",
                "filePath": "/path/to/file.jpg",
                "header": ["Content-Type": "multipart/form-data"],
                "params": ["key": "value"],
            ])

            XCTAssertEqual(params.url, "https://example.com/upload")
            XCTAssertEqual(params.filePath, "/path/to/file.jpg")
            XCTAssertEqual(params.header?["Content-Type"], "multipart/form-data")
            XCTAssertEqual(params.params?["key"] as? String, "value")
        } catch {
            XCTFail("Should not throw: \(error)")
        }
    }

    func testParamModelWithUploadSpecificParams() {
        do {
            let paramModel = try SPKUploadFileMethodParamModel(dictionary: [
                "url": "https://example.com/upload",
                "filePath": "/path/to/image.jpg",
                "name": "uploadField",
                "fileName": "custom_image.jpg",
                "mimeType": "image/jpeg",
            ])

            XCTAssertEqual(paramModel.url, "https://example.com/upload")
            XCTAssertEqual(paramModel.filePath, "/path/to/image.jpg")
            XCTAssertEqual(paramModel.name, "uploadField")
            XCTAssertEqual(paramModel.fileName, "custom_image.jpg")
            XCTAssertEqual(paramModel.mimeType, "image/jpeg")
        } catch {
            XCTFail("Should not throw: \(error)")
        }
    }

    func testParamModelWithTimeoutProperty() {
        do {
            let paramModel = try SPKUploadFileMethodParamModel(dictionary: [
                "url": "https://example.com/upload",
                "filePath": "/path/to/file.jpg",
                "timeoutInterval": 60.0,
            ])

            XCTAssertEqual(paramModel.url, "https://example.com/upload")
            XCTAssertEqual(paramModel.filePath, "/path/to/file.jpg")
            XCTAssertEqual(paramModel.timeoutInterval, 60.0)
        } catch {
            XCTFail("Should not throw: \(error)")
        }
    }

    func testParamModelWithNeedCommonParamsProperty() {
        do {
            let params = try SPKUploadFileMethodParamModel(dictionary: [
                "url": "https://example.com/upload",
                "filePath": "/path/to/file.jpg",
                "needCommonParams": false,
            ])

            XCTAssertEqual(params.url, "https://example.com/upload")
            XCTAssertEqual(params.filePath, "/path/to/file.jpg")
            XCTAssertFalse(params.needCommonParams)
        } catch {
            XCTFail("Should not throw: \(error)")
        }
    }

    func testResultModelInitialization() {
        let resultModel = SPKUploadFileMethodResultModel()

        XCTAssertEqual(resultModel.clientCode, 0)
        XCTAssertEqual(resultModel.httpCode, 0)
        XCTAssertNil(resultModel.header)
        XCTAssertNil(resultModel.responseData)

        resultModel.clientCode = 200
        resultModel.httpCode = 200
        resultModel.header = ["Content-Type": "application/json"]
        resultModel.responseData = ["success": true, "message": "Upload completed"]

        XCTAssertEqual(resultModel.clientCode, 200)
        XCTAssertEqual(resultModel.httpCode, 200)
        XCTAssertEqual(resultModel.header?["Content-Type"], "application/json")
        XCTAssertEqual(resultModel.responseData?["success"] as? Bool, true)
        XCTAssertEqual(resultModel.responseData?["message"] as? String, "Upload completed")
    }

    // MARK: - Call Method Tests

    func testCallWithInvalidParamModel() {
        let completionHandler = MockCompletionHandler()
        uploadMethod.call(withParamModel: "invalid", completionHandler: completionHandler)

        XCTAssertEqual(completionHandler.statusCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(completionHandler.statusMessage, "Invalid parameter model type")
        XCTAssertNil(completionHandler.result)
    }

    func testCallWithMissingURL() {
        do {
            let paramModel = try SPKUploadFileMethodParamModel(dictionary: ["filePath": "/path/to/file.jpg"])
            let completionHandler = MockCompletionHandler()
            uploadMethod.call(withParamModel: paramModel, completionHandler: completionHandler)

            XCTAssertEqual(completionHandler.statusCode, MethodStatusCode.invalidInputParameter.rawValue)
            XCTAssertEqual(completionHandler.statusMessage, "The URL should not be empty.")
            XCTAssertNil(completionHandler.result)
        } catch {
            XCTAssertNotNil(error)
        }
    }

    func testCallWithMissingFilePath() {
        do {
            let paramModel = try SPKUploadFileMethodParamModel(dictionary: ["url": "https://example.com/upload"])
            let completionHandler = MockCompletionHandler()
            uploadMethod.call(withParamModel: paramModel, completionHandler: completionHandler)

            XCTAssertEqual(completionHandler.statusCode, MethodStatusCode.invalidInputParameter.rawValue)
            XCTAssertEqual(completionHandler.statusMessage, "The filePath should not be empty.")
            XCTAssertNil(completionHandler.result)
        } catch {
            XCTAssertNotNil(error)
        }
    }

    func testCallWithNonExistentFile() {
        do {
            let paramModel = try SPKUploadFileMethodParamModel(dictionary: [
                "url": "https://example.com/upload",
                "filePath": "/non/existent/file.jpg",
            ])
            let completionHandler = MockCompletionHandler()
            uploadMethod.call(withParamModel: paramModel, completionHandler: completionHandler)

            XCTAssertEqual(completionHandler.statusCode, MethodStatusCode.invalidInputParameter.rawValue)
            XCTAssertEqual(completionHandler.statusMessage, "The file does not exist at the specified path.")
            XCTAssertNil(completionHandler.result)
        } catch {
            XCTFail("Should not throw: \(error)")
        }
    }

    func testGuessMimeTypeMethod() {
        let tempDir = NSTemporaryDirectory()
        let extensions = ["jpg", "png", "pdf", "mp4", "json", "txt", "unknown"]

        for ext in extensions {
            let tempFilePath = tempDir.appending("test_upload.\(ext)")
            let tempFileURL = URL(fileURLWithPath: tempFilePath)

            do {
                try "test content".data(using: .utf8)!.write(to: tempFileURL)
                defer { try? FileManager.default.removeItem(at: tempFileURL) }

                let paramModel = try SPKUploadFileMethodParamModel(dictionary: [
                    "url": "https://example.com/upload",
                    "filePath": tempFilePath,
                ])
                let completionHandler = MockCompletionHandler()
                uploadMethod.call(withParamModel: paramModel, completionHandler: completionHandler)
            } catch {
                XCTFail("Test failed for .\(ext): \(error)")
            }
        }
    }

    func testCallWithValidParametersAndSuccessResponse() {
        let tempFilePath = NSTemporaryDirectory().appending("test_upload.jpg")
        let tempFileURL = URL(fileURLWithPath: tempFilePath)

        do {
            try "test content".data(using: .utf8)!.write(to: tempFileURL)
            defer { try? FileManager.default.removeItem(at: tempFileURL) }

            let paramModel = try SPKUploadFileMethodParamModel(dictionary: [
                "url": "https://example.com/upload",
                "filePath": tempFilePath,
                "name": "uploadField",
                "fileName": "custom_file.jpg",
                "mimeType": "image/jpeg",
                "params": ["key": "value"],
                "header": ["Authorization": "Bearer token"],
                "timeoutInterval": 30.0,
                "needCommonParams": true,
            ])
            let completionHandler = MockCompletionHandler()
            uploadMethod.call(withParamModel: paramModel, completionHandler: completionHandler)
        } catch {
            XCTFail("Test failed: \(error)")
        }
    }

    func testCallWithValidParametersAndErrorResponse() {
        let tempFilePath = NSTemporaryDirectory().appending("test_upload.txt")
        let tempFileURL = URL(fileURLWithPath: tempFilePath)

        do {
            try "test content".data(using: .utf8)!.write(to: tempFileURL)
            defer { try? FileManager.default.removeItem(at: tempFileURL) }

            let paramModel = try SPKUploadFileMethodParamModel(dictionary: [
                "url": "https://example.com/upload",
                "filePath": tempFilePath,
            ])
            let completionHandler = MockCompletionHandler()
            uploadMethod.call(withParamModel: paramModel, completionHandler: completionHandler)
        } catch {
            XCTFail("Test failed: \(error)")
        }
    }
}
