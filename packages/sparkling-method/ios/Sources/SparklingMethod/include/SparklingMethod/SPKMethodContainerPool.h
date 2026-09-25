// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>
#import "SPKMethodContainerProtocol.h"

/// Process-wide weak object lookup for host containers.
/// Container lifecycle remains owned by the integrating host.
@interface SPKMethodContainerPool : NSObject

@property (class, nonatomic, strong, readonly, nonnull) SPKMethodContainerPool *sharedPool;

- (void)setObject:(nullable id<SPKMethodContainerProtocol>)object forKeyedSubscript:(nullable NSString *)key;
- (nullable id<SPKMethodContainerProtocol>)objectForKeyedSubscript:(nullable NSString *)key;

@end
