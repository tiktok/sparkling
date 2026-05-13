// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

public typealias SPKPluginData = (
  version: Int32, initializer: @convention(c) () -> UnsafePointer<CChar>
)

@freestanding(declaration)
public macro spk_register(class: String) =
  #externalMacro(module: "SparklingMacrosImpl", type: "SparklingSectionMacro")
