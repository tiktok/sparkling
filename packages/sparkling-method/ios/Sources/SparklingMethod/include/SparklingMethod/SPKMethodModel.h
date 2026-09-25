// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#if __has_include(<Mantle/Mantle.h>)
#import <Mantle/Mantle.h>
#else
#import <Mantle.h>
#endif

@protocol SPKMethodModel <NSObject>

/// Required parameter key paths, checked by the method invocation layer.
+ (nullable NSSet<NSString *> *)requiredKeyPaths;

/// Default parameter values, applied by the method invocation layer.
+ (nullable NSDictionary *)defaultValues;

@end

@interface SPKMethodModel : MTLModel <MTLJSONSerializing, SPKMethodModel>

@property (nonatomic, copy, nullable) id extraInfo;

@end
