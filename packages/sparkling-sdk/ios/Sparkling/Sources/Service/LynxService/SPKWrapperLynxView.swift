// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx
import SparklingMethod

/// Lynx-based view implementation for rendering hybrid content.
/// 
/// This class extends LynxView to provide SPK framework integration,
/// handling template loading, resource management, method pipe communication,
/// and lifecycle events. It serves as the primary view component for
/// Lynx-rendered content within the SPK ecosystem.
@objcMembers
open class SPKWrapperLynxView: LynxView, SPKWrapperLynxViewProtocol {
    
    /// The hybrid context containing configuration and global properties.
    /// 
    /// This computed property provides access to the context from the
    /// associated parameters, containing global props, providers, and
    /// other configuration data needed for rendering.
    public var context: SPKHybridContext? {
        set {
            self.context = newValue
        }
        get {
            return self.params?.context
        }
    }
    
    /// Reference to the underlying Lynx view instance.
    /// 
    /// This property maintains a reference to the Lynx view for
    /// direct access to Lynx-specific functionality when needed.
    public var lynxView: LynxView?
    
    /// Flag indicating whether the Lynx view has been successfully created.
    /// 
    /// This boolean tracks the creation state of the Lynx view to
    /// prevent duplicate initialization and handle lifecycle properly.
    public var isLynxCreated: Bool = false
    
    /// Optional raw UIView for custom view building scenarios.
    /// 
    /// This property can hold a custom view created through the
    /// rawViewBuilderBlock for specialized rendering requirements.
    public var rawView: UIView? {
        return self
    }
    
    /// Parameters containing configuration data for the Lynx view.
    /// 
    /// These parameters include source URLs, global properties,
    /// providers, and other configuration needed for view setup.
    public var params: (any SPKHybridParams)?
    
    /// Method pipe instance for JavaScript-native communication.
    /// 
    /// This property holds the method pipe that enables bidirectional
    /// communication between JavaScript and native code.
    public var anyMethodPipe: Any?
    
    /// Delegate for receiving view lifecycle events.
    /// 
    /// This delegate receives callbacks for important lifecycle events
    /// such as loading start, completion, and errors.
    public weak var lifeCycleDelegate: (any SPKWrapperViewLifecycleProtocol)?
    
    /// Current loading state of the view.
    /// 
    /// This property tracks the loading progress through various states
    /// from not loaded to fully loaded or error states.
    public var loadState: SPKLoadState = .SPKLoadStateNotLoad
    
    /// Estimated loading progress as a percentage (0.0 to 1.0).
    /// 
    /// This float value represents the current loading progress,
    /// useful for displaying progress indicators to users.
    public var estimatedProgress: Float = 0
    
    /// Global properties data for template rendering.
    /// 
    /// This private property holds the LynxTemplateData containing
    /// global properties that are passed to the Lynx template.
    private var globalProps: LynxTemplateData?

    /// Shared global resource provider for all Lynx views.
    /// 
    /// This static provider handles resource loading operations
    /// that are shared across multiple view instances.
    static private var globalResourceProvider = SPKLynxResourceProvider ()

    /// Instance-specific resource provider with lazy initialization.
    /// 
    /// This provider is configured with view-specific parameters
    /// and handles resource loading for this particular view instance.
    lazy private var internalResourceProvider: SPKLynxResourceProvider = {
        let resourceLoader = SPKLynxResourceProvider()
        resourceLoader.templateSourceURL = (self.params as? SPKLynxKitParams)?.sourceUrl
        resourceLoader.context = self.params?.context
        resourceLoader.delegate = self
        return resourceLoader
    }()
    
    /// Configuration object for the Lynx rendering engine.
    /// 
    /// This property holds the LynxConfig that defines how the
    /// Lynx view should be configured and what modules to register.
    public var lynxConfig: LynxConfig?
    
