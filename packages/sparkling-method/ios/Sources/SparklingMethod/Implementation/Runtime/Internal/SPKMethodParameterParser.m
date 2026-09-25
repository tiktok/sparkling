// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodParameterParser.h"

@implementation SPKMethodParameterParser

+ (SPKMethodModel *)parseParams:(NSDictionary *)params
                    modelClass:(Class<SPKMethodModel>)modelClass
                        status:(SPKMethodStatus * __autoreleasing *)status
{
    if (status) {
        *status = nil;
    }
    if (!modelClass) {
        return nil;
    }

    NSMutableDictionary *mergedParams = [params mutableCopy];
    NSDictionary *defaultValues = [modelClass defaultValues];
    [defaultValues enumerateKeysAndObjectsUsingBlock:^(id key, id value, BOOL *stop) {
        if (!mergedParams[key]) {
            [mergedParams setValue:value forKey:key];
        }
    }];

    for (NSString *keyPath in [modelClass requiredKeyPaths]) {
        if (![mergedParams valueForKeyPath:keyPath]) {
            if (status) {
                *status = [SPKMethodStatus statusWithStatusCode:SPKMethodStatusCodeInvalidParameter
                                                      message:@"The %@ key is required.", keyPath, nil];
            }
            return nil;
        }
    }

    NSError *error = nil;
    SPKMethodModel *model = [MTLJSONAdapter modelOfClass:modelClass fromJSONDictionary:mergedParams error:&error];
    if (error) {
        if (status) {
            *status = [SPKMethodStatus statusWithStatusCode:SPKMethodStatusCodeInvalidParameter];
            (*status).message = error.localizedDescription;
        }
        return nil;
    }
    return model;
}

@end
