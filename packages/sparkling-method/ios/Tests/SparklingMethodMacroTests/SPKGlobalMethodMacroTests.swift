import SwiftSyntax
import SwiftSyntaxBuilder
import SwiftSyntaxMacros
import SwiftSyntaxMacrosTestSupport
import XCTest

#if canImport(SparklingMethodMacroPlugin)
import SparklingMethodMacroPlugin
#endif

final class SPKGlobalMethodMacroTests: XCTestCase {
    func testMacro() throws {
        #if canImport(SparklingMethodMacroPlugin)
        let testMacros: [String: Macro.Type] = [
            "SPKGlobalMethod": SPKGlobalMethodMacro.self,
        ]
        assertMacroExpansion(
            """
            @SPKGlobalMethod
            extension ExampleMethod {}
            """,
            expandedSource: """
            extension ExampleMethod {
                #if swift(>=5.10)
                @_used
                @_section("__DATA,SPKSwiftMethods")
                static let spkGlobalMethodClassGetter_ExampleMethod: @convention(c) () -> UnsafePointer<CChar> = {
                    UnsafeRawPointer(("TestModule.ExampleMethod" as StaticString).utf8Start).assumingMemoryBound(to: CChar.self)
                }
                #endif
            }
            """,
            macros: testMacros,
            file: "TestModule/test.swift"
        )
        #else
        throw XCTSkip("Macros are only supported when running tests for the host platform")
        #endif
    }
}
