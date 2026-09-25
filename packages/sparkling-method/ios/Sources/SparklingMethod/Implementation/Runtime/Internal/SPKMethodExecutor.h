// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

#import "SPKMethodInvocation.h"

@interface SPKMethodExecutor : NSObject

/// Execute an already selected method through parameter parsing and response assembly.
/// Registration, engine eligibility, and authorization are the caller's responsibility.
/// engineType selects response formatting; hooks may supply the host's existing policy.
/// Async completions retain the hooks but do not retain the method or change callback queues.
+ (void)invokeMethod:(nullable SPKMethod *)method
              params:(nullable NSDictionary *)params
          engineType:(SPKMethodEngineType)engineType
               hooks:(nullable SPKMethodInvocationHooks *)hooks
   completionHandler:(nullable SPKMethodInvocationCompletionHandler)completionHandler;

@end
