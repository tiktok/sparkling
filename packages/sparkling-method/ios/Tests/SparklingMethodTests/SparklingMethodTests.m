// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

@import SparklingMethod;
@import XCTest;

@interface SPKPublicResultModel : SPKMethodModel
@property (nonatomic, copy) NSString *value;
@end

@implementation SPKPublicResultModel
+ (NSDictionary *)JSONKeyPathsByPropertyKey
{
    return @{
        @"value": @"value",
    };
}
@end

@interface SPKPublicParamModel : SPKMethodModel
@property (nonatomic, copy) NSString *value;
@property (nonatomic, copy) NSString *defaultedValue;
@end

@implementation SPKPublicParamModel
+ (NSDictionary *)JSONKeyPathsByPropertyKey
{
    return @{
        @"value": @"payload.value",
        @"defaultedValue": @"defaultedValue",
    };
}

+ (NSSet<NSString *> *)requiredKeyPaths
{
    return [NSSet setWithObject:@"payload.value"];
}

+ (NSDictionary *)defaultValues
{
    return @{
        @"defaultedValue": @"fallback",
    };
}
@end

@interface SPKPublicEchoMethod : SPKMethod
@end

@implementation SPKPublicEchoMethod
- (NSString *)methodName
{
    return @"public.echo";
}

- (Class)resultModelClass
{
    return SPKPublicResultModel.class;
}

- (Class)paramModelClass
{
    return SPKPublicParamModel.class;
}

- (void)invokeWithParamModel:(SPKMethodModel *)paramModel completionHandler:(SPKMethodCompletionHandler)completionHandler
{
    SPKPublicParamModel *params = (SPKPublicParamModel *)paramModel;
    SPKPublicResultModel *result = [SPKPublicResultModel new];
    result.value = [NSString stringWithFormat:@"%@:%@", params.value, params.defaultedValue];
    completionHandler(result, [SPKMethodStatus statusWithStatusCode:SPKMethodStatusCodeSucceeded]);
}
@end

@interface SPKPublicWebCallMessage : SPKMethodCallMessage
@end

@implementation SPKPublicWebCallMessage
@end

@interface SparklingMethodTests : XCTestCase
@end

@implementation SparklingMethodTests

- (void)testRegistryKeepsFirstRegistrationAndSupportsExplicitReplacement
{
    SPKMethodRegistry *registry = [SPKMethodRegistry new];
    SPKPublicEchoMethod *first = [SPKPublicEchoMethod new];
    SPKPublicEchoMethod *second = [SPKPublicEchoMethod new];

    XCTAssertTrue([registry registerMethod:first]);
    XCTAssertFalse([registry registerMethod:second]);
    XCTAssertEqual([registry methodForName:@"public.echo"], first);

    [registry registerMethod:second forName:@"public.echo"];
    XCTAssertEqual([registry methodForName:@"public.echo"], second);
}

- (void)testRuntimeInvokesARegisteredMethod
{
    SPKMethodRegistry *local = [SPKMethodRegistry new];
    SPKMethodRegistry *global = [SPKMethodRegistry new];
    SPKMethodRuntime *runtime = [[SPKMethodRuntime alloc] initWithLocalRegistry:local
                                                                globalRegistry:global
                                                                   eventCenter:[SPKMethodEventCenter new]];
    XCTAssertTrue([runtime registerLocalMethod:[SPKPublicEchoMethod new]]);

    XCTestExpectation *completion = [self expectationWithDescription:@"runtime completion"];
    [runtime invokeMethodNamed:@"public.echo"
                       params:@{ @"payload": @{ @"value": @"ok" } }
                   engineType:SPKMethodEngineTypeWeb
                        hooks:nil
            completionHandler:^(SPKMethodStatusCode statusCode, NSDictionary *result, NSString *message) {
        XCTAssertEqual(statusCode, SPKMethodStatusCodeSucceeded);
        XCTAssertEqualObjects(result[@"value"], @"ok:fallback");
        [completion fulfill];
    }];
    [self waitForExpectations:@[completion] timeout:1];
}

