// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import UIKit
import SnapKit
import SparklingMethod

/// A hybrid view container that manages different types of SPK content.
/// 
/// SPKContainerView serves as the main container for hybrid content, supporting both web-based
/// and native Lynx engine types. It handles view lifecycle, loading states, error handling,
/// and provides a unified interface for different rendering engines. The class implements
/// SPKContainerViewContainerProtocol and provides comprehensive lifecycle management.
/// The @objcMembers attribute ensures Objective-C compatibility for all properties and methods.

@objc
public enum SPKContainerViewContentMode: Int {
    case SPKContainerViewContentModeFixedSize = 1
    case SPKContainerViewContentModeFixedWidth
    case SPKContainerViewContentModeFixedHeight
    case SPKContainerViewContentModeFitSize
}

@objcMembers
open class SPKContainerView: UIView, SPKContainerProtocol {
    
    /// The content mode that determines how the view handles sizing and layout.
    /// 
    /// Defaults to fixed size mode for consistent layout behavior.
    public var sparkContentMode: SPKContainerViewContentMode = .SPKContainerViewContentModeFixedSize
        
    /// The hybrid context containing configuration and state information.
    /// 
    /// This context provides the necessary configuration for the hybrid view's operation.
    public var context: SPKHybridContext?
    
    /// Indicates whether the hybrid view is currently in background state.
    /// 
    /// This property tracks the background/foreground state for lifecycle management.
    public var hybridInBackground: Bool = false
    
    /// Indicates whether the hybrid view has appeared and is visible.
    /// 
    /// This property tracks the view's appearance state for proper lifecycle handling.
    public var hybridAppear: Bool = false
    
    /// The unique identifier for this container.
    /// 
    /// This read-only property returns the container ID from the underlying kit view,
    /// or an empty string if no kit view is present.
    public var containerID: String {
        return self.kitView?.containerID ?? ""
    }
    
    /// The original URL that was used to load this view.
    /// 
    /// This read-only property provides access to the original URL from the configuration.
    public var originURL: URL? {
        return self.config?.originURL
    }
    
    /// The type of hybrid engine being used for rendering.
    /// 
    /// This read-only property indicates whether the view uses web, Lynx, or unknown engine type.
    public var viewType: SPKHybridEngineType {
        return self.config?.engineType ?? .SPKHybridEngineTypeUnknown
    }
    
    /// Delegate for handling container lifecycle events.
    /// 
    /// This optional delegate receives notifications about container lifecycle changes.
    public weak var containerLifecycleDelegate: (any SPKContainerLifecycleProtocol)?
    
    /// The underlying kit view that handles the actual content rendering.
    /// 
    /// This view conforms to SPKWrapperViewProtocol and provides the core rendering functionality.
    public var kitView: (UIView & SPKWrapperViewProtocol)?
    
    /// The bottom toolbar displayed at the bottom of the view.
    /// 
    /// This optional toolbar provides additional UI controls for the hybrid view.
    public var bottomToolBar: (any SPKBottomToolBarProtocol)?
    
    private var isLoading: Bool {
        return self.kitView?.loadState == .SPKLoadStateLoading
    }

    private var isLoadFailed: Bool {
        return self.kitView?.loadState == .SPKLoadStateFailed
    }
    
    /// Controls the visibility of the bottom toolbar.
    /// 
    /// When true, the bottom toolbar is hidden from view.
    public var hideBottomToolBar: Bool = false
    
    /// Indicates whether the view has completed its mounting process.
    /// 
    /// This property tracks whether the view has been fully initialized and mounted.
    public var didMount: Bool = false
    
    /// The preferred layout size for the view based on its content and engine type.
    /// 
    /// This computed property returns different sizes depending on the engine type:
    /// - For web engine: Returns the kit view's frame size
    /// - For Lynx engine: Triggers layout and returns intrinsic content size
    /// - For other types: Returns zero size
    public var preferredLayoutSize: CGSize {
        var size: CGSize = .zero
        if self.viewType == .SPKHybridEngineTypeWeb {
            size = self.kitView?.frame.size ?? size
        } else if self.viewType == .SPKHybridEngineTypeLynx {
            self.kitView?.triggerLayout?()
            size = self.kitView?.intrinsicContentSize ?? size
        }
        return size
    }
    
