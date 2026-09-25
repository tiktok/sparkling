// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

@class SPKMethodLynxTransport;

@interface SPKMethodLynxTransportPool : NSObject

+ (nullable SPKMethodLynxTransport *)transportForContainerID:(nullable NSString *)containerID;
+ (void)setTransport:(nullable SPKMethodLynxTransport *)transport
      forContainerID:(nullable NSString *)containerID;

@end