- (void)testRuntimeRejectsMissingRequiredNestedParameter
{
    SPKMethodRuntime *runtime = [SPKMethodRuntime new];
    XCTAssertTrue([runtime registerLocalMethod:[SPKPublicEchoMethod new]]);

    XCTestExpectation *completion = [self expectationWithDescription:@"invalid parameter completion"];
    [runtime invokeMethodNamed:@"public.echo"
                       params:@{}
                   engineType:SPKMethodEngineTypeLynx
                        hooks:nil
            completionHandler:^(SPKMethodStatusCode statusCode, NSDictionary *result, NSString *message) {
        XCTAssertEqual(statusCode, SPKMethodStatusCodeInvalidParameter);
        XCTAssertNil(result[@"value"]);
        [completion fulfill];
    }];
    [self waitForExpectations:@[completion] timeout:1];
}

- (void)testEventCenterPublishesToCallbackSubscriber
{
    SPKMethodEventCenter *center = [SPKMethodEventCenter new];
    XCTestExpectation *delivery = [self expectationWithDescription:@"event delivery"];
    SPKMethodEventSubscriber *subscriber = [SPKMethodEventSubscriber subscriberWithCallback:^(NSString *eventName, NSDictionary *params) {
        XCTAssertEqualObjects(eventName, @"public.event");
        XCTAssertEqualObjects(params[@"value"], @1);
        [delivery fulfill];
    }];

    [center subscribeEventNamed:@"public.event" withSubscriber:subscriber];
    [center publishEvent:[SPKMethodEvent eventWithEventName:@"public.event" params:@{ @"value": @1 }]];
    [self waitForExpectations:@[delivery] timeout:1];
}

- (void)testCallRouterInvokesRuntimeAndBuildsTransportResponse
{
    SPKMethodRuntime *runtime = [SPKMethodRuntime new];
    XCTAssertTrue([runtime registerLocalMethod:[SPKPublicEchoMethod new]]);
    SPKMethodCallRouter *router = [[SPKMethodCallRouter alloc] initWithRuntime:runtime];

    SPKMethodCallMessage *message = [SPKMethodCallMessage new];
    message.methodName = @"public.echo";
    message.params = @{ @"payload": @{ @"value": @"router" } };
    message.engineType = SPKMethodEngineTypeWeb;

    XCTestExpectation *completion = [self expectationWithDescription:@"router completion"];
    [router handleCallMessage:message resultHandler:^(NSDictionary *response) {
        XCTAssertEqualObjects(response[@"code"], @(SPKMethodStatusCodeSucceeded));
        XCTAssertEqualObjects(response[@"msg"], @"");
        XCTAssertEqualObjects(response[@"data"][@"value"], @"router:fallback");
        [completion fulfill];
    }];
    [self waitForExpectations:@[completion] timeout:1];
}

- (void)testCallRouterReturnsUnregisteredResponse
{
    SPKMethodCallRouter *router = [[SPKMethodCallRouter alloc] initWithRuntime:[SPKMethodRuntime new]];
    SPKMethodCallMessage *message = [SPKMethodCallMessage new];
    message.methodName = @"missing.method";
    message.engineType = SPKMethodEngineTypeLynx;

    XCTestExpectation *completion = [self expectationWithDescription:@"missing completion"];
    [router handleCallMessage:message resultHandler:^(NSDictionary *response) {
        XCTAssertEqualObjects(response[@"code"], @(SPKMethodStatusCodeUnregisteredMethod));
        XCTAssertEqualObjects(response[@"data"], @{});
        [completion fulfill];
    }];
    [self waitForExpectations:@[completion] timeout:1];
}

