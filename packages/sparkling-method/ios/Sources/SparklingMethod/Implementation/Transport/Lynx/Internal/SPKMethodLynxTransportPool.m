// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodLynxTransportPool.h"
#import "SPKMethodLynxTransport.h"

@implementation SPKMethodLynxTransportPool

+ (NSMutableDictionary<NSString *, NSMapTable<NSString *, SPKMethodLynxTransport *> *> *)transportMaps
{
    static NSMutableDictionary<NSString *, NSMapTable<NSString *, SPKMethodLynxTransport *> *> *maps;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        maps = [NSMutableDictionary dictionary];
    });
    return maps;
}

+ (SPKMethodLynxTransport *)transportForContainerID:(NSString *)containerID moduleName:(NSString *)moduleName
{
    if (containerID.length == 0 || moduleName.length == 0) {
        return nil;
    }
    @synchronized(self.transportMaps) {
        return [self.transportMaps[moduleName] objectForKey:containerID];
    }
}

+ (void)setTransport:(SPKMethodLynxTransport *)transport forContainerID:(NSString *)containerID moduleName:(NSString *)moduleName
{
    if (containerID.length == 0 || moduleName.length == 0) {
        return;
    }
    @synchronized(self.transportMaps) {
        NSMapTable<NSString *, SPKMethodLynxTransport *> *map = self.transportMaps[moduleName];
        if (transport) {
            if (!map) {
                map = [NSMapTable strongToWeakObjectsMapTable];
                self.transportMaps[moduleName] = map;
            }
            [map setObject:transport forKey:containerID];
        } else {
            [map removeObjectForKey:containerID];
        }
    }
}

@end
