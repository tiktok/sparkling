// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SnapKit
import SparklingMethod
import UIKit

/// A view controller that manages SPK hybrid content with navigation and lifecycle support.
/// 
/// This class provides a complete container for hybrid content, handling navigation bars,
/// status bar management, lifecycle events, and integration with the SPK framework.
/// It supports both web and native content rendering through the underlying SPKContainerView.
@objcMembers
open class SPKViewController: UIViewController, SPKContainerProtocol {
    
    // MARK: - Layout Constants (Internal Only)
    /// Internal layout constants for consistent UI measurements.
    struct LayoutConstants {
        /// Standard navigation bar height used throughout the application.
        static let navigationBarHeight: CGFloat = 44.0
    }
    
    /// The top offset for content positioning based on navigation and status bar visibility.
    /// 
    /// This computed property calculates the appropriate top offset for content based on:
    /// - Status bar transparency settings
    /// - Navigation bar visibility
    /// - Status bar visibility
    /// - Device type (iPhone X series considerations)
    var topOffset: CGFloat {
        get {
            var offset = 0.0
            let config = self.config as? SPKSchemeParam
            if config?.transStatusBar == true {
                offset = 0.0
            } else {
                if config?.hideNavBar == false {
                    offset += LayoutConstants.navigationBarHeight
                }
                if config?.hideStatusBar == false || ((UIDevice.spk.isIPhoneXSeries && config?.hideNavBar == false) && CGRectGetWidth(self.view.frame) < CGRectGetHeight(self.view.frame)) {
                    offset += self.topSafeAreaHeight
                }
            }
            return offset
        }
    }
    
    var topSafeAreaHeight: CGFloat {
        if self.isViewLoaded {
            let fromWindow = self.view.window?.safeAreaInsets.top ?? 0
            let fromView = self.view.safeAreaInsets.top
            return max(fromWindow, fromView)
        }
        return UIApplication.spk.mainWindow?.spk.safeAreaInsets.top ?? 0
    }
    
    /// The total navigation bar height including status bar when applicable.
    /// 
    /// This computed property calculates the complete navigation bar height by combining
    /// the standard navigation bar height with the status bar height when appropriate.
    var navigationBarHeight: CGFloat {
        var height = LayoutConstants.navigationBarHeight
        let config = config as? SPKSchemeParam
        if (config?.hideStatusBar == false || UIDevice.spk.isIPhoneXSeries) {
            height += self.topSafeAreaHeight
        }
        return height
    }
    
    /// Controls whether pan-to-close gesture is managed externally.
    /// 
    /// When true, the pan gesture for closing the view controller is handled by external code.
    var panToCloseGestureControlByExternal: Bool = false
    
    /// Controls whether the navigation bar back button is enabled.
    /// 
    /// When true, the standard back button functionality is available in the navigation bar.
    var enableNavgionBarBackButton: Bool = true
    
    /// The configuration parameters for this view controller.
    /// 
    /// This property contains the scheme parameters that define the behavior and appearance
    /// of the hybrid content, including navigation settings and display options.
    public var config: SPKHybridSchemeParam?
    
    /// The main container view that handles hybrid content rendering.
    /// 
    /// This lazy property creates and configures a SPKContainerView with the appropriate
    /// frame, lifecycle delegate, and content mode for displaying hybrid content.
    lazy var viewContainer: SPKContainerView? = {
        let sparkView = SPKContainerView(frame: self.containerFrame)
        sparkView.containerLifecycleDelegate = self
        sparkView.sparkContentMode = .SPKContainerViewContentModeFitSize
        return sparkView
    }()
    
    /// The context object containing configuration and state information.
    /// 
    /// This property holds the hybrid context that provides additional configuration
    /// and runtime information for the view controller and its content.
    public var context: SPKHybridContext?
    
    /// Indicates whether the hybrid content is currently in background state.
    /// 
    /// This property tracks whether the hybrid view is in a background state,
    /// which affects lifecycle management and resource usage.
    public var hybridInBackground: Bool = false
    
    /// Indicates whether the hybrid content has appeared and is visible.
    /// 
    /// This property tracks the visibility state of the hybrid content,
    /// used for proper lifecycle event handling.
    public var hybridAppear: Bool = false
    
    /// The unique identifier for this container.
    /// 
    /// This computed property returns the container ID from the underlying view container,
    /// providing a unique identifier for tracking and management purposes.
    public var containerID: String {
        get {
            return self.viewContainer?.containerID ?? ""
        }
    }
    
