// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodModel.h"

@interface SPKMethodResultSerializer : NSObject

/// Serialize a result model with Mantle. A nil model returns nil without an error.
+ (nullable NSDictionary *)JSONDictionaryFromModel:(nullable SPKMethodModel *)model
                                            error:(NSError * _Nullable __autoreleasing * _Nullable)error;

/// Apply data copying, extraInfo overrides, and the status message in that order.
+ (nonnull NSDictionary *)resultDictionaryWithJSONDictionary:(nullable NSDictionary *)dictionary
                                                  extraInfo:(nullable id)extraInfo
                                              statusMessage:(nullable NSString *)statusMessage
                                                 copyToData:(BOOL)copyToData;

@end