    /// Initializes a new SPKWrapperLynxView with the specified frame and parameters.
    ///
    /// This initializer sets up the complete Lynx rendering environment including:
    /// - Configuration of the Lynx engine with resource providers
    /// - Registration of required modules (NavigationModule, custom modules)
    /// - Setup of method pipes for communication
    /// - Configuration of resource fetchers and providers
    /// - Thread strategy setup for rendering
    ///
    /// - Parameters:
    ///   - frame: The frame rectangle for the view
    ///   - params: Configuration parameters containing context, modules, and source URL
    public required init(withFrame frame: CGRect, params: SPKLynxKitParams?) {
        var lynxConfig: LynxConfig? = nil
        let containerID = UUID().uuidString
        let namescope = params?.context?.pipeNameSpace ?? "host"
        super.init { builder in
            lynxConfig = LynxConfig(provider: params?.context?.templateProvider ?? Self.globalResourceProvider)
            builder.config = lynxConfig
            builder.config?.register(NavigationModule.self)
            
            if let lynxConfig = lynxConfig {
                lynxConfig.spk_containerID = containerID
                lynxConfig.spk_namescope = namescope
                MethodPipe.setupLynxPipe(config: lynxConfig)
            }
            
            params?.context?.lynxModule?.forEach({ (name: String, params: Any) in
                guard let module = NSClassFromString(name) as? LynxModule.Type else {
                    return
                }
                if let numberParams = params as? NSNumber {
                    if numberParams.isEqual(to: 0) == true {
                        builder.config?.register(module, param: nil)
                        return
                    }
                }
                builder.config?.register(module, param: params)
            })
            
            builder.fetcher = params?.dynamicComponentFetcher ?? Self.globalResourceProvider
            builder.addLynxResourceProvider(LYNX_PROVIDER_TYPE_EXTERNAL_JS, provider: Self.globalResourceProvider)
            
            builder.setThreadStrategyForRender(.allOnUI)
            
            var URL = params?.sourceUrl ?? ""
            builder.lynxModuleExtraData = ["URL": URL]
            params?.context?.rawViewBuilderBlock?(builder)
        }
        self.containerID = containerID
        self.lynxConfig = lynxConfig
        self.params = params
        self.frame = frame
        self.layoutWidthMode = params?.widthMode as? LynxViewSizeMode ?? .undefined
        self.layoutHeightMode = params?.heightMode as? LynxViewSizeMode ?? .undefined
        self.preferredLayoutWidth = frame.size.width
        self.preferredLayoutHeight = frame.size.height
        self.setupGlobalProps()
        self.globalProps?.update(self.containerID, forKey: "containerID")
        
        self.setupPipe()
        
        self.internalResourceProvider.lynxView = self
        self.addLifecycleClient(self)
        self.loadState = .SPKLoadStateNotLoad
        self.triggerLayout()
    }
    
    
    /// Sets up the method pipe for communication between native and Lynx environments.
    ///
    /// This method creates a MethodPipe instance and registers any pipe methods
    /// from the context. The method pipe enables bidirectional communication
    /// between the native iOS code and the Lynx JavaScript runtime environment.
    private func setupPipe() {
        self.anyMethodPipe = MethodPipe(withLynxView: self)
        if let pipeMethods = self.params?.context?.pipeMethodInstances?.compactMap { $0 as? ( PipeMethod) }, !pipeMethods.isEmpty {
            self.methodPipe?.register(localMethods: pipeMethods)
        }
    }
    
    /// Sets up global properties for the Lynx view.
    ///
    /// This method creates default parameters including container initialization time
    /// and merges them with parameters from the Lynx kit configuration. Global
    /// properties are shared across the entire Lynx runtime and can be accessed
    /// by JavaScript code through the global props object.
    public func setupGlobalProps() {
        var defaultParams = [
            "containerInitTime": self.containerInitTimeStamp() ?? 0
        ]
        
        guard let params = self.params as? SPKLynxKitParams else {
            return
        }
        
        self.globalProps = SPKLynxKitUtils.globalProps(withParams: params, onDictionaryParamsCreated: { params in
            var newParams = params
            newParams?.merge(defaultParams, uniquingKeysWith: { _, new in new })
            var queryItems = params?.spk.dictionary(forKey: "queryItems") ?? [:]
            queryItems.merge(defaultParams, uniquingKeysWith: { _, new in new })
            newParams?["queryItems"] = queryItems
        })
    }
    
    /// Returns the container initialization timestamp.
    ///
    /// This method provides the timestamp when the container was initialized,
    /// which can be used for performance monitoring and analytics.
    ///
    /// - Returns: The timestamp when the container was initialized, currently returns 0
    public func containerInitTimeStamp() -> Int64? {
        //TODO: update monitor later.
        return 0
    }
    
    required public init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    /// Registers a Lynx module with the view's configuration.
    ///
    /// - Parameters:
    ///   - module: The module type to register
    ///   - param: Optional parameters for the module
    public func register(withModule module: any LynxModule.Type, param: Any?) {
        self.lynxConfig?.register(module, param: param)
    }
    
    /// Registers a shadow node class with a specific name.
    ///
    /// - Parameters:
    ///   - node: The shadow node class to register
    ///   - name: The name to associate with the shadow node
    public func register(withShadowNode node: AnyClass, withName name: String) {
        self.lynxConfig?.registerShadowNode(node, withName: name)
    }
    
