// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import <Foundation/Foundation.h>

typedef NS_OPTIONS(NSUInteger, SPKMethodEngineType) {
    SPKMethodEngineTypeWeb = 1 << 0,
    SPKMethodEngineTypeLynx = 1 << 1,
    SPKMethodEngineTypeAll = SPKMethodEngineTypeWeb | SPKMethodEngineTypeLynx
};
