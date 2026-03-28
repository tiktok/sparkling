// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod
import Sparkling_Storage

let sampleSuiteName = "com.SPK.custom.userdefault"

class StorageServiceImpl: StorageService {
    
    private let userDefaults = UserDefaults(suiteName: sampleSuiteName) ?? UserDefaults.standard
    
    func setObject(key: String, value: Any) {
        userDefaults.set(value, forKey: key)
    }
    
    func object(forKey key: String) -> Any? {
        return userDefaults.object(forKey: key)
    }
    
    func removeObject(forKey key: String) {
        userDefaults.removeObject(forKey: key)
    }
}
