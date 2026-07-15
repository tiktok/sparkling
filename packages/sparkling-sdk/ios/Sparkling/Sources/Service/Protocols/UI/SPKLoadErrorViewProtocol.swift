// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public typealias SPKLoadErrorRefreshBlock = () -> Void

@objc
public protocol SPKLoadErrorViewProtocol {
    /// Registers a callback that the error view may retain and invoke later.
    @objc optional func register(refreshBlock: @escaping SPKLoadErrorRefreshBlock)

    @objc optional func container(_ contianer: SPKContainerProtocol, didReceiveError error: Error?)

}
