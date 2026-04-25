// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

/// Enumeration defining the different types of view disappearance events.
///
/// This enum categorizes the various reasons why a SPK container
/// view might disappear, enabling appropriate handling for each scenario.
@objc public enum SPKDisappearType: Int {
    case SPKDisappearTypeUnknown = -1
    
    case SPKDisappearTypeCovered = 0
    
    case SPKDisappearTypeDestroy
    
    case SPKDisappearTypeAppResignActive
}

/// Core protocol defining the interface for SPK hybrid containers.
///
/// This protocol extends SPKBaseProtocol to provide comprehensive
/// container management capabilities including lifecycle handling, view management,
/// toolbar configuration, and hybrid engine integration.
@objc
public protocol SPKContainerProtocol: SPKBaseProtocol {
    
    var hybridInBackground: Bool {get}
    
    var hybridAppear: Bool {get}
    
    var originURL: URL? {get}
    
    var viewType: SPKHybridEngineType {get}
    
    weak var containerLifecycleDelegate: (SPKContainerLifecycleProtocol)? {set get}
    
    var kitView: (UIView & SPKWrapperViewProtocol)? {get}
    
    var bottomToolBar: SPKBottomToolBarProtocol? {get}
    
    var hideBottomToolBar: Bool {get}
    
    var didMount: Bool {get}
    
    var preferredLayoutSize: CGSize {get}
    
    var statusBarStyle: UIStatusBarStyle {set get}
    
    func handleViewDidAppear()
    
    func handleViewDidDisappear()
    
    func handleBecomeActive()
    
    func HandleResignActive()
        
    @objc optional func willDestory() -> Bool
    
    @objc optional func update(_ title: String)
    
    @objc optional func register(_ ui: AnyClass, name: String)
    
    @objc optional func update(_ data: AnyObject, processorName processor: String)
    
    @objc optional func handleViewDidDisappear(withType type: SPKDisappearType)
}

/// Extension providing convenient method pipe registration.
extension SPKContainerProtocol {
    /// Registers pipe methods with the container's kit view.
    ///
    /// This method provides a convenient way to register multiple pipe methods
    /// at once with the container's underlying kit view method pipe.
    ///
    /// - Parameter methods: Array of pipe methods to register, or nil
    public func register(pipeMethods methods: [PipeMethod]?) {
        if let methods = methods {
            self.kitView?.methodPipe?.register(localMethods: methods)
        }
    }
}
