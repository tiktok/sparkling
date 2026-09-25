// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodEventSubscriber.h"
#import "SPKMethodEvent.h"

@interface SPKMethodEventSubscriber ()

@property (nonatomic, copy) SPKMethodEventCallback callback;
@property (nonatomic, assign) NSTimeInterval timestamp;

@end

@implementation SPKMethodEventSubscriber

+ (instancetype)subscriberWithCallback:(SPKMethodEventCallback)callback
{
    return [self subscriberWithCallback:callback timestamp:0];
}

+ (instancetype)subscriberWithCallback:(SPKMethodEventCallback)callback timestamp:(NSTimeInterval)timestamp
{
    if (!callback) {
        return nil;
    }
    return [[self alloc] initWithDeliveryCallback:callback timestamp:timestamp];
}

- (instancetype)initWithDeliveryCallback:(SPKMethodEventCallback)callback timestamp:(NSTimeInterval)timestamp
{
    self = [super init];
    if (self) {
        _callback = [callback copy];
        _timestamp = timestamp ? timestamp / 1000.0 : [[NSDate date] timeIntervalSince1970];
    }
    return self;
}

- (BOOL)receiveEvent:(SPKMethodEvent *)event
{
    if (!self.callback) {
        return NO;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.callback) {
            self.callback(event.eventName, event.params);
        }
    });
    return YES;
}

- (BOOL)isEqual:(SPKMethodEventSubscriber *)object
{
    if (self == object) {
        return YES;
    }
    return object.callback && object.callback == self.callback;
}

- (NSUInteger)hash
{
    return 0;
}

@end
