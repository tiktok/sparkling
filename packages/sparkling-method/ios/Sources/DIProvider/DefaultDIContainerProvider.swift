// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public struct DefaultDIContainerProvider: DIContainerProvider {
    private static let pipeShared = DIContainer()

    public static func inject() {
        DIProviderRegistry.provider = DefaultDIContainerProvider()
    }

    public func pipeShared() -> any DIContainerProtocol {
        return Self.pipeShared
    }

    public func container() -> any DIContainerProtocol {
        return DIContainer()
    }
}
