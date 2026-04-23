// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import UIKit

public extension SPKKitWrapper where Base: UIColor {
    static func color(hexString string: String, alpha: CGFloat = 1.0) -> UIColor {
        let string = string.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        var hexString: String
        if string.hasPrefix("0x") {
            hexString = String(string.dropFirst(2))
        } else if string.hasPrefix("#") {
            hexString = String(string.dropFirst(1))
        } else {
            hexString = string
        }

        switch hexString.count {
        case 3:
            hexString = hexString.reduce(into: "") { $0.append($1); $0.append($1) }
        case 6:
            break
        case 8:
            break
        default:
            return .clear
        }
        let scanner = Scanner(string: hexString)
        var value: UInt32 = 0
        scanner.scanHexInt32(&value)
        if hexString.count != 8 {
            return color(rgb: value, alpha: alpha)
        } else {
            return color(rgba: value)
        }
    }

    /// Initialize a UIColor with the RGB hex value and alpha.
    /// 
    /// - Parameters:
    ///   - rgb: A RGB hex value.
    ///   - alpha: The transparency value from 0.0 to 1.0. Default is 1.0.
    /// - Returns: UIColor instance with the specified RGB values and alpha.
    static func color(rgb: UInt32, alpha: CGFloat = 1) -> UIColor {
        return UIColor(
            red: CGFloat((rgb & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgb & 0xFF00) >> 8) / 255.0,
            blue: CGFloat(rgb & 0xFF) / 255.0,
            alpha: alpha
        )
    }

    /// Initialize a UIColor with the RGBA hex value.
    /// 
    /// - Parameter rgba: A RGBA hex value.
    /// - Returns: UIColor instance with the specified RGBA values.
    static func color(rgba: UInt32) -> UIColor {
        return UIColor(
            red: CGFloat((rgba & 0xFF000000) >> 24) / 255.0,
            green: CGFloat((rgba & 0xFF0000) >> 16) / 255.0,
            blue: CGFloat((rgba & 0xFF00) >> 8) / 255.0,
            alpha: CGFloat(rgba & 0xFF) / 255.0
        )
    }

    /// Return the RGB/RGBA hex string of the color. For example, 0x0066cc. Return nil if the color is not in RGB color space.
    /// - Parameter withAlpha: return alpha value or not.
    /// - Returns: Return the RGB/RGBA hex string of the color
    private func hexString(withAlpha: Bool = false) -> String? {
        guard let components = base.cgColor.components else {
            return nil
        }
        
        let r, g, b, a: Double
        
        if components.count >= 3 {
            r = Double(components[0])
            g = Double(components[1])
            b = Double(components[2])
            a = components.count >= 4 ? Double(components[3]) : 1.0
        } else if components.count >= 2 {
            let gray = Double(components[0])
            r = gray
            g = gray
            b = gray
            a = Double(components[1])
        } else {
            return nil
        }
        
        if withAlpha {
            return String(format: "%02lx%02lx%02lx%02lx", lround(r * 255), lround(g * 255), lround(b * 255), lround(a * 255))
        } else {
            return String(format: "%02lx%02lx%02lx", lround(r * 255), lround(g * 255), lround(b * 255))
        }
    }

    var hexString: String? { hexString(withAlpha: false) }
    
    var hexStringWithAlpha: String? { hexString(withAlpha: true) }
}
