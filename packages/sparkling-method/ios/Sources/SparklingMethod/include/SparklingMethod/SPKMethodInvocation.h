// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethod.h"

typedef void (^SPKMethodInvocationCompletionHandler)(SPKMethodStatusCode statusCode, NSDictionary * _Nullable result, NSString * _Nullable message);

/// Optional host integrations for one invocation.
/// Hooks run on the invocation/completion thread and must not deliver the final callback.
@interface SPKMethodInvocationHooks : NSObject

/// Held weakly while waiting, then kept alive for the duration of each completion.
@property (nonatomic, weak, nullable) id completionOwner;

@property (nonatomic, copy) BOOL (^ _Nullable shouldInvoke)(NSDictionary * _Nullable params);
@property (nonatomic, copy) void (^ _Nullable willInvoke)(NSDictionary * _Nullable params, SPKMethodModel * _Nullable paramModel);
/// Runs when the method entry returns, including when its completion is still pending.
@property (nonatomic, copy) void (^ _Nullable didInvoke)(SPKMethodModel * _Nullable paramModel);

@property (nonatomic, copy) void (^ _Nullable didReceiveCompletion)(void);
@property (nonatomic, copy) void (^ _Nullable willComplete)(SPKMethodModel * _Nullable resultModel, SPKMethodStatusCode statusCode, NSString * _Nullable message);
/// Replaces the default NSAssert sink for result type and serialization checks.
@property (nonatomic, copy) void (^ _Nullable assertionHandler)(BOOL condition, NSString * _Nonnull message);
/// Runs after serialization, only when a result model exists, before response assembly.
@property (nonatomic, copy) NSDictionary * _Nullable (^ _Nullable transformResult)(NSDictionary * _Nullable result);
/// Overrides the default Web + forceCopyToData policy at completion time.
@property (nonatomic, copy) BOOL (^ _Nullable shouldCopyToData)(void);

@property (nonatomic, copy) void (^ _Nullable willDeliver)(SPKMethodStatusCode statusCode, NSString * _Nullable message, SPKMethodModel * _Nullable resultModel);
@property (nonatomic, copy) void (^ _Nullable didDeliver)(SPKMethodStatusCode statusCode, NSString * _Nullable message, NSDictionary * _Nonnull result);

@end