    /// Controls whether the view controller should auto-rotate.
    /// 
    /// This property is overridden to always return true, allowing the view controller
    /// to respond to device orientation changes.
    public override var shouldAutorotate: Bool {
        get {
            return true
        }
    }
    
    /// The original URL that was used to load the content.
    /// 
    /// This property stores the initial URL for reference and potential reloading scenarios.
    public var originURL: URL?
    
    /// The type of hybrid engine being used for content rendering.
    /// 
    /// This computed property returns the engine type from the view container,
    /// indicating whether the content is web-based, native, or unknown.
    public var viewType: SPKHybridEngineType {
        get {
            return self.viewContainer?.viewType ?? .SPKHybridEngineTypeUnknown
        }
    }
    
    /// Delegate for handling container lifecycle events.
    /// 
    /// This optional delegate receives notifications about container lifecycle changes
    /// such as loading completion, failures, and state transitions.
    public var containerLifecycleDelegate: (any SPKContainerLifecycleProtocol)?
    
    /// The underlying kit view that handles the actual content rendering.
    /// 
    /// This computed property provides access to the kit view from the container,
    /// which implements the SPKWrapperViewProtocol for content management.
    public var kitView: (any UIView & SPKWrapperViewProtocol)? {
        get {
            return self.viewContainer?.kitView
        }
    }
    
    /// The bottom toolbar displayed at the bottom of the view.
    /// 
    /// This optional toolbar provides additional UI controls and actions
    /// for the hybrid content when enabled.
    public var bottomToolBar: (any SPKBottomToolBarProtocol)?
    
    /// Controls the visibility of the bottom toolbar.
    /// 
    /// When true, the bottom toolbar is hidden from view, providing more
    /// screen space for the main content.
    public var hideBottomToolBar: Bool = false
    
    /// Indicates whether the view has completed its mounting process.
    /// 
    /// This computed property returns the mount state from the view container,
    /// indicating whether the view has been fully initialized and is ready.
    public var didMount: Bool {
        get {
            return self.viewContainer?.didMount ?? false
        }
    }
    
    /// The preferred layout size for the content based on its intrinsic dimensions.
    /// 
    /// This computed property returns the preferred size from the view container,
    /// which is calculated based on the content type and rendering engine.
    public var preferredLayoutSize: CGSize {
        get {
            return self.viewContainer?.preferredLayoutSize ?? .zero
        }
    }
    
    /// The preferred status bar style for this view controller.
    /// 
    /// This property controls the appearance of the status bar (light or dark content)
    /// when this view controller is active.
    public var statusBarStyle: UIStatusBarStyle = .default
    
    /// The current loading state of the kit view.
    /// 
    /// This property tracks the loading progress of the underlying content,
    /// with states including not loaded, loading, loaded, and failed.
    public var loadState: SPKLoadState = .SPKLoadStateNotLoad
    
    /// The custom navigation bar for the hybrid content.
    /// 
    /// This optional property holds a custom navigation bar that conforms to
    /// SPKNavigationBarProtocol for enhanced navigation functionality.
    var navigationBar: (UIView & SPKNavigationBarProtocol)?
    
    /// Indicates whether the container is ready for content loading.
    /// 
    /// This property tracks the initialization state of the container,
    /// ensuring content is only loaded when the container is properly set up.
    var isContainerReady: Bool = false
    
    /// The original status bar style before any modifications.
    /// 
    /// This property stores the original status bar style to restore it
    /// when the view controller is dismissed or deactivated.
    var originStatusBarStyle: UIStatusBarStyle?
    
    /// A background view for the status bar area.
    /// 
    /// This optional view provides a custom background for the status bar area
    /// when needed for design consistency.
    var statusBarBackgroundView: UIView?
    
    var originNavigationBarHidden: Bool = false
    var originNavigationBarIsHidden: Bool = false
    var originControllerPopGestureRecongnizerEnabled: Bool = false
    var statusBarHiddenStatus: Bool = false
    
    var _willDestory: Bool = false
    
    var hasExecuteDidAppearedOnce: Bool = false
    var isInBackground: Bool = false
    
    
    weak var originalNavigationControllerDelegate: UINavigationControllerDelegate?
    weak var oldDelegate: UIGestureRecognizerDelegate?
    
    private var containerFrame: CGRect = UIScreen.main.bounds
    
