// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// A utility class that provides version information for the SPKKit framework.
/// 
/// `SPKVersion` serves as a centralized source for framework version information,
/// which can be useful for debugging, logging, compatibility checks, and support purposes.
/// The version information follows semantic versioning principles (major.minor.patch).
/// 
/// This class is designed to be lightweight and provides a simple interface for
/// accessing the current framework version without requiring complex initialization
/// or configuration.
/// 
/// - Note: The version string is hardcoded and should be updated with each framework release.
///   Consider integrating this with your build system to automatically update version information.
@objcMembers
open class SPKVersion: NSObject {
    
    /// Returns the current version string of the SPKKit framework.
    /// 
    /// This method provides the version identifier for the current build of the framework.
    /// The version follows semantic versioning format (MAJOR.MINOR.PATCH) and can be
    /// used for compatibility checks, logging, debugging, and user support scenarios.
    /// 
    /// The version information is particularly useful when:
    /// - Reporting bugs or issues to support teams
    /// - Implementing version-specific feature flags or compatibility layers
    /// - Logging framework usage for analytics or debugging purposes
    /// - Displaying framework information in about screens or debug panels
    /// 
    /// - Returns: A string representing the current framework version (e.g., "1.0.0")
    /// 
    /// - Note: This method returns a static string that should be updated during the build process
    ///   or release workflow to reflect the actual framework version.
    static func SPKVersion() -> String {
        return "1.0.0"
    }
}