- (void)testWebBridgeOwnsStandaloneCallRouter
{
    SPKMethodWebBridgeConfiguration *configuration = [SPKMethodWebBridgeConfiguration new];
    configuration.messageHandlerName = @"HostBridge";
    configuration.javascriptObjectName = @"HostBridge";
    configuration.invokeMethodName = @"invoke";
    configuration.callbackMethodName = @"callback";
    configuration.protocolVersion = @"4.0";

    SPKMethodRuntime *runtime = [SPKMethodRuntime new];
    XCTAssertTrue([runtime registerLocalMethod:[SPKPublicEchoMethod new]]);
    SPKMethodWebBridge *bridge = [[SPKMethodWebBridge alloc] initWithRuntime:runtime configuration:configuration];
    SPKMethodCallMessage *message = [bridge callMessageWithBody:@{ @"func" : @"public.echo",
                                                                   @"params" : @{ @"payload" : @{ @"value" : @"web" } } }
                                                       container:nil invokeURL:nil];
    XCTAssertNotNil(bridge.messageHandler);
    XCTestExpectation *completion = [self expectationWithDescription:@"standalone web entry"];
    [bridge.messageHandler handleCallMessage:message resultHandler:^(NSDictionary *response) {
        XCTAssertEqualObjects(response[@"code"], @(SPKMethodStatusCodeSucceeded));
        XCTAssertEqualObjects(response[@"data"][@"value"], @"web:fallback");
        [completion fulfill];
    }];
    [self waitForExpectations:@[completion] timeout:1];
}

- (void)testWebBridgeUsesHostProvidedProtocolNames
{
    SPKMethodWebBridgeConfiguration *configuration = [SPKMethodWebBridgeConfiguration new];
    configuration.messageHandlerName = @"HostBridge";
    configuration.javascriptObjectName = @"HostBridge";
    configuration.invokeMethodName = @"invoke";
    configuration.callbackMethodName = @"callback";
    configuration.protocolVersion = @"1.0";

    SPKMethodWebBridge *bridge = [[SPKMethodWebBridge alloc] initWithConfiguration:configuration];
    NSString *source = bridge.injectionScript.source;
    XCTAssertTrue([source containsString:@"window.HostBridge.invoke"]);
    XCTAssertTrue([source containsString:@"messageHandlers.HostBridge"]);
}

- (void)testWebBridgeBuildsHostMessageAndWireResponses
{
    SPKMethodWebBridgeConfiguration *configuration = [SPKMethodWebBridgeConfiguration new];
    configuration.messageHandlerName = @"HostBridge";
    configuration.javascriptObjectName = @"HostBridge";
    configuration.invokeMethodName = @"invoke";
    configuration.callbackMethodName = @"callback";
    configuration.protocolVersion = @"4.0";
    configuration.callMessageClass = SPKPublicWebCallMessage.class;

    SPKMethodWebBridge *bridge = [[SPKMethodWebBridge alloc] initWithConfiguration:configuration];
    NSDictionary *body = @{
        @"func" : @"public.echo",
        @"namespace" : @"public",
        @"params" : @{ @"value" : @1 },
        @"__callback_id" : @7,
    };
    SPKMethodCallMessage *message = [bridge callMessageWithBody:body
                                                      container:nil
                                                      invokeURL:[NSURL URLWithString:@"https://example.com/page"]];
    XCTAssertEqualObjects(message.methodName, @"public.echo");
    XCTAssertEqualObjects(message.methodNamespace, @"public");
    XCTAssertEqualObjects(message.params, (@{ @"value" : @1 }));
    XCTAssertTrue([message isKindOfClass:SPKPublicWebCallMessage.class]);
    XCTAssertEqualObjects(message.protocolVersion, @"4.0");
    XCTAssertEqual(message.engineType, SPKMethodEngineTypeWeb);

    NSString *callback = [bridge callbackJavaScriptWithResponse:@{ @"code" : @1 } forMessage:message];
    XCTAssertTrue([callback containsString:@"window.HostBridge.callback"]);
    XCTAssertTrue([callback containsString:@"\"__sdk_version\":\"4.0\""]);
    XCTAssertTrue([callback containsString:@"\"__callback_id\":7"]);

    NSString *event = [bridge eventJavaScriptWithName:@"public.event" params:nil];
    XCTAssertTrue([event containsString:@"\"__event_id\":\"public.event\""]);
    XCTAssertTrue([event containsString:@"\"code\":1"]);
}

@end
