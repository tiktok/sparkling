// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodDispatcher.h"
#import "SPKMethodExecutor.h"

@interface SPKMethodDispatcher ()

@property (nonatomic, strong) SPKMethodRegistry *localRegistry;
@property (nonatomic, strong) SPKMethodRegistry *globalRegistry;

@end

static SPKMethodDispatchHandler SPKMethodHandler(SPKMethod *method, SPKMethodEngineType engineType, SPKMethodInvocationHooks *hooks)
{
    if (!method || !(method.supportedEngineTypes & engineType)) {
        return nil;
    }
    return ^(NSDictionary *params, SPKMethodInvocationCompletionHandler completionHandler) {
        [SPKMethodExecutor invokeMethod:method params:params engineType:engineType hooks:hooks completionHandler:completionHandler];
    };
}

@implementation SPKMethodDispatcher

- (instancetype)init
{
    return [self initWithLocalRegistry:nil globalRegistry:nil];
}

- (instancetype)initWithLocalRegistry:(SPKMethodRegistry *)localRegistry globalRegistry:(SPKMethodRegistry *)globalRegistry
{
    self = [super init];
    if (self) {
        _localRegistry = localRegistry;
        _globalRegistry = globalRegistry;
    }
    return self;
}

- (void)invokeMethodNamed:(NSString *)methodName
                  params:(NSDictionary *)params
              engineType:(SPKMethodEngineType)engineType
                   hooks:(SPKMethodInvocationHooks *)hooks
       completionHandler:(SPKMethodInvocationCompletionHandler)completionHandler
{
    BOOL handled = [self.class dispatchMethodNamed:methodName
        localResolver:^SPKMethodDispatchHandler(NSString *name) {
            return SPKMethodHandler(name ? [self.localRegistry methodForName:name] : nil, engineType, hooks);
        }
        globalResolver:^SPKMethodDispatchHandler(NSString *name) {
            return SPKMethodHandler(name ? [self.globalRegistry methodForName:name] : nil, engineType, hooks);
        }
        invocation:^(SPKMethodDispatchHandler handler) {
            handler(params, completionHandler);
        }];
    if (!handled && completionHandler) {
        completionHandler(SPKMethodStatusCodeUnregisteredMethod, nil,
                          [SPKMethodStatus statusMessageWithStatusCode:SPKMethodStatusCodeUnregisteredMethod]);
    }
}

+ (BOOL)dispatchMethodNamed:(NSString *)methodName
             localResolver:(SPKMethodDispatchResolver)localResolver
            globalResolver:(SPKMethodDispatchResolver)globalResolver
                invocation:(void (^)(SPKMethodDispatchHandler handler))invocation
{
    SPKMethodDispatchHandler handler = localResolver ? localResolver(methodName) : nil;
    if (!handler && globalResolver) {
        handler = globalResolver(methodName);
    }
    if (!handler) {
        return NO;
    }
    invocation(handler);
    return YES;
}

@end
