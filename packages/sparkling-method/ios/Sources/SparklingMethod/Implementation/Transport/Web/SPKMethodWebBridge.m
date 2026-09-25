// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodWebBridge.h"
#import "SPKMethodCallMessage.h"
#import "SPKMethodCallRouter.h"

@implementation SPKMethodWebBridgeConfiguration

- (id)copyWithZone:(NSZone *)zone
{
    SPKMethodWebBridgeConfiguration *copy = [[[self class] allocWithZone:zone] init];
    copy.messageHandlerName = self.messageHandlerName;
    copy.javascriptObjectName = self.javascriptObjectName;
    copy.invokeMethodName = self.invokeMethodName;
    copy.callbackMethodName = self.callbackMethodName;
    copy.protocolVersion = self.protocolVersion;
    copy.messageProtocolIdentifier = self.messageProtocolIdentifier;
    copy.callMessageClass = self.callMessageClass;
    return copy;
}

@end

@interface SPKMethodWeakScriptMessageHandler : NSObject <WKScriptMessageHandler>
@property (nonatomic, weak) id<WKScriptMessageHandler> target;
@end

@implementation SPKMethodWeakScriptMessageHandler

- (void)userContentController:(WKUserContentController *)userContentController
      didReceiveScriptMessage:(WKScriptMessage *)message
{
    [self.target userContentController:userContentController didReceiveScriptMessage:message];
}

@end

@interface SPKMethodWebBridge ()
@property (nonatomic, weak, readwrite) WKWebView *webView;
@property (nonatomic, strong) SPKMethodWeakScriptMessageHandler *weakMessageHandler;
@property (nonatomic, strong) SPKMethodCallRouter *ownedCallRouter;
@end

@implementation SPKMethodWebBridge

- (instancetype)initWithConfiguration:(SPKMethodWebBridgeConfiguration *)configuration
{
    NSParameterAssert(configuration.messageHandlerName.length > 0);
    NSParameterAssert(configuration.javascriptObjectName.length > 0);
    NSParameterAssert(configuration.invokeMethodName.length > 0);
    NSParameterAssert(configuration.callbackMethodName.length > 0);
    self = [super init];
    if (self) {
        _configuration = [configuration copy];
        _weakMessageHandler = [SPKMethodWeakScriptMessageHandler new];
        _weakMessageHandler.target = self;
    }
    return self;
}

- (instancetype)initWithRuntime:(SPKMethodRuntime *)runtime
                  configuration:(SPKMethodWebBridgeConfiguration *)configuration
{
    NSParameterAssert(runtime);
    self = [self initWithConfiguration:configuration];
    if (self) {
        _ownedCallRouter = [[SPKMethodCallRouter alloc] initWithRuntime:runtime];
        self.messageHandler = _ownedCallRouter;
    }
    return self;
}

- (void)dealloc
{
    [self detachFromWebView];
}

- (void)setupWithContainer:(id)container
{
    NSParameterAssert([container isKindOfClass:WKWebView.class]);
    if (![container isKindOfClass:WKWebView.class]) {
        return;
    }
    [self detachFromWebView];
    self.webView = container;

    WKUserContentController *controller = self.webView.configuration.userContentController;
    WKUserScript *script = [self injectionScript];
    BOOL containsScript = NO;
    for (WKUserScript *candidate in controller.userScripts) {
        if ([candidate.source isEqualToString:script.source]) {
            containsScript = YES;
            break;
        }
    }
    if (!containsScript) {
        [controller addUserScript:script];
    }
    [controller removeScriptMessageHandlerForName:self.configuration.messageHandlerName];
    [controller addScriptMessageHandler:self.weakMessageHandler name:self.configuration.messageHandlerName];
}

- (void)detachFromWebView
{
    WKWebView *webView = self.webView;
    if (!webView) {
        return;
    }
    [webView.configuration.userContentController removeScriptMessageHandlerForName:self.configuration.messageHandlerName];
    self.webView = nil;
}

- (WKUserScript *)injectionScript
{
    NSString *objectName = self.configuration.javascriptObjectName;
    NSString *handlerName = self.configuration.messageHandlerName;
    NSString *invokeName = self.configuration.invokeMethodName;
    NSString *source = [NSString stringWithFormat:
        @"(function(){try{window.%@=window.%@||{};window.%@.%@=function(params){if(typeof params==='string'){window.webkit.messageHandlers.%@.postMessage(params);}};}catch(e){}})();",
        objectName, objectName, objectName, invokeName, handlerName];
    return [[WKUserScript alloc] initWithSource:source
                                  injectionTime:WKUserScriptInjectionTimeAtDocumentStart
                               forMainFrameOnly:YES];
}

- (void)userContentController:(WKUserContentController *)userContentController
      didReceiveScriptMessage:(WKScriptMessage *)scriptMessage
{
    SPKMethodCallMessage *message = [self callMessageWithBody:scriptMessage.body
                                                    container:scriptMessage.webView ?: self.webView
                                                    invokeURL:scriptMessage.webView.URL ?: self.webView.URL
                                                      authURL:scriptMessage.frameInfo.request.URL];
    if (!message) {
        return;
    }

    __weak typeof(self) weakSelf = self;
    [self.messageHandler handleCallMessage:message resultHandler:^(NSDictionary *response) {
        [weakSelf deliverResponse:response ?: @{} forMessage:message completionHandler:nil];
    }];
}

