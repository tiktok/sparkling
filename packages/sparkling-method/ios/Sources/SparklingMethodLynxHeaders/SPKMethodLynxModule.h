// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>
#import <Lynx/LynxModule.h>
#import <Lynx/LynxView.h>
#import "SPKMethodTransport.h"

/// Lynx module that converts one JavaScript call into a transport-neutral
/// SPKMethodCallMessage. Hosts register this module and a transport with the
/// same container identifier.
@interface SPKMethodLynxModule : NSObject <LynxModule>

+ (nonnull Class)callMessageClass;

- (nullable id<SPKMethodCallMessageHandler>)messageHandlerForContainerID:(nullable NSString *)containerID;

- (void)call:(nullable NSString *)name
      params:(nullable NSDictionary *)params
    callback:(nullable LynxCallbackBlock)callback;

- (void)handleMethodNamed:(nullable NSString *)name
                   params:(nullable NSDictionary *)params
                container:(nullable LynxView *)container
                 callback:(nullable LynxCallbackBlock)callback;

@end
