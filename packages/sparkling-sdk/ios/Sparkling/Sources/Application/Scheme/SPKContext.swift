// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Type alias for a closure that creates loading view instances.
/// 
/// This closure returns a view that conforms to both UIView and SPKLoadingViewProtocol,
/// providing a standardized way to create loading indicators for SPK containers.
public typealias SPKLoadingViewBuilder = () -> (UIView & SPKLoadingViewProtocol)

/// Type alias for a closure that creates error view instances.
/// 
/// This closure takes a view controller and error style, then returns a view that conforms
/// to both UIView and SPKLoadErrorViewProtocol for displaying load failures.
/// 
/// - Parameters:
///   - UIViewController: The container view controller where the error view will be displayed
///   - SPKLoadErrorViewStyle: The style configuration for the error view
public typealias SPKFailedViewBuilder = (UIViewController?) -> (UIView & SPKLoadErrorViewProtocol)

public typealias SPKNavigationBarButtonItemBuilder = ((UIViewController & SPKContainerProtocol)?) -> SPKNavigationBarButtonItem?

/// Enumeration defining the available app theme modes.
/// 
/// This enum provides theme options for customizing the visual appearance
/// of SPK containers and their content.
@objc
public enum SPKAppTheme: Int {
    /// Default theme mode using system preferences
    case SPKAppThemeDefault
    /// Light theme mode with bright colors
    case SPKAppThemeLight
    /// Dark theme mode with dark colors
    case SPKAppThemeDark
}

/// A context class that provides configuration and customization options for SPK containers.
/// 
/// SPKContext extends SPKHybridContext to provide additional container-specific
/// configuration options including custom loading views, error views, navigation bars, theming,
/// and lifecycle management. It supports context merging and copying for flexible configuration
/// inheritance and customization.
/// 
/// The @objcMembers attribute ensures Objective-C compatibility for all properties and methods.
@objcMembers
open class SPKContext: SPKHybridContext {
    /// A closure that creates custom loading view instances.
    /// 
    /// When set, this builder is used to create loading views instead of the default implementation.
    /// The closure should return a view that conforms to both UIView and SPKLoadingViewProtocol.
    public var loadingViewBuilder: SPKLoadingViewBuilder?
    
    /// A closure that creates custom error view instances.
    /// 
    /// When set, this builder is used to create error views for load failures.
    /// The closure receives the container view controller and error style as parameters.
    public var failedViewBuilder: SPKFailedViewBuilder?
    
    /// A pre-configured loading view instance.
    /// 
    /// When set, this view is used directly instead of creating one through the builder.
    /// Takes precedence over loadingViewBuilder if both are set.
    public var loadingView: (UIView & SPKLoadingViewProtocol)?
    
    /// A pre-configured error view instance.
    /// 
    /// When set, this view is used directly for displaying load failures.
    /// Takes precedence over failedViewBuilder if both are set.
    public var loadFailedView: (UIView & SPKLoadErrorViewProtocol)?
    
    /// The navigation bar instance for the container.
    /// 
    /// This property allows customization of the navigation bar appearance and behavior
    /// for SPK containers. When set, it overrides the default navigation bar.
    public var naviBar: (UIView & SPKNavigationBarProtocol)?
    
    /// The background color for the container view.
    /// 
    /// This color is applied to the container's background and affects the overall
    /// visual appearance of the SPK content.
    public var containerBackgroundColor: UIColor?
    
    /// The status bar style for the container.
    /// 
    /// This NSNumber wraps a UIStatusBarStyle value to control the appearance
    /// of the status bar when the container is active.
    public var containerStatusBarStyle: NSNumber?
    
    /// The app theme mode for the container.
    /// 
    /// Determines the visual theme (default, light, or dark) applied to the container
    /// and its content. Defaults to SPKAppThemeDefault.
    public var appTheme: SPKAppTheme = .SPKAppThemeDefault
    
    /// Indicates whether the container should use right-to-left layout.
    /// 
    /// When true, the container adapts its layout for right-to-left languages.
    /// When nil, the system default is used.
    public var isRTL: Bool?
    
