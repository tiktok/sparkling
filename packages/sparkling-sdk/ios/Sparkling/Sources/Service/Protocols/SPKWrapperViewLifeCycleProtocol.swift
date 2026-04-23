// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

@objc public protocol SPKWrapperViewLifecycleProtocol {
    
    @objc optional
    func viewDidCreate(_: SPKWrapperViewProtocol?)
    
    @objc optional
    func view(_ : SPKWrapperViewProtocol, didChangeIntrinsicContentSize size: CGSize)
    
    @objc optional
    func viewBeforeLoading(_ view: SPKWrapperViewProtocol?)
    
    @objc optional
    func viewWillStartLoading(_ view: SPKWrapperViewProtocol?)
    
    @objc optional
    func viewDidStartLoading(_ view: SPKWrapperViewProtocol?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, willLoadRequest url:URL?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, didStartFetchResourceWithURL url:URL?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, didFetchedResource resource: SPKResourceProtocol?, error: Error?)

    @objc optional
    func viewDidFirstScreen(_ view: SPKWrapperViewProtocol?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, didFinishLoadWithURL url:URL?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, didLoadFailedWithURL url: URL?, error: Error?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, willStartUpdateWithURL url: URL?)
    
    @objc optional
    func viewDidPageUpdate(_ view: SPKWrapperViewProtocol?)
    
    @objc optional
    func viewDidConstructJSRuntime(_ view: SPKWrapperViewProtocol?)
    
    @objc optional
    func viewDidUpdate(_ view: SPKWrapperViewProtocol?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, didReceiveError error: Error?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, didReceivePerformance perfDict: Dictionary<AnyHashable, Any>?)
    
    @objc optional
    func view(_ view: SPKWrapperViewProtocol?, updateTitle title:String)
    
    @objc optional
    func viewWillDealloc(_ view: SPKWrapperViewProtocol?)
}

