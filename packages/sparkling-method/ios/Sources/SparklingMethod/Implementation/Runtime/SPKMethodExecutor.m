// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodExecutor.h"
#import "Internal/SPKMethodParameterParser.h"
#import "Internal/SPKMethodResultSerializer.h"

@implementation SPKMethodInvocationHooks
@end

static void SPKMethodExecutorAssert(BOOL condition, NSString *message, SPKMethodInvocationHooks *hooks)
{
    if (hooks.assertionHandler) {
        hooks.assertionHandler(condition, message);
    } else {
        NSCAssert(condition, @"%@", message);
    }
}

@implementation SPKMethodExecutor

+ (void)invokeMethod:(SPKMethod *)method
              params:(NSDictionary *)params
          engineType:(SPKMethodEngineType)engineType
               hooks:(SPKMethodInvocationHooks *)hooks
   completionHandler:(SPKMethodInvocationCompletionHandler)completionHandler
{
    __weak SPKMethod *weakMethod = method;
    SPKMethodCompletionHandler wrappedCompletion = ^(SPKMethodModel *resultModel, SPKMethodStatus *status) {
        SPKMethod * __attribute__((objc_precise_lifetime)) method = weakMethod;
        id __attribute__((objc_precise_lifetime)) completionOwner = hooks.completionOwner;
        (void)completionOwner;
        if (hooks.didReceiveCompletion) {
            hooks.didReceiveCompletion();
        }
        SPKMethodStatusCode statusCode = status ? status.statusCode : SPKMethodStatusCodeSucceeded;
        NSString *message = status.message;
        if (hooks.willComplete) {
            hooks.willComplete(resultModel, statusCode, message);
        }

        NSDictionary *result = nil;
        if (resultModel) {
            SPKMethodExecutorAssert([resultModel isKindOfClass:method.resultModelClass],
                                   [NSString stringWithFormat:@"The result model should be kind of class '%@', instead of '%@'.", method.resultModelClass, resultModel.class], hooks);
            NSError *error = nil;
            result = [SPKMethodResultSerializer JSONDictionaryFromModel:resultModel error:&error];
            SPKMethodExecutorAssert(!error, [NSString stringWithFormat:@"Failed to parse result model: %@.", error.localizedDescription], hooks);
            if (hooks.transformResult) {
                result = hooks.transformResult(result);
            }
            if (error && !status) {
                statusCode = SPKMethodStatusCodeInvalidResult;
                message = error.localizedDescription;
            }
        }
        BOOL copyToData = hooks.shouldCopyToData ? hooks.shouldCopyToData() :
            engineType == SPKMethodEngineTypeWeb && method.forceCopyToData;
        result = [SPKMethodResultSerializer resultDictionaryWithJSONDictionary:result
                                                                   extraInfo:resultModel.extraInfo
                                                               statusMessage:status.message
                                                                  copyToData:copyToData];
        if (hooks.willDeliver) {
            hooks.willDeliver(statusCode, message, resultModel);
        }
        if (completionHandler) {
            completionHandler(statusCode, result, message);
        }
        if (hooks.didDeliver) {
            hooks.didDeliver(statusCode, message, result);
        }
    };

    if (hooks.shouldInvoke && !hooks.shouldInvoke(params)) {
        wrappedCompletion(nil, [SPKMethodStatus statusWithStatusCode:SPKMethodStatusCodeOperationCancelled
                                                           message:@"rejected by interceptor", nil]);
        return;
    }
    SPKMethodStatus *parameterStatus = nil;
    SPKMethodModel *paramModel = [SPKMethodParameterParser parseParams:params modelClass:method.paramModelClass status:&parameterStatus];
    if (parameterStatus) {
        wrappedCompletion(nil, parameterStatus);
        return;
    }
    if (hooks.willInvoke) {
        hooks.willInvoke(params, paramModel);
    }
    [method invokeWithParamModel:paramModel completionHandler:wrappedCompletion];
    if (hooks.didInvoke) {
        hooks.didInvoke(paramModel);
    }
}

@end
