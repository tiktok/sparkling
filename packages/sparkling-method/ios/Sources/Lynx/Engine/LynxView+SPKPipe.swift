// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

extension LynxView: PipeContainer {
    public var spk_containerID: String? {
        get {
            return self.containerID
        }
        set {
            self.containerID = newValue ?? ""
        }
    }
}

extension LynxView {
    private struct AssociatedKeys {
        static var spk_pipeEngine: UInt8 = 0
        static var spk_isLynxViewDestroying: UInt8 = 0
    }

    var spk_pipeEngine: LynxPipeEngine? {
        get {
            var pipeEngine = objc_getAssociatedObject(self, &AssociatedKeys.spk_pipeEngine) as? LynxPipeEngine ?? LynxPipeEnginePool.engine(for: self.containerID)
            if pipeEngine == nil {
                pipeEngine = LynxPipeEngine(withLynxView: self)
                LynxPipeEnginePool.setEngine(engine: pipeEngine, containerID: self.containerID)
                objc_setAssociatedObject(self, &AssociatedKeys.spk_pipeEngine, pipeEngine, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
            }
            return pipeEngine
        }
        set {
            objc_setAssociatedObject(self, &AssociatedKeys.spk_pipeEngine, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }

    var spk_isLynxViewDestorying: Bool {
        get {
            return (objc_getAssociatedObject(self, &AssociatedKeys.spk_isLynxViewDestroying) as? NSNumber)?.boolValue ?? false
        }
        set {
            objc_setAssociatedObject(self, &AssociatedKeys.spk_isLynxViewDestroying, NSNumber(value: newValue), .OBJC_ASSOCIATION_RETAIN)
        }
    }

    private func spk_getLynxContextIfAvailable() -> LynxContext? {
        perform(#selector(getLynxContext))?.takeUnretainedValue() as? LynxContext
    }

    func spk_clearModuleForDestroy() {
        if !spk_isLynxViewDestorying {
            if let lynxContext = spk_getLynxContextIfAvailable() {
                lynxContext.spk_containerID = self.containerID
                if let pipeEngine = self.spk_pipeEngine, let namescope = self.namescope {
                    pipeEngine.namescope = namescope
                }
            } else if let containerID = self.containerID as? String {
                CoreUtils.onMain {
                    LynxPipeEnginePool.setEngine(engine: nil, containerID: containerID)
                }
            }
        }
        spk_isLynxViewDestorying = true
    }
}
