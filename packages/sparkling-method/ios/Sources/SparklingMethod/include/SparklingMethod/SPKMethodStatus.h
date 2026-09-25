// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

typedef NS_ENUM(NSInteger, SPKMethodStatusCode) {
    // General Errors
    SPKMethodStatusCodeSucceeded = 1,
    SPKMethodStatusCodeFailed = 0,
    SPKMethodStatusCodeUnauthorizedInvocation = -1,
    SPKMethodStatusCodeUnregisteredMethod = -2,
    SPKMethodStatusCodeInvalidParameter = -3,
    SPKMethodStatusCodeInvalidNamespace = -4,
    SPKMethodStatusCodeInvalidResult = -5,
    SPKMethodStatusCodeUnauthorizedAccess = -6,
    SPKMethodStatusCodeOperationCancelled = -7,
    SPKMethodStatusCodeOperationTimeout = -8,
    SPKMethodStatusCodeNotFound = -9,
    SPKMethodStatusCodeNotImplemented = -10,
    SPKMethodStatusCodeAlreadyExists = -11,
    SPKMethodStatusCodeUnknown = -1000,

    // Network Errors
    SPKMethodStatusCodeNetworkUnreachable = -1001,
    SPKMethodStatusCodeNetworkTimeout = -1002,
    SPKMethodStatusCodeMalformedResponse = -1003,

    // Business layer may define their own status code, which should start from 10001.
    // Status code <= 10000 will be reserved for SparklingMethod.
};

@interface SPKMethodStatus : NSObject

@property (nonatomic, assign) SPKMethodStatusCode statusCode;
@property (nonatomic, copy, nullable) NSString *message;

+ (nonnull instancetype)statusWithStatusCode:(SPKMethodStatusCode)statusCode message:(nullable NSString *)message, ... NS_REQUIRES_NIL_TERMINATION;
+ (nonnull instancetype)statusWithStatusCode:(SPKMethodStatusCode)statusCode;

+ (nonnull instancetype)statusWithStatusCode:(SPKMethodStatusCode)statusCode message:(nullable NSString *)message arguments:(va_list)arguments;

+ (nullable NSString *)statusMessageWithStatusCode:(SPKMethodStatusCode)statusCode;

+ (nonnull instancetype)new NS_UNAVAILABLE;
- (nonnull instancetype)init NS_UNAVAILABLE;

@end
