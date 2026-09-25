// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

@interface SPKMethodEvent : NSObject

@property (nonatomic, copy, readonly, nonnull) NSString *eventName;
@property (nonatomic, copy, readonly, nullable) NSDictionary *params;
// default is true, if set to false, the event won't add to queue.
@property (nonatomic, assign) BOOL isSticky;

@property (nonatomic, assign) NSTimeInterval timestamp;

+ (nonnull instancetype)eventWithEventName:(nonnull NSString *)eventName params:(nullable NSDictionary *)params;

- (nonnull instancetype)initWithEventName:(nonnull NSString *)eventName params:(nullable NSDictionary *)params;

- (nonnull instancetype)init NS_UNAVAILABLE;

@end
