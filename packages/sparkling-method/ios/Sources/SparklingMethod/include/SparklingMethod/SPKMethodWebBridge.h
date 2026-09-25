// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <WebKit/WebKit.h>
#import "SPKMethodTransport.h"

@class SPKMethodCallMessage;
@class SPKMethodRuntime;

/// Names used by the JavaScript wire protocol. Hosts choose the names so an
/// existing protocol can migrate without changing its JavaScript contract.
@interface SPKMethodWebBridgeConfiguration : NSObject <NSCopying>

@property (nonatomic, copy, nonnull) NSString *messageHandlerName;
@property (nonatomic, copy, nonnull) NSString *javascriptObjectName;
@property (nonatomic, copy, nonnull) NSString *invokeMethodName;
@property (nonatomic, copy, nonnull) NSString *callbackMethodName;
@property (nonatomic, copy, nonnull) NSString *protocolVersion;
/// Optional transport identifier used by compatibility layers. When omitted,
/// protocolVersion is stored on the call message.
@property (nonatomic, copy, nullable) NSString *messageProtocolIdentifier;
/// SPKMethodCallMessage subclass created for each invocation.
@property (nonatomic, assign, nullable) Class callMessageClass;

@end

/// WebKit transport for SPKMethodCallMessage. Hybrid WebView subclasses can be
/// passed directly because the implementation only requires WKWebView APIs.
@interface SPKMethodWebBridge : NSObject <SPKMethodTransport, WKScriptMessageHandler>

@property (nonatomic, weak, nullable) id<SPKMethodCallMessageHandler> messageHandler;
@property (nonatomic, weak, readonly, nullable) WKWebView *webView;
@property (nonatomic, copy, readonly, nonnull) SPKMethodWebBridgeConfiguration *configuration;

- (nonnull instancetype)initWithConfiguration:(nonnull SPKMethodWebBridgeConfiguration *)configuration;
/// Standalone Web entry. Retains its CallRouter and Runtime for the attachment lifetime.
- (nonnull instancetype)initWithRuntime:(nonnull SPKMethodRuntime *)runtime
                         configuration:(nonnull SPKMethodWebBridgeConfiguration *)configuration;
- (nonnull instancetype)init NS_UNAVAILABLE;

/// The script installed into each attached WebView.
- (nonnull WKUserScript *)injectionScript;

/// Converts a host message into the transport-neutral message model. Hosts
/// with their own WebView interception layer can use this entry without
/// installing an additional WKScriptMessageHandler.
- (nullable SPKMethodCallMessage *)callMessageWithBody:(nullable id)body
                                              container:(nullable WKWebView *)container
                                              invokeURL:(nullable NSURL *)invokeURL
                                                authURL:(nullable NSURL *)authURL;

/// Builds JavaScript using the same wire format as the directly attached
/// transport. Compatibility adapters can evaluate it with their own lifecycle
/// and monitoring hooks.
- (nullable NSString *)callbackJavaScriptWithResponse:(nullable NSDictionary *)response
                                            forMessage:(nonnull SPKMethodCallMessage *)message;
- (nullable NSString *)eventJavaScriptWithName:(nonnull NSString *)eventName
                                         params:(nullable NSDictionary *)params;

@end
