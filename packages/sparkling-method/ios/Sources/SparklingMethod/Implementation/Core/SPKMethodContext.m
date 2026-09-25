// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodContext.h"


@interface SPKMethodContext ()

@property (nonatomic, copy) NSMapTable<NSString *, id> *weakObjects;
@property (nonatomic, copy) NSMapTable<NSString *, id> *strongObjects;

@end

@implementation SPKMethodContext

- (instancetype)init
{
    self = [super init];
    if (self) {
        _weakObjects = [NSMapTable strongToWeakObjectsMapTable];
        _strongObjects = [NSMapTable strongToStrongObjectsMapTable];
    }
    return self;
}

- (id)copyWithZone:(NSZone *)zone
{
    SPKMethodContext *copy = [[self.class allocWithZone:zone] init];
    if (copy) {
        copy.weakObjects = [self.weakObjects copy];
        copy.strongObjects = [self.strongObjects copy];
    }
    return copy;
}

- (void)setWeakObject:(id)object forKey:(NSString *)key
{
    [self.weakObjects setObject:object forKey:key];
}

- (void)setStrongObject:(id)object forKey:(NSString *)key
{
    [self.strongObjects setObject:object forKey:key];
}

- (id)objectForKeyedSubscript:(NSString *)key
{
    __block id object = nil;
    [[self mapTables] enumerateObjectsUsingBlock:^(NSMapTable *table, NSUInteger idx, BOOL *stop) {
        object = [table objectForKey:key];
        if (object) {
            *stop = YES;
            return;
        }
    }];
    return object;
}

- (void)removeWeakObjectForKey:(nullable NSString *)key
{
    [self.weakObjects removeObjectForKey:key];
}

- (void)removeStrongObjectForKey:(nullable NSString *)key
{
    [self.strongObjects removeObjectForKey:key];
}

- (NSArray<NSMapTable *> *)mapTables
{
    // Return an order-sensitive array.
    return @[self.weakObjects, self.strongObjects];
}

@end
