// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

extension LynxConfig {
    private struct AssociatedKeys {
        static var spk_containerID: UInt8 = 0
        static var spk_namescope: UInt8 = 0
    }

    public var spk_containerID: String? {
        get {
            return objc_getAssociatedObject(self, &AssociatedKeys.spk_containerID) as? String
        }
        set {
            objc_setAssociatedObject(self, &AssociatedKeys.spk_containerID, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }

    public var spk_namescope: String? {
        get {
            return objc_getAssociatedObject(self, &AssociatedKeys.spk_namescope) as? String
        }
        set {
            objc_setAssociatedObject(self, &AssociatedKeys.spk_namescope, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
}
