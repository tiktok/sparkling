// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodRegistration.h"
#include <mach-o/getsect.h>
#include <mach-o/dyld.h>

#if __LP64__ || NS_BUILD_32_LIKE_64
typedef struct mach_header_64 spk_method_mach_header;
#else
typedef struct mach_header spk_method_mach_header;
#endif

typedef const char *(*SPKMethodClassNameGetter)(void);

@implementation SPKMethodRegistration

+ (void)registerGlobalMethodsInRegistry:(SPKMethodRegistry<SPKMethod *> *)registry
{
    [self registerGlobalMethodsInRegistry:registry lazily:NO];
}

+ (void)registerGlobalMethodsInRegistry:(SPKMethodRegistry<SPKMethod *> *)registry lazily:(BOOL)lazily
{
    void (^registerClass)(Class) = ^(Class methodClass) {
        if ([methodClass isSubclassOfClass:SPKMethod.class]) {
            if (lazily && [methodClass canUseLazyRegistration]) {
                NSString *name = [methodClass methodName];
                if (name.length > 0) {
                    [registry registerMethodClass:methodClass forName:name];
                    return;
                }
            }
            SPKMethod *method = [methodClass new];
            if (method.methodName.length > 0) {
                [registry registerMethod:method];
            }
        }
    };
    [self enumerateMethodClassesInSection:SPK_METHOD_GLOBAL_SECTION
                    usesClassNameGetters:NO
                           scanAllImages:YES
                           callbackQueue:nil
                                 handler:registerClass];
    [self enumerateMethodClassesInSection:SPK_METHOD_SWIFT_SECTION
                    usesClassNameGetters:YES
                           scanAllImages:YES
                           callbackQueue:nil
                                 handler:registerClass];
}

+ (void)enumerateMethodClassesInSection:(const char *)section
                  usesClassNameGetters:(BOOL)usesClassNameGetters
                         scanAllImages:(BOOL)scanAllImages
                         callbackQueue:(dispatch_queue_t)callbackQueue
                               handler:(void (^)(Class methodClass))handler __attribute__((no_sanitize("address")))
{
    uint32_t imageCount = _dyld_image_count();
    NSString *mainBundlePath = NSBundle.mainBundle.bundlePath;
    for (uint32_t index = 0; index < imageCount; index++) {
        const spk_method_mach_header *header = (const spk_method_mach_header *)_dyld_get_image_header(index);
        NSString *imageName = [NSString stringWithUTF8String:_dyld_get_image_name(index)];
        if (![imageName containsString:mainBundlePath]) {
            continue;
        }
        unsigned long size = 0;
        const char **entries = (const char **)getsectiondata(header, SPK_METHOD_SEGMENT, section, &size);
        unsigned long count = size / sizeof(*entries);
        if (count == 0) {
            continue;
        }
        for (unsigned long entryIndex = 0; entryIndex < count; entryIndex++) {
            const char *name = entries[entryIndex];
            if (usesClassNameGetters) {
                SPKMethodClassNameGetter getter = (SPKMethodClassNameGetter)entries[entryIndex];
                name = getter ? getter() : NULL;
            }
            if (!name) {
                continue;
            }
            NSString *className = [NSString stringWithUTF8String:name];
            if (className.length == 0) {
                continue;
            }
            dispatch_block_t resolveClass = ^{
                Class methodClass = NSClassFromString(className);
                if (!methodClass && [className containsString:@"."]) {
                    methodClass = NSClassFromString(className.pathExtension);
                }
                if (methodClass) {
                    handler(methodClass);
                }
            };
            if (callbackQueue) {
                dispatch_async(callbackQueue, resolveClass);
            } else {
                resolveClass();
            }
        }
        if (!scanAllImages) {
            break;
        }
    }
}

@end
