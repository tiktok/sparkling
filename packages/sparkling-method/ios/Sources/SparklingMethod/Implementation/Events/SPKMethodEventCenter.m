// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodEventCenter.h"
#import "SPKMethodEvent.h"
#import "SPKMethodEventSubscriber.h"

static const NSTimeInterval SPKMethodEventDefaultEffectiveDuration = 5 * 60;

@interface SPKMethodEventCenter ()

@property (nonatomic, strong) NSMutableDictionary<NSString *, NSMutableArray<SPKMethodEventSubscriber *> *> *eventSubscribers;
@property (nonatomic, strong) NSLock *eventSubscribersLock;
@property (nonatomic, strong) NSMutableArray<SPKMethodEvent *> *eventQueue;
@property (nonatomic, strong) NSLock *eventQueueLock;

@end

@implementation SPKMethodEventCenter

+ (instancetype)sharedCenter
{
    static SPKMethodEventCenter *eventCenter = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        eventCenter = [[SPKMethodEventCenter alloc] init];
    });
    return eventCenter;
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        _eventSubscribers = [NSMutableDictionary dictionary];
        _eventSubscribersLock = [NSLock new];
        _eventQueue = [NSMutableArray array];
        _eventQueueLock = [NSLock new];
        _effectiveDuration = SPKMethodEventDefaultEffectiveDuration;
    }
    return self;
}

- (void)subscribeEventNamed:(NSString *)eventName withSubscriber:(SPKMethodEventSubscriber *)subscriber
{
    if (!subscriber || !eventName) {
        return;
    }

    [self.eventSubscribersLock lock];
    NSMutableArray<SPKMethodEventSubscriber *> *subscribers = self.eventSubscribers[eventName];
    if (!subscribers) {
        subscribers = [NSMutableArray array];
        self.eventSubscribers[eventName] = subscribers;
    } else if ([subscribers containsObject:subscriber]) {
        [self.eventSubscribersLock unlock];
        return;
    }
    [subscribers addObject:subscriber];
    [self.eventSubscribersLock unlock];

    [self.eventQueueLock lock];

    [self cleanExpiredEvent];

    [self.eventQueue enumerateObjectsUsingBlock:^(SPKMethodEvent *obj, NSUInteger idx, BOOL *stop) {
        if ([obj.eventName isEqualToString:eventName] && obj.timestamp >= subscriber.timestamp) {
            [subscriber receiveEvent:obj];
        }
    }];

    [self.eventQueueLock unlock];
}

- (void)unsubscribeEventNamed:(NSString *)eventName withSubscriber:(SPKMethodEventSubscriber *)subscriber
{
    if (!subscriber) {
        return;
    }

    [self.eventSubscribersLock lock];
    if (eventName) {
        NSMutableArray<SPKMethodEventSubscriber *> *subscribers = self.eventSubscribers[eventName];
        [subscribers removeObject:subscriber];
    } else {
        [self.eventSubscribers.allValues enumerateObjectsUsingBlock:^(NSMutableArray<SPKMethodEventSubscriber *> *obj, NSUInteger idx, BOOL *stop) {
            [obj removeObject:subscriber];
        }];
    }
    [self.eventSubscribersLock unlock];
}

- (void)cleanExpiredEvent {
    NSTimeInterval now = [[NSDate date] timeIntervalSince1970];
    NSMutableIndexSet *expiredIndexSets = [NSMutableIndexSet indexSet];
    [self.eventQueue enumerateObjectsUsingBlock:^(SPKMethodEvent *obj, NSUInteger idx, BOOL *stop) {
        if (now - obj.timestamp > self.effectiveDuration) {
            [expiredIndexSets addIndex:idx];
        } else {
            *stop = YES;
        }
    }];
    [self.eventQueue removeObjectsAtIndexes:[expiredIndexSets copy]];
}

- (void)publishEvent:(SPKMethodEvent *)event
{
    if (!event.eventName) {
        return;
    }

    // Remove events older than the effective duration before enqueuing a new one.
    [self.eventQueueLock lock];

    [self cleanExpiredEvent];
    if (event.isSticky) {
        [self.eventQueue addObject:event];
    }

    [self.eventQueueLock unlock];

    // Send event to its subscribers.
    [self.eventSubscribersLock lock];
    NSMutableArray<SPKMethodEventSubscriber *> *subscribers = self.eventSubscribers[event.eventName];
    if (subscribers) {
        NSMutableIndexSet *invalidIndexSet = [NSMutableIndexSet indexSet];
        [subscribers enumerateObjectsUsingBlock:^(SPKMethodEventSubscriber *obj, NSUInteger idx, BOOL *stop) {
            BOOL succeeded = [obj receiveEvent:event];
            if (!succeeded) {
                [invalidIndexSet addIndex:idx];
            }
        }];
        if (invalidIndexSet.count > 0) {
            [subscribers removeObjectsAtIndexes:invalidIndexSet];
        }
    }
    [self.eventSubscribersLock unlock];
}

- (BOOL)containsSubscriberForEventName:(NSString *)eventName
{
    NSArray<SPKMethodEventSubscriber *> *subscribers = self.eventSubscribers[eventName];
    return subscribers.count != 0;
}

@end
