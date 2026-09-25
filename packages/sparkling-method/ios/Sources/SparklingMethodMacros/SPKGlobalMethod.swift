// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/// Declares a Swift method class for global SparklingMethod registration.
///
/// Apply the macro to an extension of an `SPKMethod` subclass, then call
/// `registerDeclaredGlobalMethodsLazily:` during host setup.
///
/// ```swift
/// @SPKGlobalMethod
/// extension ExampleMethod {}
/// ```
@attached(member, names: arbitrary)
public macro SPKGlobalMethod() = #externalMacro(
    module: "SparklingMethodMacroPlugin",
    type: "SPKGlobalMethodMacro"
)
