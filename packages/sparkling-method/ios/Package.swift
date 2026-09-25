// swift-tools-version: 6.0

import CompilerPluginSupport
import PackageDescription

let package = Package(
    name: "SparklingMethod",
    platforms: [
        .macOS(.v10_15),
        .iOS(.v14),
    ],
    products: [
        .library(name: "SparklingMethod", targets: ["SparklingMethod"]),
        .library(name: "SparklingMethodMacros", targets: ["SparklingMethodMacros"]),
    ],
    dependencies: [
        .package(url: "https://github.com/Mantle/Mantle.git", exact: "2.2.0"),
        .package(url: "https://github.com/swiftlang/swift-syntax.git", branch: "release/5.9.0"),
    ],
    targets: [
        .target(
            name: "SparklingMethod",
            dependencies: [
                .product(name: "Mantle", package: "Mantle"),
            ],
            path: "Sources/SparklingMethod",
            exclude: [
                "Implementation/Transport/Lynx",
            ],
            publicHeadersPath: "include",
            cSettings: [
                .headerSearchPath("include/SparklingMethod"),
                .headerSearchPath("Implementation/Core"),
                .headerSearchPath("Implementation/Events"),
                .headerSearchPath("Implementation/Registry"),
                .headerSearchPath("Implementation/Runtime"),
                .headerSearchPath("Implementation/Runtime/Internal"),
                .unsafeFlags(["-fobjc-arc"]),
            ],
            linkerSettings: [
                .linkedFramework("WebKit"),
            ]
        ),
        .macro(
            name: "SparklingMethodMacroPlugin",
            dependencies: [
                .product(name: "SwiftSyntaxMacros", package: "swift-syntax"),
                .product(name: "SwiftCompilerPlugin", package: "swift-syntax"),
            ],
            path: "Sources/SparklingMethodMacroPlugin"
        ),
        .target(
            name: "SparklingMethodMacros",
            dependencies: ["SparklingMethodMacroPlugin"],
            path: "Sources/SparklingMethodMacros"
        ),
        .testTarget(
            name: "SparklingMethodTests",
            dependencies: ["SparklingMethod"],
            path: "Tests/SparklingMethodTests"
        ),
        .testTarget(
            name: "SparklingMethodMacroTests",
            dependencies: [
                "SparklingMethodMacros",
                "SparklingMethodMacroPlugin",
                .product(name: "SwiftSyntaxMacrosTestSupport", package: "swift-syntax"),
            ],
            path: "Tests/SparklingMethodMacroTests",
            swiftSettings: [
                .unsafeFlags([
                    "-enable-experimental-feature",
                    "SymbolLinkageMarkers",
                    "-D_SYMBOL_LINKAGE_MARKERS_ENABLED",
                ]),
            ]
        ),
    ]
)
