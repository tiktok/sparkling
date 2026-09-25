// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public enum DIProviderRegistry {
    private static var _provider: DIContainerProvider?

    public static var provider: DIContainerProvider! {
        get {
            guard let instance = _provider else {
                fatalError("DIContainerProvider has not been injected. Please import DIProvider subspec then inject it or inject a custom provider before accessing it.")
            }
            return instance
        }
        set {
            _provider = newValue
        }
    }
}
