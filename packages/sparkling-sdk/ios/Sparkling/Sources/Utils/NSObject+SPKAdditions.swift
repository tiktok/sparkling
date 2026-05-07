// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

extension NSObject: SPKKitCompatible {}

public extension SPKKitWrapper where Base: NSObject {
    func setAttachedObject<T>(key: String, object: T?, weak: Bool = false) {
        base.spk_attach(object, forKey: key, isWeak: weak)
    }
    func getAttachedObject<T>(key: String, weak: Bool = false) -> T? {
        guard let object = base.spk_getAttachedObject(forKey: key, isWeak: weak) as? T else {
            return nil
        }
        return object
    }
}
