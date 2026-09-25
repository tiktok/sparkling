// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodContext.h"
#import "SPKMethodDefinitions.h"
#import "SPKMethodModel.h"
#import "SPKMethodStatus.h"

FOUNDATION_EXTERN NSString * _Nonnull const SPKMethodMetaInfoVersionKey;
FOUNDATION_EXTERN NSString * _Nonnull const SPKMethodMetaInfoUIDKey;

typedef void (^SPKMethodCompletionHandler)(SPKMethodModel * _Nullable resultModel, SPKMethodStatus * _Nullable status);
typedef void (^SPKMethodCallHandler)(__kindof SPKMethodModel * _Nullable paramModel, SPKMethodCompletionHandler _Nullable completion);

@interface SPKMethod : NSObject

@property (nonatomic, assign, readonly) SPKMethodEngineType supportedEngineTypes;
@property (nonatomic, copy, readonly, nullable) NSString *methodName;
@property (nonatomic, assign, readonly) BOOL isDevelopmentMethod;
@property (nonatomic, strong, nullable) SPKMethodContext *context;
@property (nonatomic, strong, readonly, nullable) Class<SPKMethodModel> paramModelClass;
@property (nonatomic, strong, readonly, nullable) Class resultModelClass;
@property (nonatomic, assign, readonly) BOOL forceCopyToData;

- (nonnull instancetype)initWithContext:(nullable SPKMethodContext *)context;
- (nonnull instancetype)initWithHandler:(nullable SPKMethodCallHandler)handler;

/// Invoke a parsed model. Subclasses may override this entry or provide a handler.
/// Passing nil status to the completion indicates success.
- (void)invokeWithParamModel:(nullable SPKMethodModel *)paramModel completionHandler:(nullable SPKMethodCompletionHandler)completionHandler;

/// Merge declared defaults and parse using Mantle; required-key checks belong to dispatch.
- (nullable SPKMethodModel *)paramsModelInstanceWithParams:(nullable NSDictionary *)params;

+ (BOOL)canUseLazyRegistration;
+ (nullable NSString *)methodName;

/// Override to specialize the lazily created context in compatibility subclasses.
+ (nonnull Class)contextClass;

/// Abstract-method failure hook for host assertion integrations.
- (void)raiseExceptionWithSelector:(nonnull SEL)selector;

@end
