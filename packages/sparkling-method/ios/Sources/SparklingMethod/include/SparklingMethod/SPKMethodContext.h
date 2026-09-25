// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

@interface SPKMethodContext : NSObject <NSCopying>

- (void)setWeakObject:(nullable id)object forKey:(nullable NSString *)key;

- (void)setStrongObject:(nullable id)object forKey:(nullable NSString *)key;

- (void)removeWeakObjectForKey:(nullable NSString *)key;

- (void)removeStrongObjectForKey:(nullable NSString *)key;

- (nullable id)objectForKeyedSubscript:(nullable NSString *)key;

@end