    /// The preferred status bar style for this view.
    /// 
    /// This property controls the appearance of the status bar when this view is active.
    public var statusBarStyle: UIStatusBarStyle = .default
    
    /// The configuration parameters for this view.
    /// 
    /// This optional property contains the scheme parameters used to configure the view.
    public var config: SPKSchemeParam?
    
    /// The content size of the view.
    /// 
    /// This property represents the actual content dimensions, which may differ from the view's frame.
    public var contentSize: CGSize = .zero
    
    /// The view displayed when loading fails.
    /// 
    /// This private property holds the error view shown when content loading encounters an error.
    private var loadFailedView: (UIView & SPKLoadErrorViewProtocol)?
    
    /// The view displayed during content loading.
    /// 
    /// This private property holds the loading indicator view shown while content is being loaded.
    private var loadingView: (UIView & SPKLoadingViewProtocol)?
    
    /// The URL currently being loaded or that was last loaded.
    /// 
    /// This private property tracks the current or most recent URL for the view's content.
    private var url: URL?
    
    private var initStartTimeStamp: Double = 0
    
    private var initEndTimeStamp: Double = 0
    
    private var beginLoadTimeStamp: Double = 0
    
    private var isFirstDraw: Bool = false
    
    private var isFirstLoad: Bool = false
    
    /// The most recent loading error, if any.
    /// 
    /// This private property stores any error that occurred during the last loading attempt.
    private var loadError: Error?
    
    /// The current loading state of the view.
    ///
    /// This property tracks the view's loading progress and notifies the lifecycle delegate
    /// whenever the state changes. Possible states include not loaded, loading, loaded, and failed.
    public var loadState: SPKLoadState = .SPKLoadStateNotLoad
    
