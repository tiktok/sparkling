// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Protocol defining the interface for SPK scheme parameters.
/// 
/// This protocol establishes the contract for scheme parameter objects that control
/// the appearance and behavior of SPK containers, including navigation bar,
/// status bar, and title configuration.
@objc
public protocol SPKSchemeParamProtocol {
    /// Controls the visibility of the navigation bar.
    /// 
    /// When true, the navigation bar is hidden from view.
    var hideNavBar: Bool {set get}
    
    /// Controls the visibility of the status bar.
    /// 
    /// When true, the status bar is hidden from view.
    var hideStatusBar: Bool {set get}
    
    /// The title text displayed in the navigation bar.
    /// 
    /// This property sets the main title for the container.
    var title: String {set get}
    
    /// The color used for the title text.
    /// 
    /// This property controls the visual appearance of the title.
    var titleColor: UIColor {set get}
    
    /// The background color of the navigation bar.
    /// 
    /// This property sets the navigation bar's background color.
    var navBarColor: UIColor {set get}
    
    /// Controls whether the status bar should be transparent.
    /// 
    /// When true, the status bar background becomes transparent.
    var transStatusBar: Bool {set get}
    
    /// Controls whether the navigation bar remains visible when transStatusBar is true.
    /// 
    /// When transStatusBar is true, the navigation bar is hidden by default.
    /// Set this to true to keep the navigation bar visible.
    var showNavBarInTransStatusBar: Bool {set get}
}

/// A comprehensive parameter class for configuring SPK container appearance and behavior.
/// 
/// SPKSchemeParam extends SPKHybridSchemeParam to provide additional configuration
/// options specific to SPK containers. It handles parameter parsing from dictionaries,
/// theme-aware color resolution, and provides default values for all configuration options.
/// 
/// The class supports:
/// - Navigation bar and status bar customization
/// - Theme-aware color handling
/// - Loading and error view configuration
/// - Parameter merging and updates
/// 
/// The @objcMembers attribute ensures Objective-C compatibility for all properties and methods.
@objcMembers
open class SPKSchemeParam: SPKHybridSchemeParam, SPKSchemeParamProtocol {

    /// The status bar style for the container.
    /// 
    /// Controls the appearance of status bar content (light or dark).
    /// Defaults to .default system style.
    var statusFontMode: UIStatusBarStyle = .default
    
    /// Controls whether loading views should be displayed.
    /// 
    /// When true, loading indicators are shown during content loading.
    /// Defaults to true.
    var showLoading: Bool = true
    
    /// Controls whether error views should be displayed.
    /// 
    /// When true, error views are shown when content loading fails.
    /// Defaults to true.
    var showError: Bool = true
    
    /// The background color for the container.
    /// 
    /// This color is applied to the main container background.
    /// Defaults to white.
    var containerBgColor: UIColor = .white
    
    /// The background color for loading views.
    /// 
    /// This color is applied to loading view backgrounds.
    /// Defaults to white.
    var loadingBgColor: UIColor = .white
    
    /// Any error that occurred during parameter processing.
    /// 
    /// This optional property holds error information if parameter
    /// parsing or validation fails.
    var error: Error?
    
    /// Controls the visibility of the navigation bar.
    /// 
    /// When true, the navigation bar is hidden from view.
    /// Defaults to false (navigation bar is visible).
    public var hideNavBar: Bool = false
    
    /// Controls the visibility of the status bar.
    /// 
    /// When true, the status bar is hidden from view.
    /// Defaults to false (status bar is visible).
    public var hideStatusBar: Bool = false
    
    /// The title text displayed in the navigation bar.
    /// 
    /// This property sets the main title for the container.
    /// Defaults to "SPK Page".
    public var title: String = "SPK Page"
    
    /// The color used for the title text.
    /// 
    /// This property controls the visual appearance of the title.
    /// Defaults to black.
    public var titleColor: UIColor = .black
    
    /// The background color of the navigation bar.
    /// 
    /// This property sets the navigation bar's background color.
    /// Defaults to white.
    public var navBarColor: UIColor = .white
    
    /// Controls whether the status bar should be transparent.
    /// 
    /// When true, the status bar background becomes transparent.
    /// Defaults to false (opaque status bar).
    public var transStatusBar: Bool = false
    
    public var hideBackButton: Bool = false
    
    public var showNavBarInTransStatusBar: Bool = false
    
    /// Updates the scheme parameters with values from a dictionary.
    /// 
    /// This method processes a dictionary of parameters and updates the instance properties
    /// accordingly. It handles theme-aware color resolution, status bar style parsing,
    /// and boolean flag processing. The method supports both direct parameter mapping
    /// and context-aware theming.
    /// 
    /// - Parameters:
    ///   - dict: Dictionary containing parameter key-value pairs
    ///   - context: Optional context for theme-aware processing
    open override func update(withDictionary dict: [String : Any]?, context: SPKHybridContext? = nil) {
        var dict = self.dictionary(withInnerUrlQueryItems: dict) ?? [:]
        super.update(withDictionary: dict)
        let statusFontMode = dict.spk.string(forKey: "status_font_mode")
        if statusFontMode == "light" {
            self.statusFontMode = .lightContent
        } else if statusFontMode == "dark" {
            if #available(iOS 13.0, *) {
                self.statusFontMode = .darkContent
            }
        }
        
