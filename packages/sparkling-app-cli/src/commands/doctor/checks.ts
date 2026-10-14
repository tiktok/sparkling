// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { execFileSync, spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import semver from 'semver';
import { verboseLog } from '../../utils/verbose';
import type { CheckResult } from './types';

function normalizeProcessOutput(output: unknown): string | null {
  if (typeof output === 'string') {
    return output.trim() || null;
  }
  if (Buffer.isBuffer(output)) {
    return output.toString('utf8').trim() || null;
  }
  return null;
}

/**
 * Run a command and return its stdout (trimmed).
 * Returns `null` if the command is not found or fails.
 */
function exec(cmd: string, args: string[]): string | null {
  try {
    const result = execFileSync(cmd, args, {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: 15_000,
    });
    return result.trim();
  } catch {
    return null;
  }
}

/**
 * Get `java -version` output, which normally prints to stderr.
 */
function getJavaVersionOutput(): string | null {
  try {
    const child = spawnSync('java', ['-version'], {
      encoding: 'utf8',
      shell: false,
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: 15_000,
    });
    if (child.error) {
      return null;
    }
    return normalizeProcessOutput(child.stderr) ?? normalizeProcessOutput(child.stdout);
  } catch {
    return null;
  }
}

const NODE_REQUIRED = '^22 || ^24';

export function checkNodeVersion(): CheckResult {
  const base: Omit<CheckResult, 'status'> = {
    name: 'Node.js',
    category: 'general',
    expected: NODE_REQUIRED,
  };

  const raw = exec('node', ['--version']);
  if (!raw) {
    return {
      ...base,
      status: 'fail',
      message: 'Node.js is not installed or not found in PATH',
      fixHint: 'Node.js is not installed. I need Node.js version ^22 or ^24.',
    };
  }

  const version = semver.clean(raw);
  if (!version) {
    return {
      ...base,
      status: 'fail',
      version: raw,
      message: `Could not parse Node.js version: ${raw}`,
      fixHint: `Could not parse Node.js version "${raw}". I need Node.js version ^22 or ^24.`,
    };
  }

  verboseLog(`Node.js version: ${version}`);

  if (!semver.satisfies(version, NODE_REQUIRED)) {
    return {
      ...base,
      status: 'fail',
      version,
      message: `Node.js ${version} does not satisfy ${NODE_REQUIRED}`,
      fixHint: `Node.js version is ${version}, but Sparkling requires ${NODE_REQUIRED}. Please upgrade Node.js.`,
    };
  }

  return { ...base, status: 'pass', version };
}

const JDK_MIN_MAJOR = 11;

