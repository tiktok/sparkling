// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

@class SPKMethodEvent;

typedef void(^SPKMethodEventCallback)(NSString * _Nonnull eventName, NSDictionary * _Nullable params);

@interface SPKMethodEventSubscriber : NSObject

@property (nonatomic, copy, nullable) NSString *eventName;
/// Replay cutoff in seconds since the Unix epoch.
@property (nonatomic, assign, readonly) NSTimeInterval timestamp;

+ (nonnull instancetype)subscriberWithCallback:(nonnull SPKMethodEventCallback)callback;
/// Input timestamp is in milliseconds. Zero uses the current time; use 1 to replay all unexpired events.
+ (nonnull instancetype)subscriberWithCallback:(nonnull SPKMethodEventCallback)callback timestamp:(NSTimeInterval)timestamp;

/// Subclasses may pass a nil callback and override receiveEvent: to deliver to a host engine.
/// Input timestamp follows the same millisecond/zero convention as the factory.
- (nonnull instancetype)initWithDeliveryCallback:(nullable SPKMethodEventCallback)callback timestamp:(NSTimeInterval)timestamp;

/// Enqueues the callback on the main queue. Returns NO when delivery is unavailable.
/// EventCenter removes unavailable subscribers during publication, but not during replay.
- (BOOL)receiveEvent:(nullable SPKMethodEvent *)event;

@end