    /// Used to refresh SnapKit constraints when safe-area insets become valid after the first layout (SwiftUI / multi-scene).
    private var lastAppliedTopSafeAreaHeight: CGFloat = -1
    
    /// Initializes a new SPKViewController with the specified URL and configuration.
    /// 
    /// This is the primary initializer for creating a SPKViewController instance.
    /// It sets up the view controller with the provided URL, configuration parameters,
    /// context, and frame. If no configuration is provided, it will be resolved automatically
    /// based on the URL scheme.
    /// 
    /// - Parameters:
    ///   - url: The URL to load in the hybrid content
    ///   - config: Optional configuration parameters for the hybrid content
    ///   - context: The context object containing additional configuration
    ///   - frame: The frame for the container view (defaults to screen bounds)
    public init(withURL url: URL?, config: SPKSchemeParam? = nil, context: SPKContext?, frame: CGRect = UIScreen.main.bounds) {
        super.init(nibName: nil, bundle: nil)
        let context = context ?? SPKContext()
        let config = config ?? SPKScheme.resolver(withScheme: url, context: context, paramClass: SPKSchemeParam.self) as? SPKSchemeParam
        self.containerFrame = frame
        self.originURL = url
        self.config = config
        self.statusBarStyle = config?.statusFontMode ?? self.statusBarStyle
        self.statusBarHiddenStatus = config?.hideStatusBar ?? self.statusBarHiddenStatus
        self.context = context ?? SPKContext()
        self.containerLifecycleDelegate = context.containerLifecycleDelegate
        self.containerLifecycleDelegate?.containerDidInit?(self)
    }
    
    /// Required initializer for Interface Builder support.
    /// 
    /// This initializer provides basic initialization when the view controller
    /// is created from a storyboard or XIB file.
    /// 
    /// - Parameter coder: The decoder to use for initialization
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    
    /// Called after the view controller's view is loaded into memory.
    /// 
    /// This method performs the complete setup of the view controller including:
    /// - Background color configuration
    /// - View container setup and constraints
    /// - Navigation bar initialization
    /// - Global properties configuration
    /// - Content loading with the origin URL
    /// - Notification observers setup
    /// 
    /// The method also notifies the lifecycle delegate that the view has loaded.
    public override func viewDidLoad() {
        super.viewDidLoad()
        self.setupDefaultBackgroundColor()
        self.setupLoadingBackgroundColor()
        self.updateStatusBarStatus()
        self.setupViewContainer()
        self.setupNavigationBar()
        self.automaticallyAdjustsScrollViewInsets = false
        
        self.load(withURL: self.originURL)
        
        self.setupNotification()
        self.containerLifecycleDelegate?.containerViewDidLoad?(self)
    }
    
    /// Sends an event to the hybrid content with optional parameters and callback.
    /// 
    /// This method provides a communication channel between the native container
    /// and the hybrid content, allowing the native side to trigger events or
    /// request data from the hybrid content.
    /// 
    /// - Parameters:
    ///   - event: The name of the event to send to the hybrid content
    ///   - params: Optional dictionary of parameters to include with the event
    ///   - callback: Optional callback to handle the response from the hybrid content
    public func send(event: String, params: [String : Any]?, callback: ((Any?) -> Void)? = nil) {
        self.viewContainer?.send(event: event, params: params, callback: callback)
    }
    
    /// Sets up notification observers for application lifecycle events.
    /// 
    /// This method registers the view controller to receive notifications when
    /// the application becomes active or resigns active state. This is important
    /// for properly managing the hybrid content lifecycle in response to app
    /// state changes.
    func setupNotification() {
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(handleBecomeActive),
                                               name: UIApplication.didBecomeActiveNotification,
                                               object: nil)

