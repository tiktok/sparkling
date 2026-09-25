// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodLynxTransport.h"
#import "SPKMethodCallMessage.h"
#import "SPKMethodLynxTransportPool.h"
#import <objc/runtime.h>

static void *SPKMethodLynxTransportAssociationKey = &SPKMethodLynxTransportAssociationKey;

@interface SPKMethodLynxTransport ()

@property (nonatomic, weak, readwrite) LynxView *lynxView;
@property (nonatomic, copy, readwrite) NSString *containerID;

@end

@implementation SPKMethodLynxTransport

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
    if ([SPKMethodLynxTransportPool transportForContainerID:self.containerID] == self) {
        [SPKMethodLynxTransportPool setTransport:nil forContainerID:self.containerID];
    }
}

- (void)setupWithContainer:(id)container
{
    NSParameterAssert([container isKindOfClass:LynxView.class]);
    if (![container isKindOfClass:LynxView.class]) {
        return;
    }
    LynxView *previousView = self.lynxView;
    if (previousView && previousView != container &&
        objc_getAssociatedObject(previousView, SPKMethodLynxTransportAssociationKey) == self) {
        objc_setAssociatedObject(previousView, SPKMethodLynxTransportAssociationKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    self.lynxView = container;
    objc_setAssociatedObject(container,
                             SPKMethodLynxTransportAssociationKey,
                             self,
                             OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    [SPKMethodLynxTransportPool setTransport:self forContainerID:self.containerID];
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
