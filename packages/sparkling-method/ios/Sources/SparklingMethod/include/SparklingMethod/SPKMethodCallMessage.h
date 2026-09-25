// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>
#import "SPKMethodDefinitions.h"

/// Transport-neutral representation of one JavaScript-to-native method call.
/// Concrete Web and Lynx integrations populate this object before handing it to
/// a SPKMethodCallMessageHandler.
@interface SPKMethodCallMessage : NSObject

@property (nonatomic, copy, nullable) NSString *methodName;
@property (nonatomic, copy, nullable) NSString *methodNamespace;
@property (nonatomic, copy, nullable) NSDictionary *params;
@property (nonatomic, copy, nullable) NSString *callbackID;
@property (nonatomic, copy, nullable) NSString *protocolVersion;
@property (nonatomic, strong, nullable) NSURL *invokeURL;
@property (nonatomic, strong, nullable) NSDictionary *rawData;
@property (nonatomic, weak, nullable) id container;
@property (nonatomic, assign) SPKMethodEngineType engineType;

@end
