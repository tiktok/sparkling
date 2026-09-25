// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethod.h"

/// Thread-safe method storage for one scope.
@interface SPKMethodRegistry<MethodType : SPKMethod *> : NSObject

/// A shallow snapshot of instantiated methods; it does not instantiate lazy entries.
@property (nonatomic, copy, readonly, nonnull) NSDictionary<NSString *, MethodType> *methods;

/// Host integration: retains the supplied storage without copying it.
/// Sequential external mutations remain visible. Access through Registry APIs is synchronized;
/// a host must not mutate a non-thread-safe supplied dictionary concurrently outside the Registry.
- (nonnull instancetype)initWithStorage:(nonnull NSMutableDictionary<NSString *, MethodType> *)storage;

/// Host integration: shares class declarations while keeping an independent instance cache.
/// Access through Registry APIs is synchronized. Does not copy the supplied class storage;
/// a host must not mutate a non-thread-safe supplied dictionary concurrently outside the Registry.
- (nonnull instancetype)initWithMethodClassStorage:(nonnull NSMutableDictionary<NSString *, Class> *)storage;

/// Keeps the first method registered under a name. Returns YES only for a new entry.
/// The method must have a nonnil name; engine and authorization checks belong to dispatch.
- (BOOL)registerMethod:(nonnull MethodType)method;

/// Returns the cached instance, or creates and caches a registered class under the requested name.
- (nullable MethodType)methodForName:(nonnull NSString *)name;

/// Registers or replaces a class declaration without constructing it or replacing a cached instance.
/// The class must create an instance compatible with MethodType; the host owns eligibility checks.
- (void)registerMethodClass:(nonnull Class)methodClass forName:(nonnull NSString *)name;

/// Metadata lookup only; does not construct or cache an instance.
- (nullable Class)methodClassForName:(nonnull NSString *)name;

/// Registers or replaces a method under a captured name, without reading methodName again.
/// Use registerMethod: when duplicate registrations should keep the first method.
- (void)registerMethod:(nonnull MethodType)method forName:(nonnull NSString *)name;

/// Removes the cached instance without changing snapshots or cancelling calls in progress.
/// A class declaration remains available and may recreate the instance on the next lookup.
- (void)deregisterMethodNamed:(nonnull NSString *)name;

/// Filter each scope before merging; an eligible local entry overrides the global entry.
/// A nil predicate includes every method. Inputs must not mutate during enumeration.
+ (nonnull NSDictionary<NSString *, __kindof SPKMethod *> *)methodsByMergingGlobalMethods:(nullable NSDictionary<NSString *, SPKMethod *> *)globalMethods
                                                                           localMethods:(nullable NSDictionary<NSString *, SPKMethod *> *)localMethods
                                                                              predicate:(BOOL (^ _Nullable)(SPKMethod * _Nonnull method))predicate;

@end
