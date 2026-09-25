// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethod.h"

NSString * const SPKMethodMetaInfoVersionKey = @"IDLVersion";
NSString * const SPKMethodMetaInfoUIDKey = @"UID";

static NSString * const SPKMethodCallHandlerKey = @"SPKMethodCallHandlerKey";

@implementation SPKMethod

- (instancetype)initWithContext:(SPKMethodContext *)context
{
    self = [super init];
    if (self) {
        _context = context;
    }
    return self;
}

- (instancetype)initWithHandler:(SPKMethodCallHandler)callHandler
{
    self = [super init];
    if (self) {
        [self.context setStrongObject:[callHandler copy] forKey:SPKMethodCallHandlerKey];
    }
    return self;
}

- (SPKMethodEngineType)supportedEngineTypes
{
    return SPKMethodEngineTypeAll;
}

- (NSString *)methodName
{
    [self raiseExceptionWithSelector:_cmd];
    return nil;
}

- (BOOL)isDevelopmentMethod
{
    return NO;
}

- (SPKMethodContext *)context
{
    if (!_context) {
        _context = [[[self.class contextClass] alloc] init];
    }
    return _context;
}

- (Class)paramModelClass
{
    return nil;
}

- (nullable SPKMethodModel *)paramsModelInstanceWithParams:(NSDictionary *_Nullable)params {
    SPKMethodModel *paramModel = nil;
    if (self.paramModelClass) {
        NSMutableDictionary *mergedParams = [params mutableCopy];

        // merge default values
        NSDictionary *defaultValues = [self.paramModelClass defaultValues];

        [defaultValues enumerateKeysAndObjectsUsingBlock:^(id  _Nonnull key, id  _Nonnull obj, BOOL * _Nonnull stop) {
            if (!mergedParams[key]) {
                [mergedParams setValue:obj forKey:key];
            }
        }];

        NSError *error = nil;
        paramModel = [MTLJSONAdapter modelOfClass:self.paramModelClass fromJSONDictionary:mergedParams error:&error];
    }
    return paramModel;
}

- (Class)resultModelClass
{
    return nil;
}

- (void)invokeWithParamModel:(SPKMethodModel *)paramModel completionHandler:(SPKMethodCompletionHandler)completionHandler
{
    SPKMethodCallHandler handler = self.context[SPKMethodCallHandlerKey];

    if (handler) {
        handler(paramModel, completionHandler);
        return;
    }

    [self raiseExceptionWithSelector:_cmd];
}

- (void)raiseExceptionWithSelector:(SEL)selector
{
    NSAssert(NO, @"The method '%@' should be implemented by subclass '%@'.", NSStringFromSelector(selector), NSStringFromClass(self.class));
}

+ (Class)contextClass
{
    return SPKMethodContext.class;
}

+ (BOOL)canUseLazyRegistration
{
    return self.methodName != nil;
}

+ (NSString *)methodName
{
    return nil;
}
@end
