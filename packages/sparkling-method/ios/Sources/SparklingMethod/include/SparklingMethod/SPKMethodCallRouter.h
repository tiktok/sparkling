// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodCallMessage.h"
#import "SPKMethodInvocation.h"
#import "SPKMethodTransport.h"

@class SPKMethodRuntime;

typedef SPKMethodInvocationHooks * _Nullable (^SPKMethodInvocationHooksProvider)(SPKMethodCallMessage * _Nonnull message);

/// Routes transport messages through the runtime and assembles their responses.
@interface SPKMethodCallRouter : NSObject <SPKMethodCallMessageHandler>

@property (nonatomic, strong, readonly, nonnull) SPKMethodRuntime *runtime;
@property (nonatomic, copy, nullable) SPKMethodInvocationHooksProvider hooksProvider;

- (nonnull instancetype)initWithRuntime:(nonnull SPKMethodRuntime *)runtime;
- (nonnull instancetype)init NS_UNAVAILABLE;

@end
