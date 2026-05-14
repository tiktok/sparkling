// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SystemConfiguration

/// Network reachability utility class
public class SPKReachability: NSObject {
    private var reachability: SCNetworkReachability?

    /// Check if network is reachable
    class func isReachable() -> Bool {
        var zeroAddress = sockaddr_in()
        zeroAddress.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        zeroAddress.sin_family = sa_family_t(AF_INET)

        guard
            let reachability = withUnsafePointer(
                to: &zeroAddress,
                {
                    $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                        SCNetworkReachabilityCreateWithAddress(nil, $0)
                    }
                })
        else {
            return false
        }

        var flags: SCNetworkReachabilityFlags = []
        if !SCNetworkReachabilityGetFlags(reachability, &flags) {
            return false
        }

        let isReachable = flags.contains(.reachable)
        let needsConnection = flags.contains(.connectionRequired)
        let canConnectAutomatically = flags.contains(.connectionOnDemand) || flags.contains(.connectionOnTraffic)
        let canConnectWithoutUserInteraction = canConnectAutomatically && !flags.contains(.interventionRequired)

        return isReachable && (!needsConnection || canConnectWithoutUserInteraction)
    }

    /// Singleton method
    class func sharedInstance() -> SPKReachability {
        struct Static {
            static let instance: SPKReachability = SPKReachability()
        }
        return Static.instance
    }
}
