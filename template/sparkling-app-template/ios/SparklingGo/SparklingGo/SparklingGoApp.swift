// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Lynx
import SDWebImage
import SDWebImageWebPCoder
import Sparkling
import SparklingMacro
import SparklingMethod
import SwiftUI

#if canImport(Sparkling_DebugTool)
    import Sparkling_DebugTool
#endif

class AppDelegate: NSObject, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        window = UIWindow(frame: UIScreen.main.bounds)
        let webPCoder = SDImageWebPCoder.shared
        SDImageCodersManager.shared.addCoder(webPCoder)
        #if canImport(Sparkling_DebugTool)
            SparklingDebugTool.setup()
        #endif
        SPKServiceRegister.registerAll()
        SPKExecuteAllPrepareBootTask()
        SPKKit.DIContainer.register(SPKTrackerService.self, scope: ServiceScope.transient) {
            SparklingGoTrackerService()
        }
        return true
    }
}

@main
struct SparklingGoApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    var body: some Scene {
        WindowGroup {
            DemoVC()
        }
    }
}
