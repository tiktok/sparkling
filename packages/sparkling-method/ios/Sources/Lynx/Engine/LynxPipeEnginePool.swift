// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

class LynxPipeEnginePool: NSObject {
    static var shared = LynxPipeEnginePool()

    private var engineMap: [String: LynxPipeEngine] = [:]
    private var lock = ReadWriteLock()

    private override init() {}

    static func engine(for containerID: String) -> LynxPipeEngine? {
        return LynxPipeEnginePool.shared.lock.read {
            LynxPipeEnginePool.shared.engineMap[containerID]
        }
    }

    static func setEngine(engine: LynxPipeEngine?, containerID: String) {
        return LynxPipeEnginePool.shared.lock.write {
            LynxPipeEnginePool.shared.engineMap[containerID] = engine
        }
    }

    static func enumerateKeysAndObjects(_ block: (_ key: String, _ value: Any, _ stop: inout Bool) -> Void) {
        var map: [String: LynxPipeEngine]? = nil
        LynxPipeEnginePool.shared.lock.read {
            map = LynxPipeEnginePool.shared.engineMap
        }
        guard let map = map else {
            return
        }
        var shouldStop = false
        for (key, value) in map {
            block(key, value, &shouldStop)
            if shouldStop { break }
        }
    }
}