        let context = context as? SPKContext
        
        self.containerBgColor = self.themedColor(withDict: dict, forKey: "container_bg_color", context: context) ?? self.containerBgColor
        
        self.loadingBgColor = self.themedColor(withDict: dict, forKey: "loading_bg_color", context: context) ?? self.loadingBgColor
        
        self.showLoading = !dict.spk.bool(forKey: "hide_loading", default: false)

        self.showError = !dict.spk.bool(forKey: "hide_error", default: false)
        
        //MARK: PageSchemeParams
        self.hideNavBar = dict.spk.bool(forKey: "hide_nav_bar", default: self.hideNavBar)
        
        self.hideBackButton = dict.spk.bool(forKey: "hide_back_button", default: false)
        
        self.hideStatusBar = dict.spk.bool(forKey: "hide_status_bar", default: self.hideStatusBar)
        
        self.transStatusBar = dict.spk.bool(forKey: "trans_status_bar", default: self.transStatusBar)
        
        self.showNavBarInTransStatusBar = dict.spk.bool(forKey: "show_nav_bar_in_trans_status_bar", default: self.showNavBarInTransStatusBar)
        
        self.title = dict.spk.string(forKey: "title", default: self.title)
        
        self.titleColor = self.themedColor(withDict: dict ?? [:], forKey: "title_color", context: context) ?? self.titleColor
        
        self.navBarColor = self.themedColor(withDict: dict ?? [:], forKey: "nav_bar_color", context: context) ?? self.navBarColor
        
    }
    
    /// Updates the current parameters with values from another parameter object.
    /// 
    /// This method merges the current instance with another SPKHybridSchemeParam,
    /// combining extra dictionaries and updating URL references. The merge operation
    /// preserves existing values while incorporating new ones from the source parameter.
    /// 
    /// - Parameter newParam: The source parameter object to merge from
    open override func update(withParam newParam: SPKHybridSchemeParam) {
        super.update(withParam: newParam)
        var newExtraDict = self.extra
        newExtraDict.merge(newParam.extra) { _, new in new }
        self.originURL = newParam.originURL
        self.resolvedURL = newParam.resolvedURL
    }
    
    /// Resolves a themed color from a dictionary using the specified key.
    /// 
    /// This method performs theme-aware color resolution, supporting special color values
    /// like "transparent" and hex color strings. It integrates with the context to provide
    /// appropriate colors based on the current theme settings.
    /// 
    /// - Parameters:
    ///   - dict: Dictionary containing color configuration
    ///   - key: The key to look up in the dictionary
    ///   - context: Optional context for theme-aware color resolution
    /// - Returns: A UIColor instance, or nil if the color cannot be resolved
    public func themedColor(withDict dict: [String: Any], forKey key: String, context: SPKHybridContext?) -> UIColor? {
        let colorString = self.themedColorString(withDict: dict, forKey: key, context: context)
        
        if colorString == "transparent" {
            return .clear
        }
        
        if !isEmptyString(colorString) {
            let color = UIColor.spk.color(hexString: colorString)
            if color.isEqual(UIColor.clear) && isColorStringClear(colorString) {
                return color
            } else {
                return color
            }
        }
        return nil
    }
    
    func isColorStringClear(_ colorString: String) -> Bool {
        guard colorString.count == 8 else {
            return false
        }
        
        let validCharacters = CharacterSet(charactersIn: "0123456789ABCDEF")
        if colorString.uppercased().rangeOfCharacter(from: validCharacters.inverted) != nil {
            return false
        }
        
        let alpha = colorString.suffix(2)
        return alpha == "00"
    }
    
    
    public func themedColorString(withDict dict: [String: Any], forKey key: String, context: SPKHybridContext?) -> String {
        let defaultColorString = dict.spk.string(forKey: key, default: "")
        guard let context = context as? SPKContext else {
            return defaultColorString
        }
        var colorString = defaultColorString

        let forceTheme = dict.spk.string(forKey: "force_theme_style")
        if forceTheme == "light" || context.appTheme == .SPKAppThemeLight {
            colorString = dict.spk.string(forKey: "\(key)_light", default: defaultColorString)
            return colorString
        } else if forceTheme == "dark" || context.appTheme == .SPKAppThemeDark {
            colorString = dict.spk.string(forKey: "\(key)_dark", default: defaultColorString)
            return colorString
        }
        return colorString
    }
    
}

