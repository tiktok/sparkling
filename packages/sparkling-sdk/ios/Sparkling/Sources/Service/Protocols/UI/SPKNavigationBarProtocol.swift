// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import UIKit

@objc
public protocol SPKNavigationBarProtocol {
    
    weak var container: (UIViewController & SPKContainerProtocol)? {set get}
    
    static func navigationBar() -> (UIView & SPKNavigationBarProtocol)
    
    @objc optional
    func setup(leftButton barButtonItem: SPKNavigationBarButtonProtocol)
    
    var didTapLeftButtonActionBlock: (() -> Void)? {set get}
    
    @objc optional
    func setup(rightButton barButtonItem: SPKNavigationBarButtonProtocol)
    
    var didTapRightButtonActionBlock: (() -> Void)? {set get}
    
    func update(centerTitle title: String)
    func update(titleColor color: UIColor)
    
    @objc optional
    func update(backgroundColor color: UIColor)
    
    func set(navigationBarBackButtonEnable enable: Bool)
    
    @objc optional
    func attachToContainer(_ params: SPKHybridSchemeParam)
    
    @objc optional
    func show(backButton isShow: Bool)
}