    private var hasLoadedDict: [String: Bool] = [:]
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        self.isFirstDraw = true
        self.isFirstLoad = true
        self.initStartTimeStamp = Date(timeIntervalSinceNow: 0).timeIntervalSince1970 * 1000
        self.hasLoadedDict = [:]
        self.initEndTimeStamp = Date(timeIntervalSinceNow: 0).timeIntervalSince1970 * 1000
    }
    
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
        
    deinit {
        let current = Date().timeIntervalSince1970 * 1000
        let stayDuration = current - self.initStartTimeStamp
        var metric: [String: AnyHashable] = [:]
        metric["stay_duration"] = stayDuration
        var status = "success"
        if self.isLoading {
            status = "cancel"
        } else if self.isLoadFailed {
            status = "failure"
        }
        
        self.track(event: .containerExit, metrics: metric, category: [
            "status": status
        ])
        
    }
    /// Loads content from the specified URL string.
    /// 
    /// This convenience method converts a URL string to a URL object and delegates to the main load method.
    /// It resolves the URL scheme and creates the necessary parameters for loading.
    /// 
    /// - Parameters:
    ///   - url: The URL string to load content from
    ///   - context: Optional context containing additional configuration
    ///   - forceInitKitView: Whether to force initialization of a new kit view
    public func load(withURL url: String, _ context: SPKHybridContext?, _ forceInitKitView: Bool = true) {
        let context = context as? SPKContext ?? SPKContext()
        context.originURL = url
        
        guard let param = SPKScheme.resolver(withScheme: URL.spk.url(string: url), context: context) else {
            return
        }
        self.load(withParams: param, context, forceInitKitView: forceInitKitView)
    }
    
    /// Loads content using the provided scheme parameters.
    /// 
    /// This method configures the view with the provided parameters and initiates the loading process.
    /// It sets up the context, updates global properties, and triggers the engine loading on the main queue.
    /// 
    /// - Parameters:
    ///   - params: The scheme parameters containing URL and configuration
    ///   - context: Optional context containing additional configuration
    ///   - forceInitKitView: Whether to force initialization of a new kit view
    public func load(withParams params: SPKHybridSchemeParam, _ context: SPKHybridContext?, forceInitKitView: Bool = true) {
        self.loadingView?.update?(loadingProgress: 0.05)
        let context = context as? SPKContext ?? SPKContext()
        
        self.config = params as? SPKSchemeParam
        self.context = context
        self.context?.originURL = self.originURL?.absoluteString
        
        self.addContainerDefaultGlobalProps()
        self.containerLifecycleDelegate = context.containerLifecycleDelegate
        
        self.beginLoadTimeStamp = Date(timeIntervalSinceNow: 0).timeIntervalSince1970 * 1000
        
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.loadEngine(forceInitKitView)
        }
        
        self.track(event: .containerLoadURL, metrics: nil,  category: [
            "status": "start",
            "engine_type": self.engineTypeString()
        ])
    }
    
    func engineTypeString() -> String {
        switch self.config?.engineType {
        case .SPKHybridEngineTypeUnknown:
            return "unknown"
        case .SPKHybridEngineTypeWeb:
            return "web"
        case .SPKHybridEngineTypeLynx:
            return "lynx"
        case nil:
            return "unknown"
        }
        return "unknown"
    }
    
    
    /// Adds default global properties to the container.
    /// 
    /// This method updates the global properties with essential container information
    /// including the SPK framework version for proper initialization and tracking.
    public func addContainerDefaultGlobalProps() {
        SPKKitUtils.updateGlobalProps(withContext: self.context, newGlobalProps: [
            "SPK_version": SPKVersion.SPKVersion()
        ])
        return
    }
    
    /// Lays out the view's subviews.
    /// 
    /// This method ensures the kit view is properly positioned within the container's bounds
    /// and triggers layout updates when the frame changes to maintain proper rendering.
    public override func layoutSubviews() {
        super.layoutSubviews()
        guard let kitView = self.kitView else {
            return
        }
        
        if CGRectEqualToRect(kitView.frame, self.bounds) {
            return
        }
        self.kitView?.triggerLayout?()
    }
        
    /// Loads the hybrid engine with the configured parameters.
    /// 
    /// This method handles the engine loading process, including URL resolution for hybrid schemes,
    /// kit view creation or reuse, and initialization of the loading state. It supports both
    /// forced initialization and reloading of existing kit views.
    /// 
    /// - Parameter forceInitKitView: Whether to force creation of a new kit view
    func loadEngine(_ forceInitKitView: Bool = true) {
        if let kitView = self.kitView,
           !forceInitKitView {
            kitView.reload(self.context)
            return
        }
        self.context?.schemeParams = self.config
                
        if let kitView = SPKKit.createKitView(withParams: self.config, context: self.context, frame: self.bounds) {
            self.setupKitView(kitView)
            kitView.lifeCycleDelegate = self
            kitView.lifeCycleDelegate?.viewDidCreate?(kitView)
            self.loadState = .SPKLoadStateLoading
            kitView.load()
        }
    }
    
    /// Sets up and configures the kit view for the container.
    /// 
    /// This method establishes the relationship between the container and kit view,
    /// and attaches the kit view to the container's view hierarchy.
    /// 
    /// - Parameter kitView: The kit view to set up and configure
    func setupKitView(_ kitView: (UIView & SPKWrapperViewProtocol)) {
        self.attachKitView(kitView)
    }
    
    /// Attaches the kit view to the container's view hierarchy.
    /// 
    /// This method safely replaces any existing kit view with the new one,
    /// ensuring proper cleanup of the previous view and correct setup of the new view.
    /// 
    /// - Parameter kitView: The kit view to attach to the container
    func attachKitView(_ kitView: (UIView & SPKWrapperViewProtocol)) {
        if self.kitView === kitView {
            return
        }
        self.kitView?.removeFromSuperview()
        self.kitView = kitView
        self.addSubview(kitView)
        self.kitView?.snp.makeConstraints({ make in
            make.edges.equalTo(self)
        })
    }

    public func add(loadingView loadingView: (any UIView & SPKLoadingViewProtocol)?) {
        self.loadingView?.removeFromSuperview()
        
        if let loadingView = loadingView {
            self.loadingView = loadingView
            self.addSubview(loadingView)
            loadingView.mask?.snp.makeConstraints({ make in
                make.edges.equalTo(self)
            })
            loadingView.layoutIfNeeded()
            loadingView.startLoadingAnimation?()
        }
    }
    
    /// Removes the loading view from the container.
    ///
    /// This method safely removes the current loading view from the view hierarchy,
    /// stops any ongoing loading animations, and ensures proper cleanup.
    public func removeLoadingView() {
        if let _ = self.loadingView?.superview {
            self.loadingView?.stopLoadingAnimation?()
            self.loadingView?.removeFromSuperview()
        }
    }
    
    /// Determines whether to show the load failed view for the given error.
    /// 
    /// This method checks the configuration to determine if error views should be displayed.
    /// It returns the showError flag from the configuration, defaulting to false if not set.
    /// 
    /// - Parameter error: The error that occurred during loading
    /// - Returns: True if the load failed view should be shown based on configuration
    public func shouldShowLoadFailedView(with error: (any Error)?) -> Bool {
        return self.config?.showError ?? false
    }
    
    /// Adds a load failed view to the container.
    /// 
    /// This method adds an error view to display when content loading fails.
    /// The error view is added as a subview and stored for future reference.
    /// 
    /// - Parameter view: The error view to add to the container, or nil to remove
    public func add(loadFailedView loadFailedView: (any UIView & SPKLoadErrorViewProtocol)?) {
        if let loadFailedView = loadFailedView {
            self.loadFailedView = loadFailedView
            self.addSubview(loadFailedView)
        }
    }
    
    //MARK: - SPKContainerProtocol
    /// Handles the view did appear lifecycle event.
    /// 
    /// This method is called when the view appears and notifies the kit view
    /// that it should show its content and resume any paused operations.
    public func handleViewDidAppear() {
        self.kitView?.onshow(params: [
            "event": SPKKitEvent.viewDidAppear
        ])
    }
    
    /// Handles the view did disappear lifecycle event.
    /// 
    /// This method forwards the disappear event to the typed version with unknown type,
    /// ensuring proper cleanup when the view is no longer visible.
    public func handleViewDidDisappear() {
        self.handleViewDidDisappear(withType: .SPKDisappearTypeUnknown)
    }
    
    /// Handles the view did disappear lifecycle event with a specific disappear type.
    /// 
    /// This method notifies the kit view that it should hide its content and handles
    /// different types of disappear events with appropriate cleanup actions.
    /// 
    /// - Parameter type: The type of disappear event (app resign active, covered, destroy, etc.)
    public func handleViewDidDisappear(withType type: SPKDisappearType) {
        self.kitView?.onHide(params: [
            "event": SPKKitEvent.viewDidDisappear
        ])
        guard type != .SPKDisappearTypeUnknown else {
            return
        }
        var disappearTypeString = ""
        switch type {
        case .SPKDisappearTypeAppResignActive:
            disappearTypeString = "appResignActive"
        case .SPKDisappearTypeCovered:
            disappearTypeString = "covered"
        case .SPKDisappearTypeDestroy:
            disappearTypeString = "destory"
        default:
            break
        }
        self.kitView?.send(event: "viewDisappearedWithType", params: [
            "type": disappearTypeString
        ], callback: nil)
        
        if type == .SPKDisappearTypeDestroy {
            self.kitView?.onVCWillDestory?()
        }
    }
    
    /// Handles the application becoming active lifecycle event.
    /// 
    /// This method notifies the kit view to show its content when the application
    /// transitions from inactive to active state, resuming any paused operations.
    public func handleBecomeActive() {
        self.kitView?.onshow(params: [
            "event": SPKKitEvent.viewDidAppear
        ])
    }
    
    /// Handles the application resigning active lifecycle event.
    /// 
    /// This method notifies the kit view to hide its content when the application
    /// becomes inactive, allowing for proper state management and resource cleanup.
    public func HandleResignActive() {
        self.kitView?.onHide(params: [
            "event": SPKKitEvent.viewDidDisappear
        ])
    }
    
    //MARK: -
    
    /// Initiates the loading process for the view.
    /// 
    /// This method starts the engine loading process using the current configuration.
    /// It's typically called after the view has been properly configured with a URL and context.
    public func load() {
        self.addLoadingViewIfNeeded()
        self.kitView?.load()
    }
    
    /// Adds a loading view if needed based on configuration.
    /// 
    /// This method checks the configuration to determine if a loading view should be shown,
    /// builds the appropriate loading view, and adds it to the container.
    func addLoadingViewIfNeeded() {
        if self.config?.showLoading == true,
           let loadingView = self.buildLoadingView() {
            self.add(loadingView: loadingView)
        }
    }
    
    /// Builds a loading view from the current context.
    /// 
    /// This method creates a loading view using either the pre-configured loading view
    /// or the loading view builder from the context.
    /// 
    /// - Returns: A loading view that conforms to SPKLoadingViewProtocol, or nil if unavailable
    func buildLoadingView() -> (UIView & SPKLoadingViewProtocol)? {
        guard let context = self.context as? SPKContext else {
            return nil
        }
        var view = context.loadingView ?? context.loadingViewBuilder?()
        return view
    }
    
    /// Reloads the current content in the view.
    /// 
    /// This method triggers a reload of the kit view's content with the provided context,
    /// useful for refreshing data or recovering from errors without recreating the entire view.
    /// 
    /// - Parameter context: The hybrid context to use for reloading
    public func reload(_ context: SPKHybridContext?) {
        self.addLoadingViewIfNeeded()
        
        self.containerLifecycleDelegate?.containerWillReload?(self)
        if let _ = self.loadFailedView {
            self.loadFailedView?.removeFromSuperview()
            self.loadFailedView = nil
        }
        self.kitView?.reload(self.context)
    }
    
    /// Sends an event to the kit view with optional parameters and callback.
    /// 
    /// This method provides a communication channel to send custom events to the kit view,
    /// enabling interaction between the container and the loaded content.
    /// 
    /// - Parameters:
    ///   - event: The name of the event to send
    ///   - params: Optional parameters to include with the event
    ///   - callback: Optional callback to handle the response
    public func send(event: String, params: [String : Any]?, callback: ((Any?) -> Void)?) {
        self.kitView?.send(event: event, params: params, callback: nil)
    }
    
    /// Configures the view with global properties.
    /// 
    /// This method applies global configuration properties to the kit view,
    /// allowing for runtime customization of behavior and appearance.
    /// 
    /// - Parameter globalProps: The global properties to apply to the view
    public func config(withGlobalProps globalProps: Any) {
        self.kitView?.config(withGlobalProps: globalProps)
    }
    
    /// Updates the view with new global properties.
    /// 
    /// This method updates the kit view's configuration with new global properties,
    /// allowing for dynamic changes to the view's behavior during runtime.
    /// 
    /// - Parameter globalProps: The updated global properties to apply
    public func update(withGlobalProps globalProps: Any) {
        self.kitView?.update(withGlobalProps: globalProps)
    }
    
    private func track(event: SPKTrackerEventKey?, metrics: [String: AnyHashable]? = nil, category: [String: AnyHashable]? = nil) {
        var categoryNew = category
        categoryNew?.updateValue(self.originURL?.absoluteString, forKey: "url")
        SPKKit.trackerService?.track(event: event, metrics: metrics, category: category, containerView: self.kitView?.rawView)
    }
    
}


