// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

@objcMembers
public class SparklingDebugTool: NSObject {
    public static func setup() {
        LynxEnv.sharedInstance().lynxDebugEnabled = true
        LynxEnv.sharedInstance().devtoolEnabled = true
        LynxEnv.sharedInstance().logBoxEnabled = true
    }
}