        NotificationCenter.default.addObserver(self,
                                               selector: #selector(handleResignActive),
                                               name: UIApplication.willResignActiveNotification,
                                               object: nil)
    }
    
    /// Called when the view is about to appear.
    /// 
    /// This method performs essential setup before the view becomes visible,
    /// including saving the original navigation bar state, setting up delegates,
    /// and hiding the system navigation bar to use the custom one.
    /// 
    /// - Parameter animated: Whether the appearance is animated
    public override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        let config = self.config as? SPKSchemeParam
        self.originNavigationBarHidden = self.navigationController?.isNavigationBarHidden ?? self.originNavigationBarHidden
        self.originNavigationBarIsHidden = self.navigationController?.navigationBar.isHidden ?? self.originNavigationBarIsHidden
        self.originalNavigationControllerDelegate = self.navigationController?.delegate
        self.navigationController?.delegate = self
        
        self.containerLifecycleDelegate?.containerViewWillAppear?(self)
    }
    
    /// Called when the view has appeared.
    /// 
    /// This method completes the view setup after it becomes visible,
    /// including configuring gesture recognizers, updating status bar style,
    /// and handling the first appearance lifecycle events.
    /// 
    /// - Parameter animated: Whether the appearance was animated
    public override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        self.oldDelegate = self.navigationController?.interactivePopGestureRecognizer?.delegate
        self.navigationController?.interactivePopGestureRecognizer?.delegate = self
        self.originControllerPopGestureRecongnizerEnabled = self.navigationController?.interactivePopGestureRecognizer?.isEnabled ?? self.originControllerPopGestureRecongnizerEnabled
        
        self.originStatusBarStyle = self.statusBarStyle
        self.updateStatusBarStatus()
        self.navigationController?.setNavigationBarHidden(true, animated: animated)
        
        if !self.hasExecuteDidAppearedOnce {
            self.handleViewDidAppear()
            self.handleViewDidDisappear()
        }
        self.hasExecuteDidAppearedOnce = true
        self.containerLifecycleDelegate?.containerViewDidAppear?(self)
    }
    
    /// Called when the view is about to disappear.
    /// 
    /// This method handles the transition coordination for swipe gestures,
    /// restores the original navigation controller state including delegates
    /// and gesture recognizers, and manages the view controller lifecycle.
    /// 
    /// - Parameter animated: Whether the disappearance is animated
    public override func viewWillDisappear(_ animated: Bool) {
        self.transitionCoordinator?.notifyWhenInteractionChanges{ [weak self] context in
            if context.isCancelled {
                return
            }
            self?.send(event: SPKEvent.Back.finishBack, params: [
                SPKEvent.Common.containerIdKey: self?.containerID ?? "",
                SPKEvent.Back.actionFromKey: SPKEvent.Back.actionTypeSwipe
            ], callback: nil)
        }
        
        self.navigationController?.interactivePopGestureRecognizer?.delegate = self.oldDelegate
        self.navigationController?.interactivePopGestureRecognizer?.isEnabled = self.originControllerPopGestureRecongnizerEnabled
        self.navigationController?.delegate = self.originalNavigationControllerDelegate
        super.viewWillDisappear(animated)
        
        if self.isBeingDismissed || self.isMovingFromParent || self.navigationController?.isBeingDismissed == true {
            self._willDestory = true
            self.handleViewDidDisappear()
        }
        self.containerLifecycleDelegate?.containerViewWillDisappear?(self)
    }
    
    /// Called when the view has disappeared.
    /// 
    /// This method completes the view disappearance process by resetting
    /// the status bar and navigation bar styles, managing the destruction
    /// state, and cleaning up the loading view.
    /// 
    /// - Parameter animated: Whether the disappearance was animated
    public override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        self.resetStatusBarStyle()
        self.resetNavigationBarStyle()
        
        if self.navigationController != nil {
            self._willDestory = false
        }
        
        self.containerLifecycleDelegate?.containerViewDidDisappear?(self)
        self.viewContainer?.removeLoadingView()
    }
    
    /// Resets the status bar style to its original state.
    /// 
    /// This method restores the status bar style that was active before
    /// this view controller appeared, and cleans up any custom status bar
    /// background views on iOS 13 and later.
    func resetStatusBarStyle() {
        self.statusBarStyle = self.originStatusBarStyle ?? self.statusBarStyle
        self.setNeedsStatusBarAppearanceUpdate()
        if #available(iOS 13.0, *) {
            self.statusBarBackgroundView?.removeFromSuperview()
            self.statusBarBackgroundView = nil
        }
    }
    
    /// Indicates whether the view controller is in the process of being destroyed.
    /// 
    /// This method returns the current destruction state, which is used to
    /// prevent multiple cleanup operations and ensure proper resource management.
    /// 
    /// - Returns: True if the view controller is being destroyed, false otherwise
    public func willDestory() -> Bool {
        return self._willDestory
    }
    
    /// Resets the navigation bar style to its original state.
    /// 
    /// This method restores the navigation bar visibility and hidden state
    /// that were active before this view controller appeared, ensuring
    /// proper navigation bar behavior when returning to previous controllers.
    func resetNavigationBarStyle() {
        self.navigationController?.setNavigationBarHidden(self.originNavigationBarHidden, animated: true)
        self.navigationController?.navigationBar.isHidden = self.originNavigationBarIsHidden
    }
    
    
    /// Updates the status bar appearance based on the current configuration.
    /// 
    /// This method applies the status bar style from the configuration
    /// and triggers a status bar appearance update to reflect the changes.
    func updateStatusBarStatus() {
        let config = self.config as? SPKSchemeParam
        self.statusBarStyle = config?.statusFontMode ?? self.statusBarStyle
        self.setNeedsStatusBarAppearanceUpdate()
    }

    /// Sets up the default background color for the view controller.
    /// 
    /// This method applies the default container background color from the context
    /// if available, ensuring consistent visual appearance across the application.
    func setupDefaultBackgroundColor() {
        guard let context = self.context as? SPKContext else {
            return
        }
        if let containerBackgroundColor = context.containerBackgroundColor {
            self.view.backgroundColor = containerBackgroundColor
        }
    }
    
    /// Sets up the background color for the loading state.
    /// 
    /// This method applies the loading background color from the configuration
    /// to provide visual feedback during content loading.
    func setupLoadingBackgroundColor() {
        guard let config = self.config as? SPKSchemeParam else {
            return
        }
        self.view.backgroundColor = config.loadingBgColor
    }
    
    /// Sets up the main view container with proper constraints.
    /// 
    /// This method adds the view container to the view hierarchy and configures
    /// its layout constraints to account for navigation bar and status bar offsets.
    func setupViewContainer() {
        guard let viewContainer = self.viewContainer else {
            return
        }
        self.view.addSubview(viewContainer)
        viewContainer.snp.makeConstraints { make in
            make.left.right.bottom.equalTo(self.view)
            make.top.equalTo(self.view).offset(self.topOffset)
        }
    }
    
    /// Sets up the custom navigation bar based on configuration.
    /// 
    /// This method creates and configures the custom navigation bar if not hidden,
    /// applying the appropriate styling and constraints based on the scheme parameters.
    func setupNavigationBar() {
        guard let config = self.config as? SPKSchemeParam else {
            return
        }
        if config.hideNavBar || (config.transStatusBar && !config.showNavBarInTransStatusBar) {
            self.navigationBar?.isHidden = true
            return
        }
        self.navigationBar = self.generateNavigationBar()
        self.navigationBar?.isHidden = false
        
        if let navigationBar = self.navigationBar {
            self.view.addSubview(navigationBar)
            navigationBar.attachToContainer?(config)
            self.navigationBar?.snp.makeConstraints({ make in
                make.height.equalTo(self.navigationBarHeight)
                make.top.leading.trailing.equalTo(self.view)
            })
        }
    }
    
    /// Generates and configures a new navigation bar instance.
    /// 
    /// This method creates a navigation bar with proper button handlers,
    /// title, and styling based on the current configuration and context.
    /// 
    /// - Returns: A configured navigation bar that conforms to SPKNavigationBarProtocol
    func generateNavigationBar() -> (UIView & SPKNavigationBarProtocol)? {
        let context = self.context as? SPKContext
        let navigationBar: (UIView & SPKNavigationBarProtocol) = context?.naviBar ?? SPKNavigationBar.navigationBar()
        navigationBar.container = self
        
        // set left bar buttom item
        var leftButtonItem = SPKNavigationBarButtonItem()
        if let builder = context?.leftNavigationBarButtonItemBuilder,
           let barButtomItem = builder(self) {
            leftButtonItem = barButtomItem
        }
        leftButtonItem.navBarHandler = { [weak self] _ in
            self?.didTapLeftButton()
        }
        navigationBar.setup?(leftButton: leftButtonItem)
        
        // setup right bar buttom item
        if let builder = context?.rightNavigationBarButtonItemBuilder,
            let rightButtomItem = builder(self) {
            navigationBar.setup?(rightButton: rightButtomItem)
        }
        
        let config = self.config as? SPKSchemeParam
        
        if let title = config?.title {
            navigationBar.update(centerTitle: title)
        }
        
        if let navBarColor = config?.navBarColor {
            navigationBar.update?(backgroundColor: navBarColor)
        }
        return navigationBar
    }
    
    /// Handles the left button (back button) tap event.
    /// 
    /// This method is called when the user taps the left button in the navigation bar.
    /// It sends back events to the hybrid content and performs standard navigation
    /// back behavior by popping the view controller or dismissing it.
    func didTapLeftButton() {
        self.send(event: SPKEvent.Back.pageBack, params: [
            SPKEvent.Common.containerIdKey: self.containerID,
            SPKEvent.Back.actionFromKey: SPKEvent.Back.actionTypeNavBarBackPress
        ])
        
        self.send(event: SPKEvent.Back.finishBack, params: [
            SPKEvent.Common.containerIdKey: self.containerID,
            SPKEvent.Back.actionFromKey: SPKEvent.Back.actionTypeNavBarBackPress
        ])
        
        if self.navigationController?.viewControllers.count ?? 0 > 1 {
            //MARK: currently only support lynx
            self.navigationController?.popViewController(animated: true)
        } else {
            self.dismiss(animated: true)
        }
    }
    
    private func updateViewContainerOffset(_ insets: UIEdgeInsets) {
        self.viewContainer?.snp.updateConstraints({ make in
            make.top.equalTo(self.view).offset(insets.top)
            make.left.equalTo(self.view).offset(insets.left)
            make.right.equalTo(self.view).offset(insets.right)
            make.bottom.equalTo(self.view).offset(insets.bottom)
        })
        self.view.setNeedsLayout()
    }
    
    /// Updates navigation bar height and Lynx container top inset when `topSafeAreaHeight` becomes accurate.
    private func refreshSafeAreaDependentLayoutIfNeeded() {
        let top = self.topSafeAreaHeight
        if abs(top - self.lastAppliedTopSafeAreaHeight) < 0.5, self.lastAppliedTopSafeAreaHeight >= 0 {
            return
        }
        self.lastAppliedTopSafeAreaHeight = top
        self.navigationBar?.snp.updateConstraints { make in
            make.height.equalTo(self.navigationBarHeight)
        }
        self.updateViewContainerOffset(UIEdgeInsets(top: self.topOffset, left: 0, bottom: 0, right: 0))
    }

    /// Handles the view did appear event for the hybrid content.
    /// 
    /// This method notifies the view container that the view has appeared
    /// and is now visible to the user, but only if the container is ready.
    public func handleViewDidAppear() {
        if self.isContainerReady {
            self.viewContainer?.handleViewDidAppear()
        }
    }
    
    /// Handles the view did disappear event for the hybrid content.
    /// 
    /// This method notifies the view container that the view has
    /// disappeared and is no longer visible to the user.
    public func handleViewDidDisappear() {
        self.viewContainer?.handleViewDidDisappear()
    }
    
    /// Handles the application becoming active event.
    /// 
    /// This method is called when the application becomes active from background.
    /// It updates the background state and notifies the view container if this
    /// is the top view controller and the container is ready.
    public func handleBecomeActive() {
        self.isInBackground = false
        if Self.topSPKViewController() == self && self.isContainerReady {
            self.viewContainer?.handleViewDidAppear()
        }
        
        self.containerLifecycleDelegate?.containerViewHandleAppDidBecomeActive?(self)
    }
    
    /// Handles the application resigning active event.
    /// 
    /// This method is called when the application is about to resign active state.
    /// It updates the background state and notifies the view container if this
    /// is the top view controller.
    func handleResignActive() {
        self.isInBackground = true
        if Self.topSPKViewController() == self {
            self.viewContainer?.handleViewDidDisappear()
        }
        self.containerLifecycleDelegate?.containerViewHandleAppWillResignActive?(self)
    }
    
    
    public override var prefersStatusBarHidden: Bool {
        return self.statusBarHiddenStatus
    }
    
    public override var preferredStatusBarStyle: UIStatusBarStyle {
        guard let context = self.context as? SPKContext else {
            return self.statusBarStyle
        }
        if self.statusBarStyle == .default && context.containerStatusBarStyle != nil {
            return UIStatusBarStyle(rawValue: context.containerStatusBarStyle?.intValue ?? 0) ?? self.statusBarStyle
        }
        return self.statusBarStyle
    }
    
    /// Sets up the background color for the view controller.
    /// 
    /// This method applies the background color from the configuration if available,
    /// otherwise falls back to the default container background color from context
    /// or white as the final fallback.
    func setupBackgroundColor() {
        if let config = self.config as? SPKSchemeParam {
            self.view.backgroundColor = config.containerBgColor
        } else {
            let context = self.context as? SPKContext
            let bgColor = self.defaultConatinerBackgroundColor() ?? .white
            self.view.backgroundColor = bgColor
        }
    }
    
    /// Returns the default container background color from context.
    /// 
    /// This method retrieves the default background color configured in the
    /// SPKContext, which provides application-wide styling defaults.
    /// 
    /// - Returns: The default background color, or nil if not configured
    func defaultConatinerBackgroundColor() -> UIColor? {
        guard let context = self.context as? SPKContext else {
            return nil
        }
        return context.containerBackgroundColor
    }
    
    
    static func topSPKViewController() -> (UIViewController & SPKContainerProtocol)? {
        let top = SPKResponder.topViewController
        guard let vc = top as? (UIViewController & SPKContainerProtocol) else {
            return nil
        }
        return vc
    }
    
    public func HandleResignActive() {
        if Self.topSPKViewController() == self {
            self.viewContainer?.handleViewDidDisappear()
        }
        
        self.containerLifecycleDelegate?.containerViewHandleAppWillResignActive?(self)
    }
    
    /// Loads the hybrid content using the original URL.
    /// 
    /// This method initiates the loading process for the hybrid content
    /// using the URL that was set during initialization.
    public func load() {
        self.viewContainer?.load()
    }
    
    /// Loads the hybrid content with the specified URL.
    /// 
    /// This method loads the hybrid content using the provided URL and
    /// the current context configuration. It delegates the actual loading
    /// to the view container.
    /// 
    /// - Parameter url: The URL to load in the hybrid content
    func load(withURL url: URL?) {
        guard url != nil else {
            return
        }
        guard let config = self.config else {
            return
        }
        
        self.originURL = self.originURL ?? url
        self.viewContainer?.load(withParams: config, self.context)
    }
    
    /// Reloads the hybrid content with a new context.
    /// 
    /// This method updates the current context and reloads the hybrid content
    /// with the new configuration, including updated global properties.
    /// 
    /// - Parameter context: The new context to use for reloading
    public func reload(_ context: SPKHybridContext?) {
        self.viewContainer?.reload(context)
    }
    
    /// Sets the visibility of the navigation bar.
    /// 
    /// This method controls whether the navigation bar is hidden or visible,
    /// updating both the configuration and the actual navigation bar view.
    /// 
    /// - Parameter hidden: True to hide the navigation bar, false to show it
    public func setNavigationBarHidden(_ hidden: Bool) {
        let config = self.config as? SPKSchemeParam
        config?.hideNavBar = hidden
        self.navigationBar?.isHidden = hidden
        self.updateViewContainerOffset(UIEdgeInsets(top: self.topOffset, left: 0.0, bottom: 0.0, right: 0.0))
    }
    
    /// Enables or disables the navigation bar back button.
    /// 
    /// This method controls whether the back button in the navigation bar
    /// is enabled and responds to user interaction.
    /// 
    /// - Parameter enable: True to enable the back button, false to disable it
    public func setNavigationBarBackButton(_ enable: Bool) {
        self.navigationBar?.set(navigationBarBackButtonEnable: enable)
    }
    
    /// Updates the title displayed in the navigation bar.
    /// 
    /// This method updates both the configuration and the navigation bar
    /// to display the new title text.
    /// 
    /// - Parameter title: The new title to display
    public func update(title: String) {
        if title != nil {
            let config = self.config as? SPKSchemeParam
            config?.title = title
            self.navigationBar?.update(centerTitle: title)
        }
    }
    
    /// Updates the color of the title text in the navigation bar.
    /// 
    /// This method changes the color of the navigation bar title using
    /// a hex color string.
    /// 
    /// - Parameter titleColor: The hex color string for the title color
    public func update(titleColor: String) {
        guard !isEmptyString(titleColor) else {
            return
        }
        let color = UIColor.spk.color(hexString: titleColor)
        self.navigationBar?.update(titleColor: color)
    }
    
    /// Updates the background color of the navigation bar.
    /// 
    /// This method changes the background color of the navigation bar
    /// using a hex color string.
    /// 
    /// - Parameter navigationBarColor: The hex color string for the navigation bar background
    public func update(navigationBarColor: String) {
        guard !isEmptyString(navigationBarColor) else {
            return
        }
        let color = UIColor.spk.color(hexString: navigationBarColor)
        self.navigationBar?.update?(backgroundColor: color)
    }
    
    /// Updates the status bar style.
    /// 
    /// This method changes the status bar appearance (light or dark content)
    /// and updates the configuration accordingly.
    /// 
    /// - Parameter statusBarStyle: The new status bar style to apply
    public func update(statusBarStyle: UIStatusBarStyle) {
        guard let config = self.config as? SPKSchemeParam else {
            return
        }
        config.statusFontMode = statusBarStyle
        self.updateStatusBarStatus()
    }
}

