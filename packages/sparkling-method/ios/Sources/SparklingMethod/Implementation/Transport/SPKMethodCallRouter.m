// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodCallRouter.h"
#import "SPKMethodRuntime.h"

@implementation SPKMethodCallRouter

- (instancetype)initWithRuntime:(SPKMethodRuntime *)runtime
{
    NSParameterAssert(runtime);
    self = [super init];
    if (self) {
        _runtime = runtime;
    }
    return self;
}

- (void)handleCallMessage:(SPKMethodCallMessage *)message resultHandler:(SPKMethodResponseBlock)resultHandler
{
    SPKMethodHostCallHandler hostCallHandler = self.hostCallHandler;
    if (hostCallHandler) {
        hostCallHandler(message, resultHandler);
        return;
    }
    SPKMethodInvocationHooks *hooks = self.hooksProvider ? self.hooksProvider(message) : nil;
    [self.runtime invokeMethodNamed:message.methodName
                            params:message.params
                        engineType:message.engineType
                             hooks:hooks
                 completionHandler:^(SPKMethodStatusCode statusCode, NSDictionary *result, NSString *statusMessage) {
        if (!resultHandler) {
            return;
        }
        resultHandler(@{
            @"code" : @(statusCode),
            @"msg" : statusMessage ?: @"",
            @"data" : result ?: @{},
        });
    }];
}

@end