/// Extension implementing the SPKWrapperViewLifecycleProtocol protocol.
///
/// This extension provides lifecycle event handling for the SPKContainerView,
/// forwarding events from the kit view to the container lifecycle delegate.
extension SPKContainerView: SPKWrapperViewLifecycleProtocol {
    //MARK: - KitViewLifeCycle Delegate
    
    public func view(_ view: (any SPKWrapperViewProtocol)?, didStartFetchResourceWithURL url: URL?) {
        self.containerLifecycleDelegate?.container?(self, didStartFetchResourceWithURL: url)
    }
    
    public func view(_ view: (any SPKWrapperViewProtocol)?, didFetchedResource resource: (any SPKResourceProtocol)?, error: (any Error)?) {
        self.containerLifecycleDelegate?.container?(self, didFetchedResource: resource, error: error)
    }
    
    public func viewDidFirstScreen(_ view: (any SPKWrapperViewProtocol)?) {
        self.containerLifecycleDelegate?.containerDidFirstScreen?(self)
    }
    
    public func viewDidUpdate(_ view: (any SPKWrapperViewProtocol)?) {
        self.containerLifecycleDelegate?.containerDidUpdate?(self)
    }
    
    public func viewDidPageUpdate(_ view: (any SPKWrapperViewProtocol)?) {
        self.containerLifecycleDelegate?.containerDidPageUpdate?(self)
    }
    