    /// Registers a UI component class with a specific name.
    ///
    /// - Parameters:
    ///   - ui: The UI component class to register
    ///   - name: The name to associate with the UI component
    public func register(withUI ui: AnyClass, withName name: String) {
        self.lynxConfig?.registerUI(ui, withName: name)
    }
    
    //MARK: - Method Pipe Service
    
    /// Sends an event to the Lynx JavaScript runtime.
    ///
    /// - Parameters:
    ///   - event: The event name to send
    ///   - params: Optional parameters to include with the event
    ///   - callback: Optional callback to handle the response
    ///   
    public func send(event: String, params: [String : Any]? = nil, callback: ((Any?) -> Void)? = nil) {
        self.methodPipe?.fireEvent(name: event, params: params)
    }
    
    /// Configures the view with new parameters.
    ///
    /// This method updates the view's parameters, layout modes, and refreshes the global properties
    /// to reflect any changes in the configuration. It also triggers a layout update.
    ///
    /// - Parameter params: The new Lynx kit parameters to apply to the view
    public func config(withParams params: (any SPKHybridParams)?) {
        guard let lynxParams = params as? SPKLynxKitParams else {
            return
        }
        self.params = lynxParams
        self.layoutWidthMode = lynxParams.widthMode ?? .undefined
        self.layoutHeightMode = lynxParams.heightMode ?? .undefined
        self.setupGlobalProps()
        self.triggerLayout()
    }
    
    /// Loads content into the Lynx view based on the configured parameters.
    ///
    /// This method performs the following operations:
    /// - Prepares initial data from various property formats (String, Dictionary, LynxTemplateData)
    /// - Configures resource fetchers and template providers
    /// - Notifies lifecycle delegate of loading start
    /// - Loads the template from the specified source URL
    ///
    /// The method supports multiple initial property formats and automatically converts them
    /// to LynxTemplateData for consistent handling within the Lynx runtime.
    public func load() {
        guard let params = self.params as? SPKLynxKitParams else {
            return
        }
        
        var initialData: LynxTemplateData? = nil
        if params.initialProperties != nil {
            if let initialProperties = params.initialProperties as? String {
                initialData = LynxTemplateData(json: initialProperties)
            } else if let initialProperties = params.initialProperties as? [String: Any] {
                initialData = LynxTemplateData(dictionary: initialProperties)
            } else if let initialProperties = params.initialProperties as? LynxTemplateData {
                initialData = initialProperties
            }
        }
        
        self.setResourceFetcher(params.resourceFetcher ?? self.internalResourceProvider)
        self.imageFetcher = params.imageFetcher ?? self.internalResourceProvider
        self.internalResourceProvider.customTemplateProvider = params.templateProvider
        
        self.lifeCycleDelegate?.viewWillStartLoading?(self)

        // Push globalProps to the LynxView engine before loading the template.
        // setupGlobalProps() stores them in a private var but LynxView's
        // loadTemplate(fromURL:initData:) doesn't accept globalProps, so we
        // must call updateGlobalProps separately.
        if let gp = self.globalProps {
            self.updateGlobalProps(with: gp)
        }

        if var sourceUrl = params.sourceUrl {
            self.loadTemplate(fromURL: sourceUrl, initData: initialData)
        }
    }
    
    public func reload(_ context: SPKHybridContext?) {
        self.params = SPKLynxKitUtils.lynxKitParams(withContext: context)
        self.load()
    }
    
    public func onshow(params: [AnyHashable : Any]) {
        var event = SPKKitEvent.viewDidAppear
        if params != nil {
            event = params.spk.string(forKey: "event", default: event)
        }
        self.onEnterForeground()
        self.send(event: event)
    }
    
    public func onHide(params: [AnyHashable : Any]) {
        var event = SPKKitEvent.viewDidAppear
        if params != nil {
            event = params.spk.string(forKey: "event", default: event)
        }
        self.onEnterBackground()
        self.send(event: event)
    }
    
    public func config(withGlobalProps globalProps: Any?) {
        if let dictProps = globalProps as? [String: Any] {
            var dict = self.globalProps?.dictionary() ?? [:]
            dict.merge(dictProps, uniquingKeysWith: { _, new in new})
            self.globalProps = LynxTemplateData(dictionary: dict)
        } else if let templateProps = globalProps as? LynxTemplateData {
            self.globalProps?.update(with: templateProps)
        }
    }
    
