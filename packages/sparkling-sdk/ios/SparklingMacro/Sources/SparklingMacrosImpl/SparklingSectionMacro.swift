// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import SwiftCompilerPlugin
import SwiftSyntax
import SwiftSyntaxBuilder
import SwiftSyntaxMacros

private let kSPKPluginDataVersion: Int32 = 1

public enum SparklingSectionMacroError: Error {
  case macroKeysError
  case macroKeysTypeError
}

public struct SparklingSectionMacro: DeclarationMacro {

  public static func expansion(
    of node: some SwiftSyntax.FreestandingMacroExpansionSyntax,
    in context: some SwiftSyntaxMacros.MacroExpansionContext
  ) throws -> [SwiftSyntax.DeclSyntax] {
    let lists = node.arguments
    guard lists.count == 1 else {
      throw SparklingSectionMacroError.macroKeysError
    }
    var className: String = ""
    for argument in lists {
      if argument.label?.text == "class" {
        if let stringLiteral = argument.expression.as(
          StringLiteralExprSyntax.self),
          let hostValue = stringLiteral.segments.first?.as(
            StringSegmentSyntax.self)?.content.text
        {
          className = hostValue
        } else {
          throw SparklingSectionMacroError.macroKeysTypeError
        }
      }
    }

    let infoName = "\(context.makeUniqueName(className))"
    // Embed the version check in the generated code so the user's compiler resolves it,
    // regardless of which Swift version was used to build this macro plugin binary.
    // @used and @section were officially released in Swift 6.3; earlier versions use the underscored variants.
    let declartionString = """
      #if swift(>=6.3)
      @used
      @section("__DATA,SPK_PRE_SVC")
      #else
      @_used
      @_section("__DATA,SPK_PRE_SVC")
      #endif
      let \(infoName) = SPKPluginData(
        version: \(kSPKPluginDataVersion),
        initializer: {UnsafeRawPointer(("\(className)" as StaticString).utf8Start).assumingMemoryBound(to: CChar.self)}
      )
      """
    return [
      DeclSyntax(stringLiteral: declartionString)
    ]
  }
}

@main
struct SparklingMacroPlugin: CompilerPlugin {
  let providingMacros: [Macro.Type] = [
    SparklingSectionMacro.self
  ]
}