    public func view(_ view: (any SPKWrapperViewProtocol)?, didReceiveError error: (any Error)?) {
        self.containerLifecycleDelegate?.container?(self, didRecieveError: error)
    }
    
    public func view(_ view: (any SPKWrapperViewProtocol)?, didReceivePerformance perfDict: Dictionary<AnyHashable, Any>?) {
        self.containerLifecycleDelegate?.container?(self, didReceivePerformance: perfDict)
    }
    
    public func viewBeforeLoading(_ view: (any SPKWrapperViewProtocol)?) {
        self.containerLifecycleDelegate?.containerBeforeLoading?(self)
    }
    
    public func viewWillStartLoading(_ view: (any SPKWrapperViewProtocol)?) {
        self.addLoadingViewIfNeeded()
        self.containerLifecycleDelegate?.containerWillStartLoading?(self)
    }
    
    public func viewDidStartLoading(_ view: (any SPKWrapperViewProtocol)?) {
        self.containerLifecycleDelegate?.containerDidStartLoading?(self)
    }

    public func viewDidConstructJSRuntime(_ view: (any SPKWrapperViewProtocol)?) {
        self.containerLifecycleDelegate?.containerDidConstructJSRuntime?(self)
    }

    public func view(_ view: (any SPKWrapperViewProtocol)?, updateTitle title: String) {
        self.containerLifecycleDelegate?.container?(self, updateTitle: title)
    }

