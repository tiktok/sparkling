// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKBootExecutor.h"
#include <mach-o/dyld.h>
#include <dlfcn.h>
#import <mach-o/dyld.h>
#import <mach-o/getsect.h>

#ifndef __LP64__
    typedef struct mach_header SPKCodeRunnerMachoHeader;
#else
    typedef struct mach_header_64 SPKCodeRunnerMachoHeader;
#endif

typedef struct _SPKBootExecuteImage {
    unsigned long size;
    uint64_t *memory;
    SEL selector;
} SPKBootExecuteImage;

static SPKBootExecuteImage* create_image_with_section(const SPKCodeRunnerMachoHeader *mhp, const char *secname, SEL selector)
{
    unsigned long size = 0;
    SPKBootExecuteImage *image = (SPKBootExecuteImage *)malloc(sizeof(SPKBootExecuteImage));
    image->memory = (uint64_t *)getsectiondata((const struct mach_header_64 *)mhp, "__DATA", secname, &size);
    image->size = size;
    image->selector = selector;
    if (image->memory == NULL) {
        free(image);
        return NULL;
    }
    return image;
}

static void execute_method_with_images(SPKBootExecuteImage *image)
{
    if (image->memory != NULL) {
        for (int i = 0; i < image->size / sizeof(void*); i++) {
            const char *str = (const char *)image->memory[i];
            #if __has_feature(address_sanitizer)
            if (str == 0) {
                continue;
            }
            #endif
            NSString *clsName = [@"Sparkling." stringByAppendingString:[NSString stringWithUTF8String:str]];
            if (clsName.length) {
                SEL selector = image->selector;
                Class cls = NSClassFromString(clsName);
                if ([cls respondsToSelector:selector]) {
                    IMP imp = [cls methodForSelector:selector];
                    ((void (*)(id, SEL))imp)(cls, selector);
                }
            }
        }
    }
}

static void execute_method_with_class_name(NSString *className, SEL selector)
{
    Class cls = NSClassFromString(className);
    if ([cls respondsToSelector:selector]) {
        IMP imp = [cls methodForSelector:selector];
        ((void (*)(id, SEL))imp)(cls, selector);
    }
}

static void execute_fallback_prepare_services(void)
{
    SEL selector = @selector(executePrepareServiceTask);
    execute_method_with_class_name(@"Sparkling.SPKLynxService", selector);
    execute_method_with_class_name(@"Sparkling.SPKResourceLoaderImpl", selector);
}

#pragma GCC diagnostic ignored "-Wundeclared-selector"

static void handle_did_add_image(const SPKCodeRunnerMachoHeader *mhp)
{
    SPKBootExecuteImage *prepareServiceImage = create_image_with_section(mhp, SPK_PREPARE_SERVICE_SECTION_NAME, @selector(executePrepareServiceTask));
    BOOL hasPrepareServiceSection = NO;
    if (prepareServiceImage != NULL) {
        hasPrepareServiceSection = YES;
        execute_method_with_images(prepareServiceImage);
        free(prepareServiceImage);
    }
    if (!hasPrepareServiceSection) {
        execute_fallback_prepare_services();
    }
    
    SPKBootExecuteImage *afterAllPrepareImage = create_image_with_section(mhp, SPK_AFTER_ALL_PREPARE_SECTION_NAME, @selector(executeAfterPrepareTask));
    if (afterAllPrepareImage != NULL) {
        execute_method_with_images(afterAllPrepareImage);
        free(afterAllPrepareImage);
    }
}

static void PIARunSegment() __attribute__((no_sanitize("address")))
{
    Dl_info info;
    int ret = dladdr(PIARunSegment, &info);
    if (ret == 0) {
        return;
    }
#ifndef __LP64__
    const struct mach_header *mhp = (struct mach_header*)info.dli_fbase;
#else /* defined(__LP64__) */
    const struct mach_header_64 *mhp = (struct mach_header_64*)info.dli_fbase;
#endif /* defined(__LP64__) */
    
    handle_did_add_image((SPKCodeRunnerMachoHeader *)mhp);
    
}

void SPKExecuteAllPrepareBootTask(void)
{
    static BOOL isExecuted = NO;
    if (!isExecuted) {
        isExecuted = YES;
        PIARunSegment();
    }
}
