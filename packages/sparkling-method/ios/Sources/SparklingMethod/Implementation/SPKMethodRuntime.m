// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodRuntime.h"
#import "SPKMethodDispatcher.h"
#import "SPKMethodExecutor.h"
#import "SPKMethodRegistration.h"

@interface SPKMethodRuntime ()

@property (nonatomic, strong) SPKMethodDispatcher *dispatcher;

@end

static SPKMethodRegistry *SPKMethodDefaultGlobalRegistry(void)
{
    static SPKMethodRegistry *registry;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        registry = [SPKMethodRegistry new];
    });
    return registry;
}

@implementation SPKMethodRuntime

- (instancetype)init
{
    return [self initWithLocalRegistry:[SPKMethodRegistry new]
                       globalRegistry:SPKMethodDefaultGlobalRegistry()
                          eventCenter:SPKMethodEventCenter.sharedCenter];
}

- (instancetype)initWithLocalRegistry:(SPKMethodRegistry *)localRegistry
                      globalRegistry:(SPKMethodRegistry *)globalRegistry
                         eventCenter:(SPKMethodEventCenter *)eventCenter
{
    self = [super init];
    if (self) {
        _localRegistry = localRegistry;
        _globalRegistry = globalRegistry;
        _eventCenter = eventCenter;
        _dispatcher = [[SPKMethodDispatcher alloc] initWithLocalRegistry:localRegistry globalRegistry:globalRegistry];
    }
    return self;
}

- (BOOL)registerLocalMethod:(SPKMethod *)method
{
    return [self.localRegistry registerMethod:method];
}

- (BOOL)registerGlobalMethod:(SPKMethod *)method
{
    return [self.globalRegistry registerMethod:method];
}

- (void)registerLocalMethod:(SPKMethod *)method forName:(NSString *)name
{
    [self.localRegistry registerMethod:method forName:name];
}

- (void)registerGlobalMethod:(SPKMethod *)method forName:(NSString *)name
{
    [self.globalRegistry registerMethod:method forName:name];
}

- (void)deregisterLocalMethodNamed:(NSString *)name
{
    [self.localRegistry deregisterMethodNamed:name];
}

- (void)deregisterGlobalMethodNamed:(NSString *)name
{
    [self.globalRegistry deregisterMethodNamed:name];
}

- (void)registerDeclaredGlobalMethodsLazily:(BOOL)lazily
{
    [SPKMethodRegistration registerGlobalMethodsInRegistry:self.globalRegistry lazily:lazily];
}

- (void)invokeMethodNamed:(NSString *)methodName
                  params:(NSDictionary *)params
              engineType:(SPKMethodEngineType)engineType
                   hooks:(SPKMethodInvocationHooks *)hooks
       completionHandler:(SPKMethodInvocationCompletionHandler)completionHandler
{
    [self.dispatcher invokeMethodNamed:methodName params:params engineType:engineType hooks:hooks completionHandler:completionHandler];
}

+ (void)invokeMethod:(SPKMethod *)method
              params:(NSDictionary *)params
          engineType:(SPKMethodEngineType)engineType
               hooks:(SPKMethodInvocationHooks *)hooks
   completionHandler:(SPKMethodInvocationCompletionHandler)completionHandler
{
    [SPKMethodExecutor invokeMethod:method params:params engineType:engineType hooks:hooks completionHandler:completionHandler];
}

- (void)subscribeEventNamed:(NSString *)eventName withSubscriber:(SPKMethodEventSubscriber *)subscriber
{
    [self.eventCenter subscribeEventNamed:eventName withSubscriber:subscriber];
}

- (void)unsubscribeEventNamed:(NSString *)eventName withSubscriber:(SPKMethodEventSubscriber *)subscriber
{
    [self.eventCenter unsubscribeEventNamed:eventName withSubscriber:subscriber];
}

- (void)publishEvent:(SPKMethodEvent *)event
{
    [self.eventCenter publishEvent:event];
}

@end
