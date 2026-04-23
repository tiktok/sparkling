// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

@objc
public protocol SPKContainerLifecycleProtocol {
    @objc optional func container(_ container: SPKContainerProtocol, didChangeIntrinsicContentSize size:(CGSize))
    
    @objc optional func containerBeforeLoading(_ container: SPKContainerProtocol)
    
    @objc optional func containerWillStartLoading(_ contanier: SPKContainerProtocol)
    
    @objc optional func containerDidStartLoading(_ container: SPKContainerProtocol)
    
    @objc optional func container(_ container: SPKContainerProtocol, didStartFetchResourceWithURL url: URL?)
    
    @objc optional func container(_ container: SPKContainerProtocol, didFetchedResource resource: SPKResourceProtocol?, error: Error?)
    
    @objc optional func containerDidFirstScreen(_ container: SPKContainerProtocol)
    
    @objc optional func container(_ container: SPKContainerProtocol, didFinishLoadWithURL url: URL?)
    
    @objc optional func container(_ container: SPKContainerProtocol, didLoadFailedWithURL url: URL?, error: Error?)
    
    @objc optional func containerDidUpdate(_ container: SPKContainerProtocol)
    
    @objc optional func containerDidPageUpdate(_ container: SPKContainerProtocol)
    
    @objc optional func container(_ container: SPKContainerProtocol, didRecieveError error: Error?)
    
    @objc optional func container(_ container: SPKContainerProtocol, didReceivePerformance perfDict: [AnyHashable: Any]?)
    
    
    @objc optional func container(_ container: SPKContainerProtocol, updateTitle title: String)
    
    @objc optional func containerWillReload(_ container: SPKContainerProtocol)
    
    @objc optional func containerDidConstructJSRuntime(_ container: SPKContainerProtocol)
    
    @objc optional func containerDidInit(_ container: SPKContainerProtocol)
    
    @objc optional func containerViewDidLoad(_ container: SPKContainerProtocol)
    
    @objc optional func containerViewWillAppear(_ container: SPKContainerProtocol)
    
    @objc optional func containerViewDidAppear(_ container: SPKContainerProtocol)
    
    @objc optional func containerViewWillDisappear(_ container: SPKContainerProtocol)
    
    @objc optional func containerViewDidDisappear(_ container: SPKContainerProtocol)
    
    @objc optional func containerViewHandleAppWillResignActive(_ container: SPKContainerProtocol)
    
    @objc optional func containerViewHandleAppDidBecomeActive(_ container: SPKContainerProtocol)
}

