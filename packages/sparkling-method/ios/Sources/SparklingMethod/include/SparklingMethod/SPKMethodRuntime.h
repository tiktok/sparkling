// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodRegistry.h"
#import "SPKMethodInvocation.h"
#import "SPKMethodEvent.h"
#import "SPKMethodEventSubscriber.h"
#import "SPKMethodEventCenter.h"

/// Shared entry for registration, invocation and events.
/// Registration and method lookup run on the caller's thread. Shared registries
/// synchronize their own storage across runtime instances.
@interface SPKMethodRuntime : NSObject

/// Host integration points. The runtime retains these components without copying their state.
@property (nonatomic, strong, readonly, nonnull) SPKMethodRegistry *localRegistry;
@property (nonatomic, strong, readonly, nonnull) SPKMethodRegistry *globalRegistry;
@property (nonatomic, strong, readonly, nonnull) SPKMethodEventCenter *eventCenter;

/// Creates a fresh local scope and uses the process-wide default global registry
/// and SPKMethodEventCenter.sharedCenter. Construction does not scan/register methods.
- (nonnull instancetype)init;
- (nonnull instancetype)initWithLocalRegistry:(nonnull SPKMethodRegistry *)localRegistry
                              globalRegistry:(nonnull SPKMethodRegistry *)globalRegistry
                                 eventCenter:(nonnull SPKMethodEventCenter *)eventCenter;

/// First registration under the method's name wins.
- (BOOL)registerLocalMethod:(nonnull SPKMethod *)method;
- (BOOL)registerGlobalMethod:(nonnull SPKMethod *)method;

/// Host integration: replace an entry under an already captured name.
- (void)registerLocalMethod:(nonnull SPKMethod *)method forName:(nonnull NSString *)name;
- (void)registerGlobalMethod:(nonnull SPKMethod *)method forName:(nonnull NSString *)name;
- (void)deregisterLocalMethodNamed:(nonnull NSString *)name;
- (void)deregisterGlobalMethodNamed:(nonnull NSString *)name;

/// Explicitly scans public declaration sections into this runtime's global registry.
/// Lazy mode records eligible classes; other classes register eagerly.
- (void)registerDeclaredGlobalMethodsLazily:(BOOL)lazily;

/// Resolves an eligible local method, then the global scope, and runs the shared pipeline.
/// Hooks belong to this invocation. No authorization or thread scheduling is added.
- (void)invokeMethodNamed:(nullable NSString *)methodName
                   params:(nullable NSDictionary *)params
               engineType:(SPKMethodEngineType)engineType
                    hooks:(nullable SPKMethodInvocationHooks *)hooks
        completionHandler:(nullable SPKMethodInvocationCompletionHandler)completionHandler;

/// Host integration for an already selected method, including engine-only registrations.
/// Preserves the selected instance; eligibility and authorization remain the host's responsibility.
/// Does not retain a runtime or registry while awaiting an asynchronous completion.
+ (void)invokeMethod:(nullable SPKMethod *)method
               params:(nullable NSDictionary *)params
           engineType:(SPKMethodEngineType)engineType
                hooks:(nullable SPKMethodInvocationHooks *)hooks
    completionHandler:(nullable SPKMethodInvocationCompletionHandler)completionHandler;

- (void)subscribeEventNamed:(nonnull NSString *)eventName withSubscriber:(nonnull SPKMethodEventSubscriber *)subscriber;
- (void)unsubscribeEventNamed:(nullable NSString *)eventName withSubscriber:(nonnull SPKMethodEventSubscriber *)subscriber;
- (void)publishEvent:(nonnull SPKMethodEvent *)event;

@end
