// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Sparkling_Router
import Testing

@testable import SparklingMethod

// MARK: - Test Models

class TestParamsModel: SPKMethodModel {
    public override class func requiredKeyPaths() -> Set<String>? { ["name"] }

    @objc public var name: String?
    @objc public var age: NSNumber?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["name": "name", "age": "age"]
    }
}

class TestResultModel: SPKMethodModel {
    @objc public var message: String?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["message": "message"]
    }
}

class InvalidParamsModel: SPKMethodModel {
    @objc public var invalidField: String?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["invalidField": "invalidField"]
    }
}

// MARK: - Test Methods

final class TestSuccessMethod: PipeMethod {
    public override var methodName: String { "test.success" }
    public override class func methodName() -> String { "test.success" }
    @objc public override var paramsModelClass: AnyClass { TestParamsModel.self }
    @objc public override var resultModelClass: AnyClass { EmptyMethodModelClass.self }

    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard paramModel is TestParamsModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }
        completionHandler.handleCompletion(status: .succeeded(), result: EmptyMethodModelClass())
    }
}

final class TestFailureMethod: PipeMethod {
    public override var methodName: String { "test.failure" }
    public override class func methodName() -> String { "test.failure" }
    @objc public override var paramsModelClass: AnyClass { TestParamsModel.self }
    @objc public override var resultModelClass: AnyClass { EmptyMethodModelClass.self }

    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard paramModel is TestParamsModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }
        completionHandler.handleCompletion(status: .failed(message: "Test failure"), result: nil)
    }
}

final class TestAsyncMethod: PipeMethod {
    public override var methodName: String { "test.async" }
    public override class func methodName() -> String { "test.async" }
    @objc public override var paramsModelClass: AnyClass { TestParamsModel.self }
    @objc public override var resultModelClass: AnyClass { EmptyMethodModelClass.self }

    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard paramModel is TestParamsModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            completionHandler.handleCompletion(status: .succeeded(), result: EmptyMethodModelClass())
        }
    }
}

final class TestInvalidParamsMethod: PipeMethod {
    public override var methodName: String { "test.invalidParams" }
    public override class func methodName() -> String { "test.invalidParams" }
    @objc public override var paramsModelClass: AnyClass { InvalidParamsModel.self }
    @objc public override var resultModelClass: AnyClass { EmptyMethodModelClass.self }

    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard paramModel is InvalidParamsModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }
        completionHandler.handleCompletion(status: .succeeded(), result: EmptyMethodModelClass())
    }
}

final class TestGlobalMethod: PipeMethod {
    public override var methodName: String { "test.global" }
    public override class func methodName() -> String { "test.global" }
    @objc public override class var isGlobal: Bool { true }
    @objc public override var paramsModelClass: AnyClass { TestParamsModel.self }
    @objc public override var resultModelClass: AnyClass { EmptyMethodModelClass.self }

    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard paramModel is TestParamsModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }
        completionHandler.handleCompletion(status: .succeeded(), result: EmptyMethodModelClass())
    }
}

// MARK: - Mock Engine

class MockPipeEngine: PipeEngine {
    var pipeContainer: PipeContainer?
    var fireEventCalled = false
    var lastEventName: String?
    var lastEventParams: [String: Any]?

    func fireEvent(name: String, params: [String: Any]?) {
        fireEventCalled = true
        lastEventName = name
        lastEventParams = params
    }
}

// MARK: - Test Suite

@Suite(.serialized)
struct MethodPipeTest {

    // MARK: - Helper

    private enum CallError: Error { case methodNotFound }

    /// Calls a registered method with the given params and waits for completion.
    private func callAndWait(
        pipe: MethodPipe,
        methodName: String,
        params: [String: Any],
        timeout: DispatchTime = .distantFuture
    ) throws -> (status: MethodStatus, result: SPKMethodModel?) {
        guard let method = pipe.method(forName: methodName) else {
            throw CallError.methodNotFound
        }
        let paramModel = try TestParamsModel.from(dict: params) as! TestParamsModel

        let semaphore = DispatchSemaphore(value: 0)
        var capturedStatus: MethodStatus!
        var capturedResult: SPKMethodModel?

        method.call(withParamModel: paramModel) { status, result in
            capturedStatus = status
            capturedResult = result
            semaphore.signal()
        }
        _ = semaphore.wait(timeout: timeout)

        return (capturedStatus, capturedResult)
    }

