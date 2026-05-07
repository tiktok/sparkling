// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// A customizable navigation bar component for SPKKit containers.
/// 
/// `SPKNavigationBar` provides a flexible navigation bar implementation with support for
/// left, right, and close buttons, customizable styling, and action handling. It serves as the
/// primary navigation interface for SPK containers and supports both programmatic and
/// protocol-based configuration.
/// 
/// The navigation bar includes:
/// - Customizable title with font and color options
/// - Left navigation button (typically back button)
/// - Close button for dismissing containers
/// - Right navigation button for additional actions
/// - Bottom separator line with configurable appearance
/// 
@objcMembers
open class SPKNavigationBar: UIView {
    
    /// Type alias for navigation bar button action closures.
    /// 
    /// - Parameter navigationBar: The navigation bar instance that triggered the action
    typealias SPKNavigationBarAction = ((SPKNavigationBar) -> Void)?
    
    var bottomLineHeight: CGFloat = 0.5
    
    var bottomLineColor: UIColor? {
        set {
            self.sepLine.backgroundColor = newValue
        }
        get {
            return self.sepLine.backgroundColor
        }
    }
    
    var titleFont: UIFont {
        set {
            self.titleLabel.font = newValue
        }
        get {
            return self.titleLabel.font
        }
    }
    
    var titleColor: UIColor? {
        set {
            self.titleLabel.textColor = newValue
            self.leftNaviButton.tintColor = newValue
            self.rightNaviButton.tintColor = newValue
        }
        get {
            return self.titleLabel.textColor
        }
    }
    
    var title: String? {
        set {
            self.titleLabel.text = newValue
        }
        get {
            return self.titleLabel.text
        }
    }
    
    var leftButtonAction: SPKNavigationBarAction = nil
    
    var closeButtonAction: SPKNavigationBarAction = nil
    
    var rightButtonAction: SPKNavigationBarAction = nil
    
    var titleLabel: UILabel = UILabel(frame: .zero)
    
