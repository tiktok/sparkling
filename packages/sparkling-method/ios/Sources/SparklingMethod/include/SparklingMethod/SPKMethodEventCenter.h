// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

@class SPKMethodEvent;
@class SPKMethodEventSubscriber;

@interface SPKMethodEventCenter : NSObject

@property (class, nonatomic, strong, readonly, nonnull) SPKMethodEventCenter *sharedCenter;

/// The events that aren't in the effective duration  will be remove out of the event-queue.
/// The default effective duration is 5min.
@property (nonatomic, assign) NSTimeInterval effectiveDuration;

/// Subscribe specified event.
/// @param eventName The event named `eventName` will be fed to the subscriber.
/// @param subscriber The subscriber to receive events.
- (void)subscribeEventNamed:(nonnull NSString *)eventName withSubscriber:(nonnull SPKMethodEventSubscriber *)subscriber;

/// Unsubscribe specified event.
/// @param eventName The event named `eventName` will stop being fed to the subscriber, pass `nil` to unsubscribe all events.
/// @param subscriber The subscriber to stop receiving events.
- (void)unsubscribeEventNamed:(nullable NSString *)eventName withSubscriber:(nonnull SPKMethodEventSubscriber *)subscriber;

/// Publish event to subscribers.
/// @param event An event object that contains all event details.
- (void)publishEvent:(nonnull SPKMethodEvent *)event;

/// detect contains subscriber for event name
- (BOOL)containsSubscriberForEventName:(nonnull NSString *)eventName;

@end
