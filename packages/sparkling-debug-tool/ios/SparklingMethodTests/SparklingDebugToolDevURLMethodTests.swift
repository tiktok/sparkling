// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import SparklingMethod
import XCTest
@testable import Sparkling_DebugTool

private final class DebugToolCompletionRecorder: NSObject, PipeMethod.CompletionHandlerProtocol {
    var status: MethodStatus?
    var result: SPKMethodModel?

    func handleCompletion(status: MethodStatus, result: SPKMethodModel?) {
        self.status = status
        self.result = result
    }
}

final class SparklingDebugToolDevURLMethodTests: XCTestCase {
    override func setUp() {
        super.setUp()
        SparklingDebugTool.setDevURL("")
    }

    override func tearDown() {
        SparklingDebugTool.setDevURL("")
        super.tearDown()
    }

    func testMethodNamesAndModels() {
        XCTAssertEqual(GetDevUrlMethod.methodName(), "debugtool.getDevUrl")
        XCTAssertEqual(GetDevUrlMethod().methodName, "debugtool.getDevUrl")
        XCTAssertTrue(GetDevUrlMethod().paramsModelClass is EmptyMethodModelClass.Type)
        XCTAssertTrue(GetDevUrlMethod().resultModelClass is GetDevUrlMethodResultModel.Type)

        XCTAssertEqual(SetDevUrlMethod.methodName(), "debugtool.setDevUrl")
        XCTAssertEqual(SetDevUrlMethod().methodName, "debugtool.setDevUrl")
        XCTAssertTrue(SetDevUrlMethod().paramsModelClass is SetDevUrlMethodParamModel.Type)
        XCTAssertTrue(SetDevUrlMethod().resultModelClass is EmptyMethodModelClass.Type)
    }

    func testGetDevUrlReturnsPersistedUrlInResultModel() throws {
        SparklingDebugTool.setDevURL("  http://127.0.0.1:5969/  ")
        let recorder = DebugToolCompletionRecorder()

        GetDevUrlMethod().call(
            withParamModel: EmptyMethodModelClass(),
            completionHandler: recorder
        )

        XCTAssertEqual(recorder.status?.code, .succeeded)
        let result = try XCTUnwrap(recorder.result as? GetDevUrlMethodResultModel)
        XCTAssertEqual(result.url, "http://127.0.0.1:5969/")
        XCTAssertEqual(try result.toDict()?["url"] as? String, "http://127.0.0.1:5969/")
    }

    func testGetDevUrlReturnsEmptyStringWhenNoUrlIsPersisted() {
        let recorder = DebugToolCompletionRecorder()

        GetDevUrlMethod().call(
            withParamModel: EmptyMethodModelClass(),
            completionHandler: recorder
        )

        XCTAssertEqual(recorder.status?.code, .succeeded)
        XCTAssertEqual(
            (recorder.result as? GetDevUrlMethodResultModel)?.url,
            ""
        )
    }

    func testSetDevUrlPersistsTrimmedHttpUrl() throws {
        let params = try SetDevUrlMethodParamModel(
            dictionary: ["url": "  https://example.com:5969/main.lynx.bundle  "]
        )
        let recorder = DebugToolCompletionRecorder()

        SetDevUrlMethod().call(withParamModel: params, completionHandler: recorder)

        XCTAssertEqual(recorder.status?.code, .succeeded)
        XCTAssertNil(recorder.result)
        XCTAssertEqual(
            SparklingDebugTool.devURL(fallback: ""),
            "https://example.com:5969/main.lynx.bundle"
        )
    }

    func testSetDevUrlRejectsMissingEmptyAndMalformedUrls() throws {
        let values: [String?] = [
            nil,
            "",
            "   ",
            "ftp://example.com/main.lynx.bundle",
            "http://",
            "http:///main.lynx.bundle",
            "http://exa mple.com",
            "not a url",
        ]
        for url in values {
            let params = try SetDevUrlMethodParamModel(dictionary: url.map { ["url": $0] } ?? [:])
            let recorder = DebugToolCompletionRecorder()

            SetDevUrlMethod().call(withParamModel: params, completionHandler: recorder)

            XCTAssertEqual(recorder.status?.code, .invalidInputParameter)
            XCTAssertEqual(
                recorder.status?.message,
                "The url must be a valid http:// or https:// URL"
            )
            XCTAssertEqual(SparklingDebugTool.devURL(fallback: ""), "")
        }
    }

    func testMethodsAreDiscoveredByGlobalAutoRegistration() {
        MethodRegistry.global.unregister(methodName: GetDevUrlMethod.methodName())
        MethodRegistry.global.unregister(methodName: SetDevUrlMethod.methodName())
        defer {
            MethodRegistry.global.unregister(methodName: GetDevUrlMethod.methodName())
            MethodRegistry.global.unregister(methodName: SetDevUrlMethod.methodName())
        }

        MethodRegistry.autoRegisterGlobalMethods()

        XCTAssertTrue(
            MethodRegistry.global.respondTo(methodName: GetDevUrlMethod.methodName())
        )
        XCTAssertTrue(
            MethodRegistry.global.respondTo(methodName: SetDevUrlMethod.methodName())
        )
    }
}
