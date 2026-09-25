// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

/// Shared configuration understood by every Sparkling method container.
/// Host-specific engine, authorization, and setup policy belongs in subclasses.
@interface SPKMethodContainerConfiguration : NSObject

@property (nonatomic, copy, nullable) NSString *containerID;

@end
