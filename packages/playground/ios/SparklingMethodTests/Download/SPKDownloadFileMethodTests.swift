// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import SparklingMethod
import XCTest

@testable import Sparkling_Media
@testable import Sparkling_Router

class SPKDownloadFileMethodTests: XCTestCase {
    var downloadMethod: SPKDownloadFileMethod!

    override func setUp() {
        super.setUp()
        downloadMethod = SPKDownloadFileMethod()
    }

    override func tearDown() {
        downloadMethod = nil
        super.tearDown()
    }

    func testParameterValidation_MissingURL() {
        let expectation = self.expectation(description: "Missing URL")
        let invalidParams: [String: Any] = ["saveToAlbum": "false"]

        do {
            let params = try SPKDownloadFileMethodParamModel(dictionary: invalidParams)
            downloadMethod.invoke(withParams: params) { status, _ in
                if status != MethodStatus.invalidParameter(message: "") {
                    XCTFail("Should return invalid parameter status")
                }
                expectation.fulfill()
            }
        } catch {
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    func testMethodExecution_ValidParameters() {
        let expectation = self.expectation(description: "Valid parameters")

        let validParams: [String: Any] = [
            "url": "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png",
            "saveToAlbum": "image",
        ]

        do {
            let params = try SPKDownloadFileMethodParamModel(dictionary: validParams)
            downloadMethod.invoke(withParams: params) { _, _ in
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to create parameter model: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 10.0)
    }

    func testSaveToAlbumParameter() {
        let paramsDict: [String: Any] = [
            "url": "https://example.com/image.jpg",
            "saveToAlbum": "image",
        ]

        do {
            let paramModel = try SPKDownloadFileMethodParamModel(dictionary: paramsDict)
            XCTAssertEqual(paramModel.url, "https://example.com/image.jpg")
            XCTAssertNotNil(paramModel.saveToAlbum)
        } catch {
            XCTFail("Failed to create parameter model: \(error)")
        }
    }

    func testTimeoutParameter() {
        let paramsDict: [String: Any] = [
            "url": "https://example.com/image.jpg",
            "timeoutInterval": 30.0,
            "saveToAlbum": "false",
        ]

        do {
            let paramModel = try SPKDownloadFileMethodParamModel(dictionary: paramsDict)
            XCTAssertEqual(paramModel.url, "https://example.com/image.jpg")
        } catch {
            XCTFail("Failed to create parameter model: \(error)")
        }
    }

    func testURLParsingForFileExtension() {
        let testURLs = [
            "https://example.com/image.jpg",
            "https://example.com/file.pdf",
            "https://example.com/data",
        ]

        for url in testURLs {
            let paramsDict: [String: Any] = ["url": url, "saveToAlbum": "false"]
            do {
                let paramModel = try SPKDownloadFileMethodParamModel(dictionary: paramsDict)
                XCTAssertEqual(paramModel.url, url)
            } catch {
                XCTFail("Failed to create parameter model for \(url): \(error)")
            }
        }
    }

    func testEmptyURL() {
        let expectation = self.expectation(description: "Empty URL")
        let paramsDict: [String: Any] = ["url": "", "saveToAlbum": "false"]

        do {
            let params = try SPKDownloadFileMethodParamModel(dictionary: paramsDict)
            downloadMethod.invoke(withParams: params) { status, _ in
                if status != MethodStatus.invalidParameter(message: "") {
                    XCTFail("Should return invalid parameter status")
                }
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to create parameter model: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    func testInvalidURLFormat() {
        let expectation = self.expectation(description: "Invalid URL format")
        let paramsDict: [String: Any] = ["url": "not-a-valid-url", "saveToAlbum": "false"]

        do {
            let params = try SPKDownloadFileMethodParamModel(dictionary: paramsDict)
            downloadMethod.invoke(withParams: params) { _, _ in
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to create parameter model: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    func testSaveToAlbumParameterDifferentFormats() {
        let formats = ["image", "true", "1"]

        for format in formats {
            let paramsDict: [String: Any] = [
                "url": "https://example.com/image.jpg",
                "saveToAlbum": format,
            ]
            do {
                let paramModel = try SPKDownloadFileMethodParamModel(dictionary: paramsDict)
                XCTAssertNotNil(paramModel.saveToAlbum, "saveToAlbum should not be nil for value '\(format)'")
            } catch {
                XCTFail("Failed to create parameter model for format '\(format)': \(error)")
            }
        }
    }
}
