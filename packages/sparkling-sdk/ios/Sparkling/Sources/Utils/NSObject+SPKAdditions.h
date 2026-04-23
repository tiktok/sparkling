// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

@interface NSObject (SPKAdditions)

/**
 Set the associated object.
 */

- (void)spk_attachObject:(nullable id)obj forKey:(nullable NSString *)key isWeak:(BOOL)bWeak;
- (nullable id)spk_getAttachedObjectForKey:(nullable NSString *)key isWeak:(BOOL)bWeak;

@end
