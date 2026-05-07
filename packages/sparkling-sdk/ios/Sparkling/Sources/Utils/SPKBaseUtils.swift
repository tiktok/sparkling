// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// A generic wrapper that provides a namespace for SPKKit extensions.
/// 
/// This wrapper struct serves as the foundation for the SPKKit extension system,
/// allowing clean separation of framework-specific functionality from native types.
/// It follows the namespace pattern commonly used in Swift frameworks to avoid
/// polluting the global namespace of extended types.
/// 
/// - Generic Parameter Base: The type being wrapped and extended
public struct SPKKitWrapper<Base> {
    /// The underlying instance being wrapped
    public let base: Base
    
    /// Creates a new wrapper around the provided base instance.
    /// 
    /// - Parameter base: The instance to wrap
    public init(_ base: Base) {
        self.base = base
    }
}

/// Protocol for reference types that can be extended with SPKKit functionality.
/// 
/// This protocol enables reference types (classes) to gain access to the SPKKit
/// extension namespace through the `spk` property. Types conforming to this protocol
/// automatically receive both instance and static access to SPKKit extensions.
/// 
/// - Note: This protocol is specifically for AnyObject types (reference types)
public protocol SPKKitCompatible: AnyObject {}

/// Protocol for value types that can be extended with SPKKit functionality.
/// 
/// This protocol enables value types (structs, enums) to gain access to the SPKKit
/// extension namespace through the `spk` property. Types conforming to this protocol
/// automatically receive both instance and static access to SPKKit extensions.
/// 
/// - Note: This protocol is for value types, complementing SPKKitCompatible
public protocol SPKKitCompatibleValue {}

public extension SPKKitCompatible {
    /// Provides access to SPKKit extensions for this instance.
    /// 
    /// This computed property creates a SPKKitWrapper around the current instance,
    /// enabling access to all SPKKit-specific functionality through a clean namespace.
    /// The setter is provided for protocol conformance but performs no operation.
    /// 
    /// - Returns: A SPKKitWrapper containing this instance
    var spk: SPKKitWrapper<Self> {
        get {
            return SPKKitWrapper(self)
        }
        set {}
    }

    /// Provides access to SPKKit static extensions for this type.
    /// 
    /// This computed property enables access to static/class-level SPKKit functionality
    /// through the same namespace pattern used for instance methods.
    /// The setter is provided for protocol conformance but performs no operation.
    /// 
    /// - Returns: The SPKKitWrapper type for static access
    static var spk: SPKKitWrapper<Self>.Type {
        get {
            return SPKKitWrapper.self
        }
        set {}
    }
}

public extension SPKKitCompatibleValue {
    /// Provides access to SPKKit extensions for this value type instance.
    /// 
    /// This computed property creates a SPKKitWrapper around the current value,
    /// enabling access to all SPKKit-specific functionality through a clean namespace.
    /// The setter is provided for protocol conformance but performs no operation.
    /// 
    /// - Returns: A SPKKitWrapper containing this value
    var spk: SPKKitWrapper<Self> {
        get {
            return SPKKitWrapper(self)
        }
        set {}
    }

    /// Provides access to SPKKit static extensions for this value type.
    /// 
    /// This computed property enables access to static/class-level SPKKit functionality
    /// for value types through the same namespace pattern used for instance methods.
    /// The setter is provided for protocol conformance but performs no operation.
    /// 
    /// - Returns: The SPKKitWrapper type for static access
    static var spk: SPKKitWrapper<Self>.Type {
        get {
            return SPKKitWrapper.self
        }
        set {}
    }
}
