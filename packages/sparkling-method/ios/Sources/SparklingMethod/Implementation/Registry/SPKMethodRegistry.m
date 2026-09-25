// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodRegistry.h"
#import <pthread.h>

@interface SPKMethodRegistry ()

@property (nonatomic, strong) NSMutableDictionary<NSString *, SPKMethod *> *innerMethods;
@property (nonatomic, strong) NSMutableDictionary<NSString *, Class> *methodClasses;

@end

@implementation SPKMethodRegistry {
    pthread_rwlock_t _registryLock;
}

- (instancetype)init
{
    return [self initWithStorage:[NSMutableDictionary dictionary]];
}

- (instancetype)initWithStorage:(NSMutableDictionary<NSString *, SPKMethod *> *)storage
{
    self = [super init];
    if (self) {
        pthread_rwlock_init(&_registryLock, NULL);
        _innerMethods = storage;
        _methodClasses = [NSMutableDictionary dictionary];
    }
    return self;
}

- (instancetype)initWithMethodClassStorage:(NSMutableDictionary<NSString *, Class> *)storage
{
    self = [self initWithStorage:[NSMutableDictionary dictionary]];
    if (self) {
        _methodClasses = storage;
    }
    return self;
}

- (void)dealloc
{
    pthread_rwlock_destroy(&_registryLock);
}

- (NSDictionary<NSString *, SPKMethod *> *)methods
{
    pthread_rwlock_rdlock(&_registryLock);
    NSDictionary<NSString *, SPKMethod *> *methods = [_innerMethods copy];
    pthread_rwlock_unlock(&_registryLock);
    return methods;
}

- (BOOL)registerMethod:(SPKMethod *)method
{
    NSString *name = method.methodName;
    pthread_rwlock_wrlock(&_registryLock);
    if ([_innerMethods objectForKey:name]) {
        pthread_rwlock_unlock(&_registryLock);
        return NO;
    }
    [_innerMethods setObject:method forKey:name];
    pthread_rwlock_unlock(&_registryLock);
    return YES;
}

- (SPKMethod *)methodForName:(NSString *)name
{
    while (YES) {
        pthread_rwlock_rdlock(&_registryLock);
        SPKMethod *method = [_innerMethods objectForKey:name];
        Class methodClass = method ? Nil : [_methodClasses objectForKey:name];
        pthread_rwlock_unlock(&_registryLock);
        if (method || !methodClass) {
            return method;
        }

        SPKMethod *createdMethod = [methodClass new];
        if (!createdMethod) {
            return nil;
        }

        pthread_rwlock_wrlock(&_registryLock);
        method = [_innerMethods objectForKey:name];
        Class currentMethodClass = [_methodClasses objectForKey:name];
        if (!method && currentMethodClass == methodClass) {
            _innerMethods[name] = createdMethod;
            method = createdMethod;
        }
        BOOL shouldRetry = !method && currentMethodClass != methodClass;
        pthread_rwlock_unlock(&_registryLock);
        if (method || !shouldRetry) {
            return method;
        }
    }
}

- (void)registerMethodClass:(Class)methodClass forName:(NSString *)name
{
    pthread_rwlock_wrlock(&_registryLock);
    _methodClasses[name] = methodClass;
    pthread_rwlock_unlock(&_registryLock);
}

- (Class)methodClassForName:(NSString *)name
{
    pthread_rwlock_rdlock(&_registryLock);
    Class methodClass = [_methodClasses objectForKey:name];
    pthread_rwlock_unlock(&_registryLock);
    return methodClass;
}

- (void)registerMethod:(SPKMethod *)method forName:(NSString *)name
{
    pthread_rwlock_wrlock(&_registryLock);
    _innerMethods[name] = method;
    pthread_rwlock_unlock(&_registryLock);
}

- (void)deregisterMethodNamed:(NSString *)name
{
    pthread_rwlock_wrlock(&_registryLock);
    [_innerMethods removeObjectForKey:name];
    pthread_rwlock_unlock(&_registryLock);
}

+ (NSDictionary<NSString *, SPKMethod *> *)methodsByMergingGlobalMethods:(NSDictionary<NSString *, SPKMethod *> *)globalMethods
                                                         localMethods:(NSDictionary<NSString *, SPKMethod *> *)localMethods
                                                            predicate:(BOOL (^)(SPKMethod *method))predicate
{
    NSMutableDictionary<NSString *, SPKMethod *> *methods = [NSMutableDictionary dictionary];
    void (^enumerateBlock)(NSString *, SPKMethod *, BOOL *) = ^(NSString *key, SPKMethod *method, BOOL *stop) {
        if (!predicate || predicate(method)) {
            methods[key] = method;
        }
    };
    [globalMethods enumerateKeysAndObjectsUsingBlock:enumerateBlock];
    [localMethods enumerateKeysAndObjectsUsingBlock:enumerateBlock];
    return [methods copy];
}

@end