    lazy var leftNaviButton: UIButton = {
        let button = Self.button(target: self, action: #selector(handleLeftButton(_:)))
        return button
    }()
    
    var leftButtonImage: UIImage? {
        didSet {
            self.updateUI()
        }
    }
    
    var leftButtonBackgroundImage: UIImage? {
        didSet {
            self.updateUI()
        }
    }
    
    var leftButtonTitle: String? {
        didSet {
            self.updateUI()
        }
    }
    
    var leftButtonFont: UIFont? {
        didSet {
            self.updateUI()
        }
    }
    
    var leftButtonTitleColor: UIColor? {
        didSet {
            self.updateUI()
        }
    }
    
    lazy var closeNaviButton: UIButton = {
        let button = Self.button(target: self, action: #selector(handleCloseButton(_:)))
        return button
    }()
    
    var closeButtonImage: UIImage? {
        didSet {
            self.updateUI()
        }
    }
    
    var closeButtonBackgroundImage: UIImage? {
        didSet {
            self.updateUI()
        }
    }
    
    var closeButtonTitle: String? {
        didSet {
            self.updateUI()
        }
    }
    
    var closeButtonFont: UIFont? {
        didSet {
            self.updateUI()
        }
    }
    
    var closeButtonTitleColor: UIColor? {
        didSet {
            self.updateUI()
        }
    }
    
    lazy var rightNaviButton: UIButton = {
        let button = Self.button(target: self, action: #selector(handleRightButton(_:)))
        return button
    }()
    
    var sepLine: UIView = UIView(frame: .zero)
    
    var rightButtonImage: UIImage? {
        didSet {
            self.rightNaviButton.setImage(self.rightButtonImage?.withRenderingMode(.alwaysTemplate), for: .normal)
            self.updateUI()
        }
    }
    
    var rightButtonBackgroundImage: UIImage? {
        didSet {
            self.updateUI()
        }
    }
    
    var rightButtonTitle: String? {
        didSet {
            self.updateUI()
        }
    }
    
    var rightButtonFont: UIFont? {
        didSet {
            self.updateUI()
        }
    }
    
    var rightButtonTitleColor: UIColor? {
        didSet {
            self.updateUI()
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.commonInit()
    }
    
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.commonInit()
    }
    
    func setLeftButtonActionBlock(_ block: SPKNavigationBarAction) {
        self.leftButtonAction = block
    }
    
    func setCloseButtonActionBlock(_ block: SPKNavigationBarAction) {
        self.closeButtonAction = block
    }
    
    func setRightButtonActionBlock(_ block: SPKNavigationBarAction) {
        self.rightButtonAction = block
    }
    
    func commonInit() {
        self.backgroundColor = .white
        self.titleLabel = UILabel(frame: .zero)
        self.titleLabel.textAlignment = .center
        self.titleLabel.textColor = UIColor(red: 0.098, green: 0.098, blue: 0.098, alpha: 1)
        self.titleLabel.font = UIFont.systemFont(ofSize: 18.0, weight: .regular)
        self.addSubview(titleLabel)
        
        self.sepLine = UIView(frame: .zero)
        self.sepLine.backgroundColor = UIColor(red: 0.91, green: 0.91, blue: 0.91, alpha: 1)
        self.addSubview(self.sepLine)
        
        self.bottomLineHeight = 0.5
        self.leftButtonFont = UIFont.systemFont(ofSize: 14.0)
        self.rightButtonFont = UIFont.systemFont(ofSize: 14.0)
    }
    
    func updateUI() {
        let containerHeight = 44.0
        if self.leftButtonImage != nil || self.leftButtonTitle != nil || (self.leftButtonAction != nil) {
            self.addSubview(self.leftNaviButton)
            self.leftNaviButton.setImage(self.leftButtonImage?.withRenderingMode(.alwaysTemplate), for: .normal)
            self.leftNaviButton.setBackgroundImage(self.leftButtonBackgroundImage, for: .normal)
            self.leftNaviButton.setTitle(self.leftButtonTitle, for: .normal)
            self.leftNaviButton.titleLabel?.font = self.leftButtonFont
            if self.leftButtonTitleColor != nil {
                self.leftNaviButton.setTitleColor(self.leftButtonTitleColor, for: .normal)
                self.leftNaviButton.setTitleColor(self.leftButtonTitleColor, for: .highlighted)
                self.leftNaviButton.tintColor = self.leftButtonTitleColor
            } else {
                self.leftNaviButton.tintColor = UIColor(red: 0.098, green: 0.098, blue: 0.098, alpha: 1)
                self.leftNaviButton.setTitleColor(UIColor(red: 0.91, green: 0.91, blue: 0.91, alpha: 1), for: .normal)
                self.leftNaviButton.setTitleColor(UIColor(red: 0.098, green: 0.098, blue: 0.098, alpha: 1), for: .highlighted)
            }
            self.leftNaviButton.snp.remakeConstraints { make in
                make.left.bottom.equalTo(self)
                make.height.equalTo(containerHeight)
                make.width.equalTo(40)
            }
        } else {
            self.leftNaviButton.removeFromSuperview()
        }
        
        if self.rightButtonImage != nil || self.rightButtonTitle != nil || self.rightButtonAction != nil {
            self.addSubview(self.rightNaviButton)
            self.rightNaviButton.setImage(self.rightButtonImage?.withRenderingMode(.alwaysTemplate), for: .normal)
            self.rightNaviButton.setBackgroundImage(self.rightButtonBackgroundImage, for: .normal)
            self.rightNaviButton.setTitle(self.rightButtonTitle, for: .normal)
            self.rightNaviButton.titleLabel?.font = self.rightButtonFont
            
            if self.rightButtonTitleColor != nil {
                self.rightNaviButton.setTitleColor(self.rightButtonTitleColor, for: .normal)
                self.rightNaviButton.setTitleColor(self.rightButtonTitleColor, for: .highlighted)
            } else {
                self.rightNaviButton.tintColor = UIColor(red: 0.098, green: 0.098, blue: 0.098, alpha: 1)
                self.rightNaviButton.setTitleColor(UIColor(red: 0.91, green: 0.91, blue: 0.91, alpha: 1), for: .normal)
            }
            self.rightNaviButton.snp.remakeConstraints { make in
                make.right.bottom.equalTo(self)
                make.height.equalTo(containerHeight)
                make.width.equalTo(40)
            }
        } else {
            self.rightNaviButton.removeFromSuperview()
        }
        
        var leftOffset = 20
        if self.leftNaviButton.superview != nil {
            leftOffset += 40
        }
        
        if self.closeNaviButton.superview != nil {
            leftOffset += 40
        }
        
        self.titleLabel.snp.remakeConstraints { make in
            make.left.equalTo(self).offset(leftOffset)
            make.right.equalTo(self).offset(-leftOffset)
            make.height.equalTo(containerHeight)
            make.bottom.equalTo(self)
        }
        self.sepLine.snp.remakeConstraints { make in
            make.leading.trailing.bottom.equalTo(self)
            make.height.equalTo(self.bottomLineHeight)
        }
    }
    
    @objc func handleLeftButton(_ sender: UIButton) {
        self.leftButtonAction?(self)
    }
    
    @objc func handleCloseButton(_ sender: UIButton) {
        self.closeButtonAction?(self)
    }
    
    @objc func handleRightButton(_ sender: UIButton) {
        self.rightButtonAction?(self)
    }
    
    static func button(target: Any, action: Selector) -> UIButton {
        let button = UIButton(type: .custom)
        button.setTitleColor(UIColor(red: 0.91, green: 0.91, blue: 0.91, alpha: 1), for: .normal)
        button.setTitleColor(UIColor(red: 0.98, green: 0.98, blue: 0.98, alpha: 1), for: .highlighted)
        button.addTarget(target, action: action, for: .touchUpInside)
        return button
    }
}


// MARK: - SPKNavigationBarProtocol

/// Extension implementing the SPKNavigationBarProtocol.
/// 
/// This extension provides protocol conformance for navigation bar customization,
/// allowing external components to interact with the navigation bar through a standardized interface.
extension SPKNavigationBar: SPKNavigationBarProtocol {
    weak var param: SPKSchemeParam? {
        set {
            self.spk.setAttachedObject(key: "param", object: newValue, weak: true)
        }
        get {
            self.spk.getAttachedObject(key: "param", weak: true)
        }
    }
    
    public weak var container: (any UIViewController & SPKContainerProtocol)? {
        set {
            self.spk.setAttachedObject(key: "container", object: newValue, weak: true)
        }
        get {
            self.spk.getAttachedObject(key: "container", weak: true)
        }
    }
    
    open var didTapLeftButtonActionBlock: (() -> Void)? {
        set {
            self.spk.setAttachedObject(key: "didTapLeftButtonActionBlock", object: newValue)
        }
        get {
            return self.spk.getAttachedObject(key: "didTapLeftButtonActionBlock")
        }
    }
    
    open var didTapRightButtonActionBlock: (() -> Void)? {
        set {
            self.spk.setAttachedObject(key: "didTapRightButtonActionBlock", object: newValue)
        }
        get {
            return self.spk.getAttachedObject(key: "didTapRightButtonActionBlock")
        }
    }
    
    public static func navigationBar() -> (any UIView & SPKNavigationBarProtocol) {
        let safeAreaTop = UIApplication.spk.mainWindow?.spk.safeAreaInsets.top ?? 0
        let navigationBarHeight = 44.0
        let navibar = SPKNavigationBar(frame: CGRect(x: 0, y: 0, width: CGRectGetWidth(UIScreen.main.bounds), height: safeAreaTop + navigationBarHeight))
        return navibar
    }
    
    public func attachToContainer(_ params: SPKHybridSchemeParam) {
        guard let container =  self.container as? SPKViewController,
              let params = params as? SPKSchemeParam else {
            return
        }
        
        self.param = params
        self.leftNaviButton.isHidden = params.hideNavBar
        
        if let leftButtonImage = self.leftButtonImage,
           self.param?.hideBackButton == false {
            let frameworkBundle = Bundle(for: SPKNavigationBar.self)
            if let resURL = frameworkBundle.url(forResource: "sparklingPageResource", withExtension: "bundle"),
               let resourceBundle = Bundle(url: resURL),
               let image = UIImage(named: "icon_navibar_back", in: resourceBundle, compatibleWith: nil) {
                self.leftButtonImage = image
            }
        }
        
        self.backgroundColor = params.navBarColor
        self.titleColor = params.titleColor
        self.setLeftButtonActionBlock { [weak self] _ in
            self?.didTapLeftButtonActionBlock?()
        }
        self.setRightButtonActionBlock { [weak self] _ in
            self?.didTapRightButtonActionBlock?()
        }
    }
    
    open func update(centerTitle title: String) {
        self.title = title
    }
    
    open func update(titleColor color: UIColor) {
        self.titleColor = color
    }
    
    open func set(navigationBarBackButtonEnable enable: Bool) {
        self.leftNaviButton.isEnabled = enable
        self.closeNaviButton.isEnabled = enable
    }
    
    open func setup(leftButton barButtonItem: any SPKNavigationBarButtonProtocol) {
        self.leftButtonImage = barButtonItem.icon
        if let navBarHandler = barButtonItem.navBarHandler {
            self.didTapLeftButtonActionBlock = { [weak self] in
                navBarHandler(self?.container)
            }
        }
    }
    
    open func setup(rightButton barButtonItem: any SPKNavigationBarButtonProtocol) {
        self.rightButtonImage = barButtonItem.icon
        if let navBarHandler = barButtonItem.navBarHandler {
            self.didTapRightButtonActionBlock = { [weak self] in
                navBarHandler(self?.container)
            }
        }
    }
}