- (void)fireEvent:(NSString *)eventName
            params:(NSDictionary *)params
     resultHandler:(void (^)(id result))resultHandler
{
    NSString *script = [self eventJavaScriptWithName:eventName params:params];
    [self evaluateJavaScript:script inWebView:self.webView completionHandler:resultHandler];
}

- (SPKMethodCallMessage *)callMessageWithBody:(id)body
                                     container:(WKWebView *)container
                                     invokeURL:(NSURL *)invokeURL
                                       authURL:(NSURL *)authURL
{
    NSDictionary *rawData = [self dictionaryFromMessageBody:body];
    if (!rawData) {
        return nil;
    }

    Class messageClass = self.configuration.callMessageClass ?: SPKMethodCallMessage.class;
    if (![messageClass isSubclassOfClass:SPKMethodCallMessage.class]) {
        return nil;
    }
    SPKMethodCallMessage *message = [messageClass new];
    message.methodName = rawData[@"func"];
    message.methodNamespace = rawData[@"namespace"];
    message.methodType = rawData[@"__msg_type"];
    message.callbackID = rawData[@"__callback_id"];
    message.iframeURLString = rawData[@"__iframe_url"];
    message.secureToken = rawData[@"_secure_token"];
    message.JSSDKVersion = rawData[@"JSSDK"];
    message.protocolVersion = self.configuration.messageProtocolIdentifier ?: self.configuration.protocolVersion;
    message.sendTimeStamp = rawData[@"__timestamp"];
    message.receivedTimeStamp = @((long long)(NSDate.date.timeIntervalSince1970 * 1000));
    message.requestDecodeDuration = @0;
    message.rawData = rawData;
    message.container = container;
    message.invokeURL = invokeURL;
    message.authURL = authURL;
    message.engineType = SPKMethodEngineTypeWeb;

    id params = rawData[@"params"];
    if (params && ![params isKindOfClass:NSDictionary.class]) {
        NSAssert(NO, @"The params field should be nil or of type NSDictionary.");
    } else {
        message.params = params;
    }
    return message;
}

- (NSDictionary *)dictionaryFromMessageBody:(id)body
{
    if ([body isKindOfClass:NSDictionary.class]) {
        return body;
    }
    if (![body isKindOfClass:NSString.class]) {
        return nil;
    }
    NSData *data = [(NSString *)body dataUsingEncoding:NSUTF8StringEncoding];
    id object = data ? [NSJSONSerialization JSONObjectWithData:data options:0 error:nil] : nil;
    return [object isKindOfClass:NSDictionary.class] ? object : nil;
}

- (void)deliverResponse:(NSDictionary *)response
              forMessage:(SPKMethodCallMessage *)message
       completionHandler:(void (^)(id result))completionHandler
{
    NSString *script = [self callbackJavaScriptWithResponse:response forMessage:message];
    WKWebView *webView = [message.container isKindOfClass:WKWebView.class] ? message.container : self.webView;
    [self evaluateJavaScript:script inWebView:webView completionHandler:completionHandler];
}

- (NSString *)callbackJavaScriptWithResponse:(NSDictionary *)response
                                   forMessage:(SPKMethodCallMessage *)message
{
    NSMutableDictionary *payload = [NSMutableDictionary dictionary];
    payload[@"__msg_type"] = @"callback";
    if (message.callbackID) {
        payload[@"__callback_id"] = message.callbackID;
    }
    payload[@"__params"] = response ?: @{};
    payload[@"__sdk_version"] = self.configuration.protocolVersion ?: @"";
    return [self javaScriptForPayload:payload];
}

- (NSString *)eventJavaScriptWithName:(NSString *)eventName params:(NSDictionary *)params
{
    NSMutableDictionary *payload = [NSMutableDictionary dictionary];
    payload[@"__msg_type"] = @"event";
    payload[@"__event_id"] = eventName;
    NSMutableDictionary *eventParams = [params mutableCopy] ?: [NSMutableDictionary dictionary];
    if (!eventParams[@"code"]) {
        eventParams[@"code"] = @1;
    }
    payload[@"__params"] = eventParams;
    payload[@"__sdk_version"] = self.configuration.protocolVersion ?: @"";
    return [self javaScriptForPayload:payload];
}

- (NSString *)javaScriptForPayload:(NSDictionary *)payload
{
    if (![NSJSONSerialization isValidJSONObject:payload]) {
        return nil;
    }
    NSData *data = [NSJSONSerialization dataWithJSONObject:payload options:0 error:nil];
    NSString *JSON = data ? [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] : nil;
    if (!JSON) {
        return nil;
    }
    NSString *objectName = self.configuration.javascriptObjectName;
    NSString *callbackName = self.configuration.callbackMethodName;
    return [NSString stringWithFormat:@"window.%@&&window.%@.%@&&window.%@.%@(%@)",
                                      objectName, objectName, callbackName, objectName, callbackName, JSON];
}

- (void)evaluateJavaScript:(NSString *)script
                 inWebView:(WKWebView *)webView
         completionHandler:(void (^)(id result))completionHandler
{
    if (!script || !webView) {
        if (completionHandler) {
            completionHandler(nil);
        }
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        [webView evaluateJavaScript:script completionHandler:^(id result, NSError *error) {
            if (completionHandler) {
                completionHandler(result);
            }
        }];
    });
}

@end
