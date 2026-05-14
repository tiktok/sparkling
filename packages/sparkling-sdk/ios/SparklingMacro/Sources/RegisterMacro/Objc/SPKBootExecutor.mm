// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKBootExecutor.h"
#include <dlfcn.h>
#include <mach-o/dyld.h>
#import <mach-o/dyld.h>
#import <mach-o/getsect.h>

#ifndef __LP64__
typedef struct mach_header SPKCodeRunnerMachoHeader;
#else
typedef struct mach_header_64 SPKCodeRunnerMachoHeader;
#endif

typedef const char *(*SPKInitializer)(void);

typedef struct _SPKPluginData {
  int32_t version;
  SPKInitializer initializer;
} SPKPluginData;

typedef struct _SPKBootExecuteImage {
  unsigned long size;
  uint64_t *memory;
  SEL selector;
} SPKBootExecuteImage;

static SPKBootExecuteImage *create_image_with_section(const SPKCodeRunnerMachoHeader *mhp,
                                                      const char *secname, SEL selector) {
  unsigned long size = 0;
  SPKBootExecuteImage *image =
      static_cast<SPKBootExecuteImage *>(malloc(sizeof(SPKBootExecuteImage)));
  image->memory = reinterpret_cast<uint64_t *>(getsectiondata(
      reinterpret_cast<const struct mach_header_64 *>(mhp), "__DATA", secname, &size));
  image->size = size;
  image->selector = selector;
  if (image->memory == NULL) {
    free(image);
    return NULL;
  }
  return image;
}

static void execute_method_with_images(SPKBootExecuteImage *image) {
  if (image == NULL || image->memory == NULL || image->size == 0) {
    return;
  }
  const SPKPluginData *plugins = reinterpret_cast<const SPKPluginData *>(image->memory);
  NSUInteger count = image->size / sizeof(SPKPluginData);
  for (NSUInteger i = 0; i < count; i++) {
    SPKPluginData plugin = plugins[i];
    if (plugin.initializer == NULL) {
      continue;
    }
    const char *serviceName = plugin.initializer();
    if (serviceName == NULL) {
      continue;
    }
    NSString *clsName =
        [@"Sparkling." stringByAppendingString:[NSString stringWithUTF8String:serviceName]];
    if (clsName.length) {
      SEL selector = image->selector;
      Class cls = NSClassFromString(clsName);
      if ([cls respondsToSelector:selector]) {
        IMP imp = [cls methodForSelector:selector];
        reinterpret_cast<void (*)(id, SEL)>(imp)(cls, selector);
      }
    }
  }
}

#pragma GCC diagnostic ignored "-Wundeclared-selector"

static void handle_did_add_image(const SPKCodeRunnerMachoHeader *mhp) {
  SPKBootExecuteImage *prepareServiceImage = create_image_with_section(
      mhp, SPK_PREPARE_SERVICE_SECTION_NAME, @selector(executePrepareServiceTask));
  if (prepareServiceImage != NULL) {
    execute_method_with_images(prepareServiceImage);
    free(prepareServiceImage);
  }

  SPKBootExecuteImage *afterAllPrepareImage = create_image_with_section(
      mhp, SPK_AFTER_ALL_PREPARE_SECTION_NAME, @selector(executeAfterPrepareTask));
  if (afterAllPrepareImage != NULL) {
    execute_method_with_images(afterAllPrepareImage);
    free(afterAllPrepareImage);
  }
}

static void SPKRunSegment() __attribute__((no_sanitize("address"))) {
  Dl_info info;
  int ret = dladdr(reinterpret_cast<const void *>(&SPKRunSegment), &info);
  if (ret == 0) {
    return;
  }
#ifndef __LP64__
  const struct mach_header *mhp = reinterpret_cast<struct mach_header *>(info.dli_fbase);
#else  /* defined(__LP64__) */
  const struct mach_header_64 *mhp = reinterpret_cast<struct mach_header_64 *>(info.dli_fbase);
#endif /* defined(__LP64__) */

  handle_did_add_image(reinterpret_cast<const SPKCodeRunnerMachoHeader *>(mhp));
}

void SPKExecuteAllPrepareBootTask(void) {
  static BOOL isExecuted = NO;
  if (!isExecuted) {
    isExecuted = YES;
    SPKRunSegment();
  }
}
