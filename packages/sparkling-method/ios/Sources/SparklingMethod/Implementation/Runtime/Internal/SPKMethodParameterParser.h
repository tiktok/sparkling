// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodModel.h"
#import "SPKMethodStatus.h"

@interface SPKMethodParameterParser : NSObject

/// Merge defaults, check required key paths, and deserialize the parameter model.
/// A nil model class succeeds with a nil model. Status is nil on success.
+ (nullable SPKMethodModel *)parseParams:(nullable NSDictionary *)params
                            modelClass:(nullable Class<SPKMethodModel>)modelClass
                                status:(SPKMethodStatus * _Nullable __autoreleasing * _Nullable)status;

@end