extension SPKViewController: SPKContainerLifecycleProtocol {
    public func container(_ container: any SPKContainerProtocol, didFinishLoadWithURL url: URL?) {
        self.setupBackgroundColor()
        if !self.hybridInBackground && self.hasExecuteDidAppearedOnce {
            self.viewContainer?.handleViewDidAppear()
        }
    }
    
    public func container(_ container: any SPKContainerProtocol, didLoadFailedWithURL url: URL?, error: (any Error)?) {
        if self.viewContainer?.shouldShowLoadFailedView(with: error) == true {
            self.addLoadFailedView(error)
        }
        self.forceShowNavigationBar()
    }
    
    public func container(_ container: any SPKContainerProtocol, updateTitle title: String) {
        if !isEmptyString(title) {
            self.navigationBar?.update(centerTitle: title)
        }
    }
    
    public func addLoadFailedView(_ error: Error?) {
        guard let config = self.config as? SPKSchemeParam,
            config.showError else {
            return
        }
        
        let view = self.buildLoadErrorView()
        
        view?.container?(self, didReceiveError: error)
        
        self.updateViewContainerOffset(UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0))
        view?.frame = self.view.bounds
        self.viewContainer?.add(loadFailedView: view)
    }
    
    func buildLoadErrorView() -> (UIView & SPKLoadErrorViewProtocol)? {
        guard let context = self.context as? SPKContext else {
            return nil
        }
        
        let loadFailedView = context.loadFailedView ?? context.failedViewBuilder?(self)
        
        loadFailedView?.register?(refreshBlock: { [weak self] in
            self?.handleErrorViewReload()
        })
        
        return nil
    }
    
    func handleErrorViewReload() {
        self.setupNavigationBar()
        self.updateViewContainerOffset(UIEdgeInsets(top: self.topOffset, left: 0, bottom: 0, right: 0))
        if let context = self.context {
            self.reload(context)
        }
    }
    
    func forceShowNavigationBar() {
        if let navigationBar = self.navigationBar {
            self.navigationBar?.isHidden = false
            self.view.bringSubviewToFront(navigationBar)
            return
        }
        self.navigationBar = self.generateNavigationBar()
        
        if let navigationBar = self.navigationBar {
            self.view.addSubview(navigationBar)
            self.navigationBar?.container = self
            if let config = self.config as? SPKSchemeParam {
                self.navigationBar?.attachToContainer?(config)
            }
            self.navigationBar?.snp.makeConstraints({ make in
                make.height.equalTo(self.navigationBarHeight)
                make.top.leading.right.equalTo(self.view)
            })
        }
    }
    
    open override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        self.refreshSafeAreaDependentLayoutIfNeeded()
    }
    
    open override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        self.refreshSafeAreaDependentLayoutIfNeeded()
    }
    
    open override func traitCollectionDidChange(_ previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if #available(iOS 13.0, *) {
            if self.traitCollection.hasDifferentColorAppearance(comparedTo: previousTraitCollection) {
                if self.traitCollection.userInterfaceStyle == .dark {
                    self.send(event: SPKEvent.Theme.changed, params: [
                        SPKEvent.Theme.valueKey: SPKEvent.Theme.valueDark
                    ])
                } else if self.traitCollection.userInterfaceStyle == .light {
                    self.send(event: SPKEvent.Theme.changed, params: [
                        SPKEvent.Theme.valueKey: SPKEvent.Theme.valueLight
                    ])
                }
            }
        }
    }
}

extension SPKViewController: UINavigationControllerDelegate {
    
}

extension SPKViewController: UIGestureRecognizerDelegate {

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        if !self.panToCloseGestureControlByExternal {
            self.reportPangestureEvent()
        }
        return true
    }
    
    func reportPangestureEvent() {
        self.send(event: SPKEvent.Back.pageBack, params: [
            SPKEvent.Common.containerIdKey: self.containerID,
            SPKEvent.Back.actionFromKey: SPKEvent.Back.actionTypeSwipe
        ])
    }
}
