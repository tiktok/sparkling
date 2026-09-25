// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodResultSerializer.h"

@implementation SPKMethodResultSerializer

+ (NSDictionary *)JSONDictionaryFromModel:(SPKMethodModel *)model error:(NSError * __autoreleasing *)error
{
    if (error) {
        *error = nil;
    }
    if (!model) {
        return nil;
    }
    return [MTLJSONAdapter JSONDictionaryFromModel:model error:error];
}

+ (NSDictionary *)resultDictionaryWithJSONDictionary:(NSDictionary *)dictionary
                                          extraInfo:(id)extraInfo
                                      statusMessage:(NSString *)statusMessage
                                         copyToData:(BOOL)copyToData
{
    NSMutableDictionary *result = [NSMutableDictionary dictionaryWithDictionary:dictionary];
    if (copyToData) {
        result[@"data"] = result.copy;
    }
    if ([extraInfo isKindOfClass:NSDictionary.class]) {
        [result addEntriesFromDictionary:extraInfo];
    } else if ([extraInfo isKindOfClass:NSString.class]) {
        result[@"data"] = extraInfo;
    }
    result[@"__status_message__"] = statusMessage ?: @"";
    return result.copy;
}

@end
