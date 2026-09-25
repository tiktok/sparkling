// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodRegistry.h"

#define SPK_METHOD_SEGMENT "__DATA"
#define SPK_METHOD_GLOBAL_SECTION "SPKMethods"
#define SPK_METHOD_SWIFT_SECTION "SPKSwiftMethods"

// The explicit symbol preserves existing host macro declarations.
#define spk_method_export_class_name(className, sectionName, symbol) \
    __attribute__((used, section(SPK_METHOD_SEGMENT "," sectionName))) \
    static char * const symbol = className

#define spk_method_register_global_method(method) \
    spk_method_export_class_name(#method, SPK_METHOD_GLOBAL_SECTION, SPKMethodGlobalMethod_##method)

/// Discovers declarations in loaded images inside the main bundle. No startup hook is installed.
@interface SPKMethodRegistration : NSObject

/// Synchronously registers public declarations on the caller's thread, scanning all bundle images.
/// Keeps existing entries, like registerMethod:. Call once during host setup, before dispatch.
+ (void)registerGlobalMethodsInRegistry:(nonnull SPKMethodRegistry<SPKMethod *> *)registry;

/// With lazily=YES, eligible classes with a nonempty +methodName are stored without construction.
/// Class declarations replace earlier declarations; existing instances keep their priority.
/// Other classes use the same eager registration path as the entry above.
+ (void)registerGlobalMethodsInRegistry:(nonnull SPKMethodRegistry<SPKMethod *> *)registry lazily:(BOOL)lazily;

/// Host integration: scans only the requested section and resolves classes on callbackQueue.
/// A nil queue invokes the handler synchronously. Getter entries return a class-name C string.
/// With scanAllImages=NO, stops after the first bundle image containing a nonempty section.
+ (void)enumerateMethodClassesInSection:(nonnull const char *)section
                  usesClassNameGetters:(BOOL)usesClassNameGetters
                         scanAllImages:(BOOL)scanAllImages
                         callbackQueue:(nullable dispatch_queue_t)callbackQueue
                               handler:(void (^ _Nonnull)(Class _Nonnull methodClass))handler;

@end
