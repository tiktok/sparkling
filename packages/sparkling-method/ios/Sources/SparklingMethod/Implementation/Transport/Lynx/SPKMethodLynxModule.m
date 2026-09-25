// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodLynxModule.h"
#import "SPKMethodCallMessage.h"
#import "SPKMethodLynxTransport.h"
#import "SPKMethodLynxTransportPool.h"

@interface SPKMethodLynxModule ()

@property (nonatomic, copy) NSString *containerID;

@end

@implementation SPKMethodLynxModule

+ (NSDictionary<NSString *, NSString *> *)methodLookup
{
    return @{@"call" : NSStringFromSelector(@selector(call:params:callback:))};
}

+ (NSString *)name
{
    return @"spkPipe";
}

+ (Class)callMessageClass
{
    return SPKMethodCallMessage.class;
}

- (instancetype)initWithParam:(NSDictionary *)param
{
    self = [super init];
    if (self) {
        _containerID = [param[@"containerID"] copy];
    }
    return self;
}

- (void)call:(NSString *)name params:(NSDictionary *)params callback:(LynxCallbackBlock)callback
{
    [self handleMethodNamed:name params:params container:nil callback:callback];
}

- (void)handleMethodNamed:(NSString *)name
                   params:(NSDictionary *)params
                container:(LynxView *)container
                 callback:(LynxCallbackBlock)callback
{
    Class messageClass = [[self class] callMessageClass];
    NSAssert([messageClass isSubclassOfClass:SPKMethodCallMessage.class] || messageClass == SPKMethodCallMessage.class,
             @"callMessageClass must be SPKMethodCallMessage or a subclass.");
    SPKMethodCallMessage *message = [messageClass new];
    message.methodName = name;
    message.params = params[@"data"];
    message.methodNamespace = params[@"namespace"];
    message.rawData = params;
    message.container = container;
    message.engineType = SPKMethodEngineTypeLynx;

    id<SPKMethodCallMessageHandler> handler = [self messageHandlerForContainerID:self.containerID];
    if (!handler) {
        return;
    }
    [handler handleCallMessage:message resultHandler:^(NSDictionary *response) {
        if (callback) {
            callback(response);
        }
    }];
}

- (id<SPKMethodCallMessageHandler>)messageHandlerForContainerID:(NSString *)containerID
{
    return [SPKMethodLynxTransportPool transportForContainerID:containerID];
}

@end
