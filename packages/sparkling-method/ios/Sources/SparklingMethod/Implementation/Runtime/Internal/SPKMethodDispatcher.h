// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodRegistry.h"
#import "SPKMethodInvocation.h"

typedef void (^SPKMethodDispatchHandler)(NSDictionary * _Nullable params, SPKMethodInvocationCompletionHandler _Nullable completionHandler);
typedef SPKMethodDispatchHandler _Nullable (^SPKMethodDispatchResolver)(NSString * _Nullable methodName);

/// Name-based dispatch into the shared runtime. Authorization belongs to the host.
@interface SPKMethodDispatcher : NSObject

/// Retains the registries. SPKMethodRegistry synchronizes its own storage.
- (nonnull instancetype)initWithLocalRegistry:(nullable SPKMethodRegistry *)localRegistry
                               globalRegistry:(nullable SPKMethodRegistry *)globalRegistry;

/// Selects an engine-compatible local method, then falls back to the global scope.
/// Missing or unsupported methods complete with UnregisteredMethod and a nil result.
/// Runs on the caller's thread and performs no authorization checks.
- (void)invokeMethodNamed:(nullable NSString *)methodName
                  params:(nullable NSDictionary *)params
              engineType:(SPKMethodEngineType)engineType
                   hooks:(nullable SPKMethodInvocationHooks *)hooks
       completionHandler:(nullable SPKMethodInvocationCompletionHandler)completionHandler;

/// Host integration: resolve local first, global only on a miss, then hand off once.
/// Resolvers run synchronously and may adapt existing host registrations.
/// invocation owns thread scheduling and calls the selected handler with current parameters.
/// Returns NO without calling invocation when neither scope resolves the name.
+ (BOOL)dispatchMethodNamed:(nullable NSString *)methodName
             localResolver:(nullable SPKMethodDispatchResolver)localResolver
            globalResolver:(nullable SPKMethodDispatchResolver)globalResolver
                invocation:(void (^ _Nonnull)(SPKMethodDispatchHandler _Nonnull handler))invocation;

@end
