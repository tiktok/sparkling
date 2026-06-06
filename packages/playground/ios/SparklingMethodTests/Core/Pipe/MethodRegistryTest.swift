// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import SparklingMethod
import Sparkling_Router
import Testing

@Suite(.serialized)
struct MethodRegistryTest {

    private func clearAutoRegisteredMethods() {
        [
            OpenMethod.methodName(),
            "test.success",
            "test.failure",
            "test.async",
            "test.invalidParams",
            "test.global"
        ].forEach { MethodRegistry.global.unregister(methodName: $0) }
    }

    @Test func testGlobalBasicApis() throws {
        defer { MethodRegistry.global.unregister(methodName: OpenMethod.methodName()) }

        MethodRegistry.global.register(methodType: OpenMethod.self)
        #expect(MethodRegistry.global.respondTo(methodName: OpenMethod.methodName()))

        let method = MethodRegistry.global.method(forName: OpenMethod.methodName())
        #expect(method != nil)

        let methodName = type(of: method!).methodName()
        #expect(methodName == OpenMethod.methodName())
    }

    @Test func testGlobalUnregister() throws {
        MethodRegistry.global.unregister(methodName: OpenMethod.methodName())
        #expect(!MethodRegistry.global.respondTo(methodName: OpenMethod.methodName()))
    }

    @Test func testAutoRegister() throws {
        defer { clearAutoRegisteredMethods() }

        MethodRegistry.autoRegisterGlobalMethods()
        #expect(MethodRegistry.global.respondTo(methodName: OpenMethod.methodName()))
        let openMethod = MethodRegistry.global.method(forName: OpenMethod.methodName())
        #expect(openMethod is OpenMethod)
    }
}
