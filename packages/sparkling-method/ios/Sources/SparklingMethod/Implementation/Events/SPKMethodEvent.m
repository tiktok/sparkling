// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodEvent.h"

@interface SPKMethodEvent ()

@property (nonatomic, copy) NSString *eventName;
@property (nonatomic, copy) NSDictionary *params;

@end

@implementation SPKMethodEvent

+ (instancetype)eventWithEventName:(NSString *)eventName params:(NSDictionary *)params
{
    return [[self alloc] initWithEventName:eventName params:params];
}

- (instancetype)initWithEventName:(NSString *)eventName params:(NSDictionary *)params
{
    self = [super init];
    if (self) {
        _isSticky = YES;
        _eventName = [eventName copy];
        _params = [params copy];
        _timestamp = [[NSDate date] timeIntervalSince1970];
    }
    return self;
}

- (NSString *)description
{
    return [NSString stringWithFormat:@"<%@: %p, %@>", self.class, self, @{
        @"eventName": self.eventName,
        @"params": self.params ?: @{},
        @"timestamp": @(self.timestamp),
    }];
}

@end