    public func view(_ view: (any SPKWrapperViewProtocol)?, didLoadFailedWithURL url: URL?, error: (any Error)?) {
        self.loadError = error
        self.removeLoadingView()
        
        self.track(event: .viewLoadFailed, metrics: nil, category: nil)
        if let url = url,
           !self.hasLoadedDict.spk.bool(forKey: url.absoluteString) {
            self.track(event: .containerLoadURL, metrics: nil, category: [
                "status": "failure",
                "has_error_viwe": self.loadFailedView != nil ? "true": "false",
                "fail_reason": error?.localizedDescription ?? "",
                "engine_type": self.engineTypeString()
            ])
            self.hasLoadedDict.updateValue(true, forKey: url.absoluteString)
        }
        self.containerLifecycleDelegate?.container?(self, didLoadFailedWithURL: url, error: error)
    }
    
    public func view(_ view: (any SPKWrapperViewProtocol)?, didFinishLoadWithURL url: URL?) {
        self.loadError = nil
        self.didMount = true
        
        self.loadingView?.update?(loadingProgress: 1.0)
        self.removeLoadingView()
        
        let current = Date(timeIntervalSinceNow: 0).timeIntervalSince1970 * 1000
        var metric: [String: AnyHashable] = [:]
        metric["init_to_render_duration"] = current - self.initEndTimeStamp
        metric["load_to_render_duration"] = current - self.beginLoadTimeStamp
        
        self.track(event: .viewLoadSuccess, metrics: metric, category: [
            "is_first_draw": self.isFirstDraw ? "1" : "0"
        ])
        self.isFirstDraw = false
        
        if let url = url,
           !self.hasLoadedDict.spk.bool(forKey: url.absoluteString) {
            self.track(event: .containerLoadURL, metrics: nil, category: [
                "status": "success",
                "has_error_view": false,
                "fail_reason": "",
                "engine_type": self.engineTypeString()
            ])
            self.hasLoadedDict.updateValue(true, forKey: url.absoluteString)
        }
        
        self.containerLifecycleDelegate?.container?(self, didFinishLoadWithURL: url)
    }
    
    public func view(_: any SPKWrapperViewProtocol, didChangeIntrinsicContentSize size: CGSize) {
        let width = size.width > 0 ? size.width : self.frame.size.width
        let height = size.height > 0 ? size.height : self.frame.size.height
        let frame = CGRect(x: self.frame.origin.x, y: self.frame.origin.y, width: width, height: height)
        
        switch self.sparkContentMode {
        case .SPKContainerViewContentModeFitSize:
            self.frame = frame
        case .SPKContainerViewContentModeFixedWidth:
            self.frame = CGRect(x: frame.origin.x, y: frame.origin.y, width: self.frame.size.width, height: frame.size.height)
        case .SPKContainerViewContentModeFixedHeight:
            self.frame = CGRect(x: frame.origin.x, y: frame.origin.y, width: frame.size.width, height: self.frame.size.height)
        default:
            self.frame = frame
        }
        
        if self.sparkContentMode != .SPKContainerViewContentModeFixedSize {
            self.contentSize = size
            self.invalidateIntrinsicContentSize()
            self.setNeedsLayout()
        }
        
        self.containerLifecycleDelegate?.container?(self, didChangeIntrinsicContentSize: size)
    }
}
