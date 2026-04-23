// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public typealias SPKNavBarHandler = ((UIViewController & SPKContainerProtocol)?) -> Void

@objc
public protocol SPKNavigationBarButtonProtocol {
    var icon: UIImage {set get}
    
    var navBarHandler: SPKNavBarHandler? {set get}
}
