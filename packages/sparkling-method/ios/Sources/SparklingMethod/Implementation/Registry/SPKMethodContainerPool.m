// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodContainerPool.h"

@interface SPKMethodContainerPool ()

@property (nonatomic, copy) NSMapTable<NSString *, id<SPKMethodContainerProtocol>> *containers;

@end

@implementation SPKMethodContainerPool

+ (instancetype)sharedPool
{
    static SPKMethodContainerPool *pool = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        pool = [SPKMethodContainerPool new];
    });
    return pool;
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        _containers = [NSMapTable strongToWeakObjectsMapTable];
    }
    return self;
}

- (void)setObject:(id<SPKMethodContainerProtocol>)object forKeyedSubscript:(NSString *)key
{
    [self.containers setObject:object forKey:key];
}

- (id<SPKMethodContainerProtocol>)objectForKeyedSubscript:(NSString *)key
{
    return [self.containers objectForKey:key];
}

@end
