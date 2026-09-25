// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodModel.h"

@implementation SPKMethodModel

+ (MTLPropertyStorage)storageBehaviorForPropertyWithKey:(NSString *)propertyKey
{
    // Xcode 16 adds an optional-protocol marker to NSObject.debugDescription.
    // It is readonly NSObject metadata and is never part of a method model.
    if ([propertyKey isEqualToString:@"debugDescription"]) {
        return MTLPropertyStorageNone;
    }
    return [super storageBehaviorForPropertyWithKey:propertyKey];
}

+ (NSDictionary *)JSONKeyPathsByPropertyKey
{
    return nil;
}

+ (NSSet<NSString *> *)requiredKeyPaths
{
    return nil;
}

+ (NSDictionary *)defaultValues
{
    return nil;
}

@end
