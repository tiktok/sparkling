// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodLynxTransportPool.h"
#import "SPKMethodLynxTransport.h"

@implementation SPKMethodLynxTransportPool

+ (NSMapTable<NSString *, SPKMethodLynxTransport *> *)transportMap
{
    static NSMapTable<NSString *, SPKMethodLynxTransport *> *transportMap;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        transportMap = [NSMapTable strongToWeakObjectsMapTable];
    });
    return transportMap;
}

+ (SPKMethodLynxTransport *)transportForContainerID:(NSString *)containerID
{
    if (containerID.length == 0) {
        return nil;
    }
    @synchronized(self.transportMap) {
        return [self.transportMap objectForKey:containerID];
    }
}

+ (void)setTransport:(SPKMethodLynxTransport *)transport forContainerID:(NSString *)containerID
{
    if (containerID.length == 0) {
        return;
    }
    @synchronized(self.transportMap) {
        if (transport) {
            [self.transportMap setObject:transport forKey:containerID];
        } else {
            [self.transportMap removeObjectForKey:containerID];
        }
    }
}

@end
