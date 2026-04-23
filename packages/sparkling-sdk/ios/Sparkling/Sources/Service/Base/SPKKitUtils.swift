// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

//TODO: try to optimize this later
import Lynx

/// Utility class providing helper methods for SPK Kit operations.
/// 
/// This class contains static utility methods for common operations
/// such as updating global properties and managing hybrid context data.
/// It handles different data types and provides type-safe operations.
@objcMembers
open class SPKKitUtils: NSObject {
    /// Updates global properties in a hybrid context with new values.
    /// 
    /// This method merges new global properties into the existing context,
    /// handling both dictionary-based and LynxTemplateData-based global properties.
    /// The merge operation preserves existing values unless explicitly overridden.
    /// 
    /// - Parameters:
    ///   - context: The hybrid context to update. If nil, no operation is performed.
    ///   - newGlobalProps: Dictionary of new properties to merge. If nil, treated as empty.
    /// 
    /// - Note: This method supports two global property formats:
    ///   - Dictionary format: Standard key-value pairs
    ///   - LynxTemplateData format: Lynx-specific template data structure
    public class func updateGlobalProps(withContext context: SPKHybridContext?, newGlobalProps: [String: Any]?) {
        guard let context = context else {
            return
        }
        context.globalProps = context.globalProps ?? [:]
        if var dict = context.globalProps as? [String: Any] {
            dict.merge(newGlobalProps ?? [:]) { _, new in new }
            context.globalProps = dict
            return
        }
        
        if var globalProps = context.globalProps as? LynxTemplateData {
            let newData = LynxTemplateData(dictionary: newGlobalProps)
            globalProps.update(with: newData)
        }
        return
    }
}