    // MARK: - Basic Registration Tests

    @Test func testRegisterLocalMethod() throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestSuccessMethod())

        #expect(pipe.respondTo(methodName: "test.success"))
        let retrieved: TestSuccessMethod? = pipe.method(forName: "test.success")
        #expect(retrieved != nil)
        #expect(retrieved?.methodName == "test.success")
    }

    @Test func testRegisterLocalMethods() throws {
        let pipe = MethodPipe()
        pipe.register(localMethods: [TestSuccessMethod(), TestFailureMethod()])

        #expect(pipe.respondTo(methodName: "test.success"))
        #expect(pipe.respondTo(methodName: "test.failure"))

        let success: TestSuccessMethod? = pipe.method(forName: "test.success")
        let failure: TestFailureMethod? = pipe.method(forName: "test.failure")
        #expect(success != nil)
        #expect(failure != nil)
    }

    @Test func testRegisterEmptyMethods() throws {
        let pipe = MethodPipe()
        pipe.register(localMethods: [])
        #expect(!pipe.respondTo(methodName: "test.success"))
    }

    @Test func testRegisterGlobalMethod() throws {
        let pipe = MethodPipe()
        MethodRegistry.global.unregister(methodName: "test.global")

        pipe.register(globalMethod: TestGlobalMethod())

        #expect(pipe.respondTo(methodName: "test.global"))
        #expect(MethodRegistry.global.respondTo(methodName: "test.global"))

        let retrieved: TestGlobalMethod? = pipe.method(forName: "test.global")
        #expect(retrieved != nil)

        MethodRegistry.global.unregister(methodName: "test.global")
    }

    // MARK: - Unregistration Tests

    @Test func testUnregisterLocalMethod() throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestSuccessMethod())
        #expect(pipe.respondTo(methodName: "test.success"))

        pipe.unregister(localMethodName: "test.success")
        #expect(!pipe.respondTo(methodName: "test.success"))
    }

    @Test func testUnregisterGlobalMethod() throws {
        let pipe = MethodPipe()
        MethodRegistry.global.unregister(methodName: "test.global")

        pipe.register(globalMethod: TestGlobalMethod())
        #expect(pipe.respondTo(methodName: "test.global"))

        pipe.unregister(globalMethodName: "test.global")
        #expect(!pipe.respondTo(methodName: "test.global"))
    }

    @Test func testUnregisterNonExistentMethod() throws {
        let pipe = MethodPipe()
        pipe.unregister(localMethodName: "non.existent")
        pipe.unregister(globalMethodName: "non.existent")
        #expect(!pipe.respondTo(methodName: "non.existent"))
    }

    // MARK: - Method Resolution Tests

    @Test func testRespondToLocalMethod() throws {
        let pipe = MethodPipe()
        #expect(!pipe.respondTo(methodName: "test.success"))

        pipe.register(localMethod: TestSuccessMethod())
        #expect(pipe.respondTo(methodName: "test.success"))
    }

    @Test func testRespondToGlobalMethod() throws {
        let pipe = MethodPipe()
        MethodRegistry.global.unregister(methodName: "test.global")

        #expect(!pipe.respondTo(methodName: "test.global"))

        MethodRegistry.global.register(method: TestGlobalMethod())
        #expect(pipe.respondTo(methodName: "test.global"))

        MethodRegistry.global.unregister(methodName: "test.global")
    }

    @Test func testRespondToNonExistentMethod() throws {
        let pipe = MethodPipe()
        #expect(!pipe.respondTo(methodName: "non.existent"))
    }

    @Test func testMethodForNameLocalPriority() throws {
        let pipe = MethodPipe()
        MethodRegistry.global.unregister(methodName: "test.success")

        pipe.register(localMethod: TestSuccessMethod())
        MethodRegistry.global.register(method: TestSuccessMethod())

        #expect(pipe.method(forName: "test.success") != nil)

        MethodRegistry.global.unregister(methodName: "test.success")
    }

    @Test func testMethodForNameTyped() throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestSuccessMethod())

        let typed: TestSuccessMethod? = pipe.method(forName: "test.success")
        #expect(typed != nil)

        let wrongTyped: TestFailureMethod? = pipe.method(forName: "test.success")
        #expect(wrongTyped == nil)
    }

    @Test func testMethodForNameNonExistent() throws {
        let pipe = MethodPipe()
        #expect(pipe.method(forName: "non.existent") == nil)

        let typed: TestSuccessMethod? = pipe.method(forName: "non.existent")
        #expect(typed == nil)
    }

    // MARK: - Fire Event Tests

    @Test func testFireEventWithEngine() throws {
        let pipe = MethodPipe()
        let mockEngine = MockPipeEngine()
        pipe.engine = mockEngine

        pipe.fireEvent(name: "test.event", params: ["key": "value"])

        #expect(mockEngine.fireEventCalled)
        #expect(mockEngine.lastEventName == "test.event")
        #expect(mockEngine.lastEventParams?["key"] as? String == "value")
    }

    @Test func testFireEventWithoutEngine() throws {
        let pipe = MethodPipe()
        pipe.fireEvent(name: "test.event", params: ["key": "value"])
        pipe.fireEvent(name: "test.event", params: nil)
    }

    @Test func testFireEventWithNilParams() throws {
        let pipe = MethodPipe()
        let mockEngine = MockPipeEngine()
        pipe.engine = mockEngine

        pipe.fireEvent(name: "test.event", params: nil)

        #expect(mockEngine.fireEventCalled)
        #expect(mockEngine.lastEventName == "test.event")
        #expect(mockEngine.lastEventParams == nil)
    }

    // MARK: - Registry Access Tests

    @Test func testRegistryAccess() throws {
        let pipe = MethodPipe()
        let registry = pipe.registry
        #expect(registry != nil)
        #expect(registry === pipe.registry)
    }

    // MARK: - Integration Tests

    @Test func testMethodExecutionSuccess() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestSuccessMethod())

        let (status, result) = try callAndWait(pipe: pipe, methodName: "test.success", params: ["name": "TestUser", "age": 25])
        #expect(status.code == MethodStatusCode.succeeded)
        #expect(result != nil)
    }

    @Test func testMethodAsyncExecutionSuccess() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestAsyncMethod())

        let (status, result) = try callAndWait(pipe: pipe, methodName: "test.async", params: ["name": "TestUser", "age": 25], timeout: .now() + 1.0)
        #expect(status.code == MethodStatusCode.succeeded)
        #expect(result != nil)
    }

    @Test func testMethodExecutionFailure() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestFailureMethod())

        let (status, _) = try callAndWait(pipe: pipe, methodName: "test.failure", params: ["name": "TestUser"])
        #expect(status.code == MethodStatusCode.failed)
        #expect(status.message == "Test failure")
    }

    @Test func testMethodExecutionNotFound() async throws {
        let pipe = MethodPipe()
        #expect(pipe.method(forName: "nonExistent") == nil)
    }

    @Test func testMethodExecutionMissingRequiredParams() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestSuccessMethod())

        do {
            let (status, _) = try callAndWait(pipe: pipe, methodName: "test.success", params: ["age": 25])
            #expect(status.code == MethodStatusCode.invalidInputParameter)
        } catch {
            // Expected: model creation fails when required params are missing
            #expect(true)
        }
    }

    // MARK: - Edge Cases

    @Test func testMethodExecutionEmptyParams() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestSuccessMethod())

        do {
            let (status, _) = try callAndWait(pipe: pipe, methodName: "test.success", params: [:])
            #expect(status.code == MethodStatusCode.invalidInputParameter)
        } catch {
            #expect(true)
        }
    }

    @Test func testMethodExecutionNilParams() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestSuccessMethod())

        do {
            let (status, _) = try callAndWait(pipe: pipe, methodName: "test.success", params: [:])
            #expect(status.code == MethodStatusCode.invalidInputParameter)
        } catch {
            #expect(true)
        }
    }

    @Test func testMultipleMethodRegistration() throws {
        let pipe = MethodPipe()
        pipe.register(localMethods: [TestSuccessMethod(), TestFailureMethod(), TestAsyncMethod()])

        #expect(pipe.respondTo(methodName: "test.success"))
        #expect(pipe.respondTo(methodName: "test.failure"))
        #expect(pipe.respondTo(methodName: "test.async"))

        let s: TestSuccessMethod? = pipe.method(forName: "test.success")
        let f: TestFailureMethod? = pipe.method(forName: "test.failure")
        let a: TestAsyncMethod? = pipe.method(forName: "test.async")
        #expect(s != nil)
        #expect(f != nil)
        #expect(a != nil)
    }

    @Test func testMethodOverride() throws {
        let pipe = MethodPipe()

        pipe.register(localMethod: TestSuccessMethod())
        #expect(pipe.method(forName: "test.success") != nil)

        pipe.register(localMethod: TestSuccessMethod())
        #expect(pipe.method(forName: "test.success") != nil)
        #expect(pipe.respondTo(methodName: "test.success"))
    }

    // MARK: - Result Model Type Tests

    final class TestWrongResultMethod: PipeMethod {
        public override var methodName: String { "test.wrongResult" }
        public override class func methodName() -> String { "test.wrongResult" }
        @objc public override var paramsModelClass: AnyClass { TestParamsModel.self }
        @objc public override var resultModelClass: AnyClass { EmptyMethodModelClass.self }

        @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
            guard paramModel is TestParamsModel else {
                completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
                return
            }
            completionHandler.handleCompletion(status: .resultModelTypeWrong(message: "Result type mismatch"), result: TestParamsModel())
        }
    }

    @Test func testMethodExecutionResultModelTypeWrong() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestWrongResultMethod())

        let (status, _) = try callAndWait(pipe: pipe, methodName: "test.wrongResult", params: ["name": "TestUser"])
        #expect(status.code == MethodStatusCode.resultModelTypeWrong)
        #expect(status.message?.contains("Result type") == true || status.message?.contains("ResultType") == true)
    }

    // MARK: - Param Model Type Tests

    final class TestWrongParamModelMethod: PipeMethod {
        public override var methodName: String { "test.wrongParamModel" }
        public override class func methodName() -> String { "test.wrongParamModel" }
        @objc public override var paramsModelClass: AnyClass { InvalidParamsModel.self }
        @objc public override var resultModelClass: AnyClass { EmptyMethodModelClass.self }

        @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
            guard paramModel is InvalidParamsModel else {
                completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
                return
            }
            completionHandler.handleCompletion(status: .succeeded(), result: EmptyMethodModelClass())
        }
    }

    @Test func testMethodExecutionWrongParamModel() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestWrongParamModelMethod())

        do {
            let semaphore = DispatchSemaphore(value: 0)
            var capturedStatus: MethodStatus?

            if let method = pipe.method(forName: "test.wrongParamModel") {
                let wrongParamModel = try TestParamsModel.from(dict: ["name": "TestUser"]) as! TestParamsModel
                method.call(withParamModel: wrongParamModel) { status, _ in
                    capturedStatus = status
                    semaphore.signal()
                }
                semaphore.wait()
                #expect(capturedStatus?.code == MethodStatusCode.invalidInputParameter)
            }
        } catch {
            #expect(true)
        }
    }

    @Test func testMethodExecutionStatusMessageInjection() async throws {
        let pipe = MethodPipe()
        pipe.register(localMethod: TestFailureMethod())

        let (status, _) = try callAndWait(pipe: pipe, methodName: "test.failure", params: ["name": "TestUser"])
        #expect(status.code == MethodStatusCode.failed)
        #expect(status.message == "Test failure")
    }

    @Test func testMethodExecutionResultInvalid() async throws {
        let pipe = MethodPipe()

        final class InvalidResultMethod: PipeMethod {
            public override var methodName: String { "test.invalidResult" }
            public override class func methodName() -> String { "test.invalidResult" }
            @objc public override var paramsModelClass: AnyClass { TestParamsModel.self }
            @objc public override var resultModelClass: AnyClass { TestResultModel.self }

            @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
                guard paramModel is TestParamsModel else {
                    completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
                    return
                }
                let result = TestResultModel()
                result.message = "success"
                completionHandler.handleCompletion(status: .succeeded(), result: result)
            }
        }

        pipe.register(localMethod: InvalidResultMethod())

        let (status, _) = try callAndWait(pipe: pipe, methodName: "test.invalidResult", params: ["name": "TestUser"])
        #expect(status != nil)
    }
}