    public func update(withGlobalProps globalProps: Any?) {
        self.config(withGlobalProps: globalProps)
        if let dictProps = globalProps as? [String: Any] {
            self.updateGlobalProps(with: dictProps)
        } else if let templateProps = globalProps as? LynxTemplateData {
            self.updateGlobalProps(with: templateProps)
        }
        self.send(event: "globalPropsUpdated", params: nil)
    }
    
    public func update(withData data: Any?, processorName processor: String? = nil) {
        guard data != nil else {
            return
        }
        if let dictData = data as? [String: Any] {
            self.updateData(with: dictData, processorName: processor)
        } else if let templateData = data as? LynxTemplateData {
            self.updateData(with: templateData)
        } else if let stringData = data as? String {
            self.updateData(with: stringData, processorName: processor)
        }
    }
    
    public func loadURLString() -> String? {
        guard let params = self.params as? SPKLynxKitParams else {
            return nil
        }
        return params.sourceUrl
    }
}

extension SPKWrapperLynxView: SPKLynxResourceProviderDelegate {
    
    public func resourceProviderDidStartLoad(withURL url: String) {
        self.lifeCycleDelegate?.view?(self, didStartFetchResourceWithURL: URL.spk.url(string: url))
    }
    
    public func resourceProviderDidFinishLoad(withURL url: String, resource: (any SPKResourceProtocol)?, error: (any Error)?) {
        self.lifeCycleDelegate?.view?(self, didFetchedResource: resource, error: error)
    }
}

protocol SPKUIKit {}

extension SPKWrapperLynxView: SPKUIKit {
    open override func triggerLayout() {
        switch self.layoutWidthMode {
        case .undefined, .max:
            self.preferredMaxLayoutWidth = self.frame.size.width
        case .exact:
            self.preferredLayoutWidth = self.frame.size.width
        default:
            self.preferredMaxLayoutWidth = self.frame.size.width
        }
        
        switch self.layoutHeightMode {
        case .undefined, .max:
            self.preferredMaxLayoutHeight = self.frame.size.height
        case .exact:
            self.preferredLayoutHeight = self.frame.size.height
        default:
            self.preferredMaxLayoutHeight = self.frame.size.height
        }
        super.triggerLayout()
    }
    
    open override func layoutSubviews() {
        super.layoutSubviews()
        self.triggerLayout()
    }
}

extension SPKWrapperLynxView: LynxViewLifecycle {
    public func lynxViewDidStartLoading(_ view: LynxView!) {
        self.loadState = .SPKLoadStateLoading
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.lifeCycleDelegate?.viewDidStartLoading?(self
            )
        }
    }
    
    public func lynxView(_ view: LynxView!, didLoadFinishedWithUrl url: String!) {
        self.loadState = .SPKLoadStateSucceed
        self.estimatedProgress = Float(1)
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.triggerLayout()
            self?.lifeCycleDelegate?.view?(self, didFinishLoadWithURL: URL.spk.url(string: url))
        }
    }
    
    public func lynxViewDidPageUpdate(_ view: LynxView!) {
        self.lifeCycleDelegate?.viewDidPageUpdate?(self)
    }
    
    public func lynxViewDidConstructJSRuntime(_ view: LynxView!) {
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.lifeCycleDelegate?.viewDidConstructJSRuntime?(self)
        }
    }
    
    public func lynxViewDidUpdate(_ view: LynxView!) {
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.lifeCycleDelegate?.viewDidUpdate?(self)
        }
    }
    
    public func lynxViewDidChangeIntrinsicContentSize(_ view: LynxView!) {
        self.lifeCycleDelegate?.view?(self, didChangeIntrinsicContentSize: view.intrinsicContentSize())
    }
    
    public func lynxView(_ view: LynxView!, didRecieveError error: (any Error)!) {
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.lifeCycleDelegate?.view?(self, didReceiveError: error)
        }
    }
    
    public func lynxViewDidFirstScreen(_ view: LynxView!) {
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.lifeCycleDelegate?.viewDidFirstScreen?(self)
        }
    }
    
    public func lynxView(_ view: LynxView!, didReceiveFirstLoadPerf perf: LynxPerformance!) {
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.lifeCycleDelegate?.view?(self, didReceivePerformance: perf.toDictionary())
        }
    }
    
    public func lynxView(_ view: LynxView!, didReceiveUpdatePerf perf: LynxPerformance!) {
        DispatchQueue.spk.asyncMain { [weak self] in
            self?.lifeCycleDelegate?.view?(self, didReceivePerformance: perf.toDictionary())
        }
    }
}