export function checkJdk(): CheckResult {
  const base: Omit<CheckResult, 'status'> = {
    name: 'JDK',
    category: 'android',
    expected: `>= ${JDK_MIN_MAJOR}`,
  };

  const output = getJavaVersionOutput();
  if (!output) {
    return {
      ...base,
      status: 'fail',
      message: 'Java is not installed or not found in PATH',
      fixHint: `Java (JDK) is not installed. I need JDK ${JDK_MIN_MAJOR} or higher.`,
    };
  }

  verboseLog(`java -version output:\n${output}`);

  // Parse version from patterns like:
  //   openjdk version "11.0.21" ...
  //   java version "1.8.0_392" ...
  //   openjdk version "17.0.9" ...
  const match = output.match(/version\s+"(\d+)(?:\.(\d+))?(?:\.(\d+))?/);
  if (!match) {
    return {
      ...base,
      status: 'fail',
      message: `Could not parse Java version from output: ${output.split('\n')[0]}`,
      fixHint: `Could not determine Java version. I need JDK ${JDK_MIN_MAJOR}+.`,
    };
  }

  let major = parseInt(match[1], 10);
  const minor = match[2] ? parseInt(match[2], 10) : 0;
  const patch = match[3] ? parseInt(match[3], 10) : 0;

  // Old-style versioning: "1.8.0" means Java 8
  if (major === 1 && minor > 0) {
    major = minor;
  }

  const versionStr = `${major}.${minor}.${patch}`;

  if (major < JDK_MIN_MAJOR) {
    return {
      ...base,
      status: 'fail',
      version: versionStr,
      message: `JDK ${major} is below the minimum required version ${JDK_MIN_MAJOR}`,
      fixHint: `JDK version is ${major}, but Sparkling requires JDK ${JDK_MIN_MAJOR}+. Please install JDK ${JDK_MIN_MAJOR}.`,
    };
  }

  return { ...base, status: 'pass', version: versionStr };
}

export function checkAndroidSdk(): CheckResult {
  const base: Omit<CheckResult, 'status'> = {
    name: 'Android SDK',
    category: 'android',
  };

  const sdkRoot = process.env.ANDROID_HOME ?? process.env.ANDROID_SDK_ROOT;

  if (!sdkRoot) {
    return {
      ...base,
      status: 'fail',
      message: 'ANDROID_HOME (or ANDROID_SDK_ROOT) environment variable is not set',
      fixHint:
        'ANDROID_HOME environment variable is not set. I need it pointing to my Android SDK installation directory.',
    };
  }

  if (!fs.existsSync(sdkRoot)) {
    return {
      ...base,
      status: 'fail',
      message: `ANDROID_HOME points to "${sdkRoot}" but the directory does not exist`,
      fixHint: `ANDROID_HOME is set to "${sdkRoot}" but that directory does not exist. Please install the Android SDK or fix the path.`,
    };
  }

  verboseLog(`ANDROID_HOME: ${sdkRoot}`);

  // Check for compileSdk 34 platform
  const platformDir = path.join(sdkRoot, 'platforms', 'android-34');
  if (!fs.existsSync(platformDir)) {
    return {
      ...base,
      status: 'warn',
      version: sdkRoot,
      message: `Android SDK found at "${sdkRoot}" but platform android-34 (compileSdk 34) is missing`,
      fixHint:
        'Android SDK is installed but platform android-34 (compileSdk 34) is missing. Please install it via Android Studio SDK Manager or `sdkmanager "platforms;android-34"`.',
    };
  }

  return {
    ...base,
    status: 'pass',
    version: sdkRoot,
    message: 'Android SDK found with platform android-34',
  };
}

export function checkAdb(): CheckResult {
  const base: Omit<CheckResult, 'status'> = {
    name: 'adb',
    category: 'android',
  };

  const output = exec('adb', ['version']);
  if (!output) {
    return {
      ...base,
      status: 'fail',
      message: 'adb is not found in PATH',
      fixHint:
        'adb is not found in PATH. It is bundled with Android SDK platform-tools. Please ensure Android SDK platform-tools is installed and added to PATH.',
    };
  }

  const match = output.match(/(\d+\.\d+\.\d+)/);
  const version = match ? match[1] : undefined;

  verboseLog(`adb version: ${version ?? output}`);

  return { ...base, status: 'pass', version };
}

const XCODE_MIN_MAJOR = 26;

function isMacOS(): boolean {
  return process.platform === 'darwin';
}

export function checkXcode(): CheckResult {
  const base: Omit<CheckResult, 'status'> = {
    name: 'Xcode',
    category: 'ios',
    expected: `>= ${XCODE_MIN_MAJOR}`,
  };

  if (!isMacOS()) {
    return {
      ...base,
      status: 'skip',
      message: 'Not on macOS — Xcode check skipped',
    };
  }

  const output = exec('xcodebuild', ['-version']);
  if (!output) {
    return {
      ...base,
      status: 'fail',
      message: 'Xcode is not installed or xcodebuild is not available',
      fixHint: `Xcode is not installed. I need Xcode ${XCODE_MIN_MAJOR} or later. Please install it from the Mac App Store.`,
    };
  }

  verboseLog(`xcodebuild -version output: ${output}`);

  // Parse "Xcode 15.4" or "Xcode 26.0"
  const match = output.match(/Xcode\s+(\d+)(?:\.(\d+))?/);
  if (!match) {
    return {
      ...base,
      status: 'fail',
      message: `Could not parse Xcode version from: ${output.split('\n')[0]}`,
      fixHint: `Could not determine Xcode version. I need Xcode ${XCODE_MIN_MAJOR}+.`,
    };
  }

  const major = parseInt(match[1], 10);
  const minor = match[2] ? parseInt(match[2], 10) : 0;
  const versionStr = `${major}.${minor}`;

  if (major < XCODE_MIN_MAJOR) {
    return {
      ...base,
      status: 'fail',
      version: versionStr,
      message: `Xcode ${versionStr} is below the required version ${XCODE_MIN_MAJOR}`,
      fixHint: `Xcode version is ${versionStr}, but Sparkling requires Xcode ${XCODE_MIN_MAJOR}+. Please update Xcode from the Mac App Store.`,
    };
  }

  return { ...base, status: 'pass', version: versionStr };
}

export function checkCocoaPods(): CheckResult {
  const base: Omit<CheckResult, 'status'> = {
    name: 'CocoaPods',
    category: 'ios',
  };

  if (!isMacOS()) {
    return {
      ...base,
      status: 'skip',
      message: 'Not on macOS — CocoaPods check skipped',
    };
  }

  const output = exec('pod', ['--version']);
  if (!output) {
    return {
      ...base,
      status: 'fail',
      message: 'CocoaPods is not installed or not found in PATH',
      fixHint:
        'CocoaPods is not installed. I need CocoaPods for iOS dependency management. Please install it with `sudo gem install cocoapods` or `brew install cocoapods`.',
    };
  }

  const version = output.split('\n')[0].trim();
  verboseLog(`CocoaPods version: ${version}`);

  return { ...base, status: 'pass', version };
}

export function checkSimulator(): CheckResult {
  const base: Omit<CheckResult, 'status'> = {
    name: 'iOS Simulator',
    category: 'ios',
  };

  if (!isMacOS()) {
    return {
      ...base,
      status: 'skip',
      message: 'Not on macOS — iOS Simulator check skipped',
    };
  }

  const output = exec('xcrun', ['simctl', 'list', 'devices', 'available']);
  if (!output) {
    return {
      ...base,
      status: 'fail',
      message: 'Could not list iOS simulators (xcrun simctl failed)',
      fixHint:
        'iOS Simulator is not available. Please install Xcode and an iOS Simulator runtime from Xcode > Settings > Platforms.',
    };
  }

  // Count lines that look like device entries (indented, with UDID in parentheses)
  const deviceLines = output
    .split('\n')
    .filter((line) => /^\s+.+\([\dA-F-]+\)/i.test(line));

  verboseLog(`Found ${deviceLines.length} available iOS simulators`);

  if (deviceLines.length === 0) {
    return {
      ...base,
      status: 'fail',
      message: 'No available iOS simulators found',
      fixHint:
        'No iOS simulators are available. Please install an iOS Simulator runtime from Xcode > Settings > Platforms.',
    };
  }

  return {
    ...base,
    status: 'pass',
    message: `${deviceLines.length} simulator(s) available`,
  };
}
