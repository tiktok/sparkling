// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodCallMessage.h"
#import "SPKMethodInvocation.h"
#import "SPKMethodTransport.h"

@class SPKMethodRuntime;

typedef SPKMethodInvocationHooks * _Nullable (^SPKMethodInvocationHooksProvider)(SPKMethodCallMessage * _Nonnull message);
typedef void (^SPKMethodHostCallHandler)(SPKMethodCallMessage * _Nonnull message,
                                         SPKMethodResponseBlock _Nonnull resultHandler);

/// Shared transport entry. Standalone calls use the runtime and assemble the
/// transport response; hosts with an existing dispatch policy may inject it.
@interface SPKMethodCallRouter : NSObject <SPKMethodCallMessageHandler>

@property (nonatomic, strong, readonly, nonnull) SPKMethodRuntime *runtime;
@property (nonatomic, copy, nullable) SPKMethodInvocationHooksProvider hooksProvider;
/// Compatibility hook for hosts that already select and schedule methods.
/// When set, the host handles this message and its response unchanged;
/// hooksProvider applies only to the default runtime path.
@property (nonatomic, copy, nullable) SPKMethodHostCallHandler hostCallHandler;

- (nonnull instancetype)initWithRuntime:(nonnull SPKMethodRuntime *)runtime;
- (nonnull instancetype)init NS_UNAVAILABLE;

@end
