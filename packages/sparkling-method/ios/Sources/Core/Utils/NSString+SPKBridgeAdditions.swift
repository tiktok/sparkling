// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

extension String {
    /// Remove sandbox path prefix from path
    public func spk_stringByStrippingSandboxPath() -> String {
        // Implement simplified version to remove common sandbox path prefixes
        let fileManager = FileManager.default
        let homeDirectory = NSHomeDirectory()

        if self.hasPrefix(homeDirectory) {
            return self.replacingOccurrences(of: homeDirectory, with: "")
        }
        return self
    }

    /// Get actual path from processed file path
    public func spk_stringFromProcessFile() -> String {
        // Implement simplified version, assuming input is already a full path
        let fileManager = FileManager.default
        if fileManager.fileExists(atPath: self) {
            return self
        }
        // Try adding sandbox path prefix
        let homeDirectory = NSHomeDirectory()
        let fullPath = homeDirectory + self
        if fileManager.fileExists(atPath: fullPath) {
            return fullPath
        }
        return self
    }
}