    /// Delegate for receiving container lifecycle events.
    /// 
    /// This weak reference receives notifications about container lifecycle changes
    /// such as appearance, disappearance, and other state transitions.
    public weak var containerLifecycleDelegate: SPKContainerLifecycleProtocol?
    
    public var leftNavigationBarButtonItemBuilder: SPKNavigationBarButtonItemBuilder?
    
    public var rightNavigationBarButtonItemBuilder: SPKNavigationBarButtonItemBuilder?
    
    /// Creates a copy of the current SPKContext instance.
    /// 
    /// This method implements NSCopying protocol to create a deep copy of the context.
    /// The copy includes all configuration properties merged from the original instance.
    /// 
    /// - Parameter zone: The memory zone for allocation (typically nil)
    /// - Returns: A new SPKContext instance with copied configuration
    public override func copy(with zone: NSZone? = nil) -> Any {
        let copy = SPKContext()
        if let context = copy as? SPKContext {
            context.merge(withContext: self, isOverride: true)
        }
        return copy
    }
    
    /// Merges configuration from another context into this instance.
    /// 
    /// This method combines properties from the provided context with the current instance.
    /// The merge behavior is controlled by the isOverride parameter, which determines
    /// whether existing values should be replaced or preserved.
    /// 
    /// - Parameters:
    ///   - context: The source context to merge from (must be SPKContext type)
    ///   - isOverride: When true, source values replace existing values; when false, existing values are preserved
    open override func merge(withContext context: SPKHybridContext?, isOverride: Bool = false) {
        guard let context = context as? SPKContext else {
            return
        }
        super.merge(withContext: context, isOverride: isOverride)
        
        self.loadingViewBuilder = SPKHybridContext.merge(withProp: context.loadingViewBuilder,
                                                         to: self.loadingViewBuilder,
                                                         isOverride: isOverride) as? SPKLoadingViewBuilder
        
        self.failedViewBuilder = SPKHybridContext.merge(withProp: context.failedViewBuilder,
                                                        to: self.failedViewBuilder,
                                                        isOverride: isOverride) as? SPKFailedViewBuilder
        
        self.loadingView = SPKHybridContext.merge(withProp: context.loadingView,
                                                  to: self.loadingView,
                                                  isOverride: isOverride) as? (UIView & SPKLoadingViewProtocol)
        
        self.loadFailedView = SPKHybridContext.merge(withProp: context.loadFailedView,
                                                     to: self.loadFailedView,
                                                     isOverride: isOverride) as? (UIView & SPKLoadErrorViewProtocol)
        
        self.naviBar = SPKHybridContext.merge(withProp: context.naviBar,
                                              to: self.naviBar,
                                              isOverride: isOverride) as? (UIView & SPKNavigationBarProtocol)
        
        self.containerBackgroundColor = SPKHybridContext.merge(withProp: context.containerBackgroundColor,
                                                               to: self.containerBackgroundColor,
                                                               isOverride: isOverride) as? UIColor
        
        self.containerStatusBarStyle = SPKHybridContext.merge(withProp: context.containerStatusBarStyle,
                                                              to: self.containerStatusBarStyle,
                                                              isOverride: isOverride) as? NSNumber
        
        self.appTheme = isOverride ? context.appTheme : self.appTheme
        
        self.containerLifecycleDelegate = SPKHybridContext.merge(withProp: context.containerLifecycleDelegate,
                                                                 to: self.containerLifecycleDelegate,
                                                                 isOverride: isOverride) as? SPKContainerLifecycleProtocol
        
        self.leftNavigationBarButtonItemBuilder = SPKHybridContext.merge(withProp: context.leftNavigationBarButtonItemBuilder, to: self.leftNavigationBarButtonItemBuilder, isOverride: isOverride) as? SPKNavigationBarButtonItemBuilder
        
        self.rightNavigationBarButtonItemBuilder = SPKHybridContext.merge(withProp: context.rightNavigationBarButtonItemBuilder, to: self.rightNavigationBarButtonItemBuilder, isOverride: isOverride) as? SPKNavigationBarButtonItemBuilder
        
        self.isRTL = isOverride ? context.isRTL : self.isRTL
    }
    
}
