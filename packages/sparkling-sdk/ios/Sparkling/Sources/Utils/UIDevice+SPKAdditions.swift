// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

extension UIDevice: SPKKitCompatible {}

public extension SPKKitWrapper where Base: UIDevice {
    static var hwModel: String? {
        return self.getSysInfoByName("hw.model")
    }
    private static func getSysInfoByName(_ typeSpecifier: String) -> String? {
        var size: size_t = 0
        if sysctlbyname(typeSpecifier, nil, &size, nil, 0) != 0 {
            return nil
        }

        var answer = [CChar](repeating: 0, count: size)
        if sysctlbyname(typeSpecifier, &answer, &size, nil, 0) != 0 {
            return nil
        }
        return String(cString: answer)
    }
    static var isIPhoneXSeries: Bool {
        if #available(iOS 11.0, *) {
            if let window = UIApplication.shared.windows.first {
                return window.safeAreaInsets.bottom > 0
            }
        }
        
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let platform = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
        
        let xSeriesRegex = try? NSRegularExpression(pattern: "^iPhone(1[0-9]|[2-9][0-9]),", options: [])
        let isXSeriesModel = xSeriesRegex?.firstMatch(in: platform, options: [], range: NSRange(location: 0, length: platform.count)) != nil
        
        if platform.contains("x86_64") || platform.contains("arm64") {
            let deviceName = UIDevice.current.name
            let modelName = UIDevice.current.model
            
            if modelName.contains("iPhone") && self.isScreenHeightLarge736() {
                return true
            }
        }
        
        return isXSeriesModel
    }
    
    private static func isScreenHeightLarge736() -> Bool {
        let size = UIScreen.main.bounds.size
        let len = max(size.height, size.width)
        return len > 736;
    }
}
