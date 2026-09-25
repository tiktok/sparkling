// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

@class SPKMethodCallMessage;

typedef void (^SPKMethodResponseBlock)(NSDictionary * _Nullable response);

/// Receives transport messages and returns wire-format response dictionaries.
@protocol SPKMethodCallMessageHandler <NSObject>

- (void)handleCallMessage:(nonnull SPKMethodCallMessage *)message
            resultHandler:(nonnull SPKMethodResponseBlock)resultHandler;

@end

/// Container integration boundary implemented by Web and Lynx transports.
@protocol SPKMethodTransport <NSObject>

@property (nonatomic, weak, nullable) id<SPKMethodCallMessageHandler> messageHandler;

- (void)setupWithContainer:(nonnull id)container;

@optional
- (void)fireEvent:(nonnull NSString *)eventName
            params:(nullable NSDictionary *)params
     resultHandler:(nullable void (^)(id _Nullable result))resultHandler;

@end
