// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

@objcMembers
public class SPKLynxModuleService: NSObject, LynxServiceModuleProtocol {
    public func initLynxViewGroup(_ lynxViewGroup: LynxViewGroup) {

    }

    public func cloneGlobalProps(forReload lynxView: LynxView) {

    }

    public static func serviceScope() -> LynxServiceScope {
        return .default
    }

    public func initGlobalProps(_ lynxView: LynxView) {}

    public func clearModule(forDestroy lynxView: LynxView) {
        lynxView.spk_clearModuleForDestroy()
    }
}
