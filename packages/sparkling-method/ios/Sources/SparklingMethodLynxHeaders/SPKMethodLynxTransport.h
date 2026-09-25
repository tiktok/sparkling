// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>
#import <Lynx/LynxView.h>
#import "SPKMethodTransport.h"

/// Lynx transport that associates one container identifier with one LynxView.
/// Register SPKMethodLynxModule with the same identifier in the Lynx config.
@interface SPKMethodLynxTransport : NSObject <SPKMethodTransport, SPKMethodCallMessageHandler>

@property (nonatomic, weak, nullable) id<SPKMethodCallMessageHandler> messageHandler;
@property (nonatomic, weak, readonly, nullable) LynxView *lynxView;
@property (nonatomic, copy, readonly, nonnull) NSString *containerID;

- (nonnull instancetype)initWithLynxView:(nonnull LynxView *)lynxView
                             containerID:(nonnull NSString *)containerID NS_DESIGNATED_INITIALIZER;
- (nonnull instancetype)init NS_UNAVAILABLE;

@end
