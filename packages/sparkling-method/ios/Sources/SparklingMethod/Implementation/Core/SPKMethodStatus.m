// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodStatus.h"

@implementation SPKMethodStatus

+ (instancetype)statusWithStatusCode:(SPKMethodStatusCode)statusCode message:(NSString *)message, ...
{
    va_list arguments;
    va_start(arguments, message);
    SPKMethodStatus *status = [self statusWithStatusCode:statusCode message:message arguments:arguments];
    va_end(arguments);
    return status;
}

+ (instancetype)statusWithStatusCode:(SPKMethodStatusCode)statusCode message:(NSString *)message arguments:(va_list)arguments
{
    if (message) {
        message = [[NSString alloc] initWithFormat:message arguments:arguments];
    }

    SPKMethodStatus *status = [(id)[self alloc] init];
    status.statusCode = statusCode;
    status.message = [message copy];
    return status;
}

+ (instancetype)statusWithStatusCode:(SPKMethodStatusCode)statusCode
{
    return [self statusWithStatusCode:statusCode message:nil];
}

- (NSString *)description
{
    return [NSString stringWithFormat:@"<%@: %p, %@>", self.class, self, @{
        @"statusCode": @(self.statusCode),
        @"message": self.message ?: @"",
    }];
}

+ (NSString *)statusMessageWithStatusCode:(SPKMethodStatusCode)statusCode
{
    switch (statusCode) {
        case SPKMethodStatusCodeSucceeded: return @"JSB_SUCCESS";
        case SPKMethodStatusCodeFailed: return @"JSB_FAILED";
        case SPKMethodStatusCodeInvalidParameter: return @"JSB_PARAM_ERROR";
        case SPKMethodStatusCodeUnregisteredMethod: return @"The JSBridge method is not found, please register";
        case SPKMethodStatusCodeUnauthorizedInvocation: return @"The URL is not authorized to call this JSBridge method";
        case SPKMethodStatusCodeInvalidNamespace: return @"JSB_NAMESPACE_ERROR";
        default: return @"JSB_UNKNOWN_ERROR";
    }
}

@end
