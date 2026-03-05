// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

export type CheckStatus = 'pass' | 'fail' | 'warn' | 'skip';

export interface CheckResult {
  /** Display name of the check (e.g. "Node.js", "JDK") */
  name: string;
  /** Category for grouping in output */
  category: 'general' | 'android' | 'ios' | 'web';
  /** Whether the check passed */
  status: CheckStatus;
  /** Detected version string, if applicable */
  version?: string;
  /** Expected version range, if applicable */
  expected?: string;
  /** Human-readable detail message */
  message?: string;
  /** Short fix instruction used to build the Coding Agent prompt */
  fixHint?: string;
}
