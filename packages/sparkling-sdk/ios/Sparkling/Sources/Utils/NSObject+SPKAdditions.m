// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "NSObject+SPKAdditions.h"
#import <objc/runtime.h>

@implementation NSObject (SPKAdditions)

#pragma mark - Associated Object

- (const void *)spk_computedKeyFromString:(NSString *)key {
    return (char *)((__bridge void*)self) + [key hash] + [key characterAtIndex:0] + [key characterAtIndex:key.length - 1];
}

- (void)spk_attachObject:(nullable id)obj forKey:(NSString *)key isWeak:(BOOL)bWeak {
    if (key.length <= 0) {
        return ;
    }
    if (bWeak) {
        id __weak weakObject = obj;
        id (^block)(void) = ^{ return weakObject; };
        objc_setAssociatedObject(self,
                                 [self spk_computedKeyFromString:key],
                                 block,
                                 OBJC_ASSOCIATION_COPY);
        return;
    }
    else {
        objc_setAssociatedObject(self,
                                 [self spk_computedKeyFromString:key],
                                 obj,
                                 OBJC_ASSOCIATION_RETAIN);
    }
}

- (nullable id)spk_getAttachedObjectForKey:(NSString *)key isWeak:(BOOL)bWeak {
    if (key.length <= 0) {
        return nil;
    }
    if (bWeak) {
        id (^block)(void) = objc_getAssociatedObject(self,
                                                     [self spk_computedKeyFromString:key]);
        return (block ? block() : nil);
    }
    else {
        return objc_getAssociatedObject(self,
                                        [self spk_computedKeyFromString:key]);
    }
}

@end
