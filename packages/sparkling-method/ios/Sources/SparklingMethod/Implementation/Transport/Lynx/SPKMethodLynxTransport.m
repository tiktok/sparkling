// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodLynxTransport.h"
#import <SparklingMethod/SPKMethodCallMessage.h>
#import <SparklingMethod/SPKMethodLynxModule.h>
#import "SPKMethodLynxTransportPool.h"
#import <objc/runtime.h>

static void *SPKMethodLynxTransportAssociationKey = &SPKMethodLynxTransportAssociationKey;

@interface SPKMethodLynxTransport ()

@property (nonatomic, weak, readwrite) LynxView *lynxView;
@property (nonatomic, copy, readwrite) NSString *containerID;

@end

@implementation SPKMethodLynxTransport

+ (NSString *)moduleName
{
    return SPKMethodLynxModule.name;
}

- (instancetype)initWithLynxView:(LynxView *)lynxView containerID:(NSString *)containerID
{
    NSParameterAssert(lynxView);
    NSParameterAssert(containerID.length > 0);
    self = [super init];
    if (self) {
        _containerID = [containerID copy];
        [self setupWithContainer:lynxView];
    }
    return self;
}

- (void)dealloc
{
    if ([SPKMethodLynxTransportPool transportForContainerID:self.containerID moduleName:self.class.moduleName] == self) {
        [SPKMethodLynxTransportPool setTransport:nil forContainerID:self.containerID moduleName:self.class.moduleName];
    }
}

- (void)setupWithContainer:(id)container
{
    NSParameterAssert([container isKindOfClass:LynxView.class]);
    if (![container isKindOfClass:LynxView.class]) {
        return;
    }
    NSString *moduleName = self.class.moduleName;
    NSParameterAssert(moduleName.length > 0);
    LynxView *previousView = self.lynxView;
    NSMutableDictionary<NSString *, SPKMethodLynxTransport *> *previousTransports =
        objc_getAssociatedObject(previousView, SPKMethodLynxTransportAssociationKey);
    if (previousView && previousView != container && previousTransports[moduleName] == self) {
        [previousTransports removeObjectForKey:moduleName];
    }
    self.lynxView = container;
    NSMutableDictionary<NSString *, SPKMethodLynxTransport *> *transports =
        objc_getAssociatedObject(container, SPKMethodLynxTransportAssociationKey);
    if (!transports) {
        transports = [NSMutableDictionary dictionary];
        objc_setAssociatedObject(container,
                                 SPKMethodLynxTransportAssociationKey,
                                 transports,
                                 OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    transports[moduleName] = self;
    [SPKMethodLynxTransportPool setTransport:self forContainerID:self.containerID moduleName:moduleName];
}

- (void)handleCallMessage:(SPKMethodCallMessage *)message resultHandler:(SPKMethodResponseBlock)resultHandler
{
    LynxView *lynxView = self.lynxView;
    message.container = lynxView;
    if (!message.invokeURL) {
        message.invokeURL = [NSURL URLWithString:lynxView.url ?: @""];
    }
    message.engineType = SPKMethodEngineTypeLynx;
    [self.messageHandler handleCallMessage:message resultHandler:resultHandler];
}

- (void)fireEvent:(NSString *)eventName
            params:(NSDictionary *)params
     resultHandler:(void (^)(id result))resultHandler
{
    [self.lynxView sendGlobalEvent:eventName withParams:@[params ?: @{}]];
    if (resultHandler) {
        resultHandler(nil);
    }
}

@end
