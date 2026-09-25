// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

/// Minimal identity contract shared by method runtimes and host containers.
/// Engine access, event transport, and lifecycle are supplied by the host.
@protocol SPKMethodContainerProtocol <NSObject>

@property (nonatomic, copy, readonly, nullable) NSString *spk_containerID;

@end
