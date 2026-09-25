import Foundation
#if SWIFT_PACKAGE
import SwiftCompilerPlugin
#endif
import SwiftSyntax
import SwiftSyntaxBuilder
import SwiftSyntaxMacros

struct SPKGlobalMethodMacroError: Error, CustomStringConvertible {
    let message: String
    var description: String { message }
}

public struct SPKGlobalMethodMacro: MemberMacro {
    public static func expansion(
        of node: AttributeSyntax,
        providingMembersOf declaration: some DeclGroupSyntax,
        in context: some MacroExpansionContext
    ) throws -> [DeclSyntax] {
        guard
            let fileID = context.location(of: node)?.file
                .as(StringLiteralExprSyntax.self)?.segments.first?
                .as(StringSegmentSyntax.self)?.content.text.utf8,
            let moduleName = fileID.firstIndex(where: { $0 == UInt8(ascii: "/") })
                .flatMap({ slashIndex in String(fileID[..<slashIndex]) })
        else {
            throw SPKGlobalMethodMacroError(message: "Unable to determine the declaring module.")
        }

        guard
            let className = declaration.as(ExtensionDeclSyntax.self)?.extendedType
                .as(SimpleTypeIdentifierSyntax.self)?.name.text
        else {
            throw SPKGlobalMethodMacroError(
                message: "Incorrect usage. Use `@SPKGlobalMethod extension ExampleMethod {}`."
            )
        }

        return ["""
        #if swift(>=5.10)
        @_used
        @_section("__DATA,SPKSwiftMethods")
        static let spkGlobalMethodClassGetter_\(raw: className): @convention(c) () -> UnsafePointer<CChar> = {
            UnsafeRawPointer(("\(raw: moduleName).\(raw: className)" as StaticString).utf8Start).assumingMemoryBound(to: CChar.self)
        }
        #endif
        """]
    }
}

#if SWIFT_PACKAGE
@main
struct SparklingMethodMacroPlugin: CompilerPlugin {
    let providingMacros: [Macro.Type] = [
        SPKGlobalMethodMacro.self,
    ]
}
#endif
