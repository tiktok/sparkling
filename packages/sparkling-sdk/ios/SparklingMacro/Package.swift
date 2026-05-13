// swift-tools-version: 6.0
import CompilerPluginSupport
// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import PackageDescription

let package = Package(
  name: "SparklingMacro",
  platforms: [
    .macOS(.v10_15), .iOS(.v13), .tvOS(.v13), .watchOS(.v6), .macCatalyst(.v13),
  ],
  products: [
    // SPM: import this library to use #spk_register
    .library(name: "RegisterMacro", targets: ["RegisterMacro"])
  ],
  dependencies: [
    .package(
      url: "https://github.com/swiftlang/swift-syntax.git",
      from: "600.0.0-latest")
  ],
  targets: [
    // macro plugin implementation; also the CocoaPods-distributed executable
    .macro(
      name: "SparklingMacrosImpl",
      dependencies: [
        .product(name: "SwiftSyntaxMacros", package: "swift-syntax"),
        .product(name: "SwiftCompilerPlugin", package: "swift-syntax"),
      ]
    ),
    // SPM library wrapper that exposes the public macro declarations
    .target(
      name: "RegisterMacro",
      dependencies: ["SparklingMacrosImpl", "RegisterMacroInternal"],
      path: "Sources/RegisterMacro",
      exclude: ["Objc"]
    ),
    .target(
      name: "RegisterMacroInternal",
      path: "Sources/RegisterMacro",
      exclude: ["Swift"],
      publicHeadersPath: "Objc"
    ),
  ]
)
