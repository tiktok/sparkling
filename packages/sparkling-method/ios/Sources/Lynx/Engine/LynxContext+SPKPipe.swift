// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

extension LynxContext {
    private struct AssociatedKeys {
        static var spk_containerID: UInt8 = 0
    }

    var spk_containerID: String? {
        get {
            return objc_getAssociatedObject(self, &AssociatedKeys.spk_containerID) as? String
        }
        set {
            objc_setAssociatedObject(self, &AssociatedKeys.spk_containerID, newValue, .OBJC_ASSOCIATION_COPY_NONATOMIC)
        }
    }
}
