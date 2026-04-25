// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

@objc
public enum SPKTrackerEventKey: Int {
    case containerExit
    case containerLoadURL
    case viewLoadFailed
    case viewLoadSuccess
    
    public var rawValueString: String {
        switch self {
        case .containerExit: return "hybrid_monitor_container_exit"
        case .containerLoadURL: return "hybrid_monitor_container_load_url"
        case .viewLoadFailed: return "spark_view_load_failed"
        case .viewLoadSuccess: return "spark_view_load_success"
        }
    }
}

public protocol SPKTrackerService {
    func track(event: SPKTrackerEventKey?, metrics: [AnyHashable: AnyHashable]?, category: [AnyHashable: AnyHashable]?, containerView: UIView?)
}
