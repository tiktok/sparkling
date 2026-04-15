// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';
import { getConfiguredDevServerPorts, loadAppConfig, resolveDevServerPort } from '../config';
import { autolink } from './autolink';
import { buildProject } from './build';
import { runCommand } from '../utils/exec';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';
import { ensureDevServerRunning } from '../utils/dev-server';

export interface RunIosOptions {
  cwd: string;
  skipCopy?: boolean;
  device?: string;
  skipPodInstall?: boolean;
}

interface SimulatorDevice {
  name: string;
  udid: string;
  runtime?: string;
  state?: string;
  isAvailable?: boolean;
}

const DEFAULT_BUNDLE_ID = 'com.sparkling.app.SparklingGo';

/**
 * Read the main app bundle identifier from the Xcode project.
 * Falls back to the env var or the hardcoded default.
 */
function resolveBundleId(cwd: string): string {
  const envOverride = process.env.SPARKLING_IOS_BUNDLE_ID;
  if (envOverride) return envOverride;

  const pbxprojPath = path.resolve(cwd, 'ios', 'SparklingGo.xcodeproj', 'project.pbxproj');
  if (!fs.existsSync(pbxprojPath)) return DEFAULT_BUNDLE_ID;

  try {
    const content = fs.readFileSync(pbxprojPath, 'utf8');
    // Match all PRODUCT_BUNDLE_IDENTIFIER values
    const matches = [...content.matchAll(/PRODUCT_BUNDLE_IDENTIFIER\s*=\s*([^;\n]+)/g)]
      .map(m => m[1].trim().replace(/^"|"$/g, ''));
    // Pick the first one that doesn't look like a test target
    const appBundleId = matches.find(id => !/tests?$/i.test(id));
    if (appBundleId) {
      if (isVerboseEnabled()) {
        verboseLog(`Resolved bundle ID from pbxproj: ${appBundleId}`);
      }
      return appBundleId;
    }
  } catch (error) {
    if (isVerboseEnabled()) {
      verboseLog(`Failed to read bundle ID from pbxproj: ${String(error)}`);
    }
  }

  return DEFAULT_BUNDLE_ID;
}

function resolveWorkspacePath(cwd: string): string | null {
  const candidates = [
    path.resolve(cwd, 'ios', 'SparklingGo.xcworkspace'),
    path.resolve(cwd, 'ios', 'SparklingGo', 'SparklingGo.xcworkspace'),
  ];
  return candidates.find(p => fs.existsSync(p)) ?? null;
}

function findNearestGemfile(startDir: string): string | null {
  let current = startDir;
  const { root } = path.parse(startDir);
  while (true) {
    const gemfile = path.join(current, 'Gemfile');
    if (fs.existsSync(gemfile)) return gemfile;
    if (current === root) break;
    current = path.dirname(current);
  }
  return null;
}

function getRequiredBundlerVersion(gemfileDir: string): string | null {
  const lockfilePath = path.join(gemfileDir, 'Gemfile.lock');
  if (!fs.existsSync(lockfilePath)) return null;
  const content = fs.readFileSync(lockfilePath, 'utf8');
  const match = content.match(/BUNDLED WITH\s+(\S+)/);
  return match?.[1] ?? null;
}

function isBundlerVersionInstalled(version: string): boolean {
  try {
    const output = execSync('gem list bundler --exact --versions', { stdio: 'pipe' }).toString('utf8');
    // Output looks like: bundler (2.5.6, 2.3.27, default: 2.3.26)
    return output.includes(version);
  } catch {
    return false;
  }
}

async function ensureRequiredBundler(gemfileDir: string): Promise<void> {
  const requiredVersion = getRequiredBundlerVersion(gemfileDir);
  if (!requiredVersion) {
    verboseLog('No BUNDLED WITH version found in Gemfile.lock, skipping bundler version check.');
    return;
  }

  if (isBundlerVersionInstalled(requiredVersion)) {
    verboseLog(`Bundler ${requiredVersion} is already installed.`);
    return;
  }

  console.log(ui.info(`Installing required Bundler version ${requiredVersion}...`));

  // First try with --user-install to avoid permission issues with system Ruby
  try {
    await runCommand('gem', ['install', '--user-install', `bundler:${requiredVersion}`], { cwd: gemfileDir });
  } catch {
    // Fall back to normal install (may work with Homebrew Ruby or rbenv)
    try {
      await runCommand('gem', ['install', `bundler:${requiredVersion}`], { cwd: gemfileDir });
    } catch (error) {
      console.warn(
        ui.warn(
          `Failed to install Bundler ${requiredVersion}: ${String(error)}. ` +
            `You may need to run \`gem install bundler:${requiredVersion}\` manually (possibly with sudo).`,
        ),
      );
    }
  }

  // After install, the user gem bin may not be in PATH. Ensure bundle can find it.
  if (!isBundlerVersionInstalled(requiredVersion)) {
    console.warn(
      ui.warn(
        `Bundler ${requiredVersion} could not be verified after install. Proceeding anyway...`,
      ),
    );
  }
}

async function ensureBundleInstall(gemfileDir: string): Promise<boolean> {
  await ensureRequiredBundler(gemfileDir);

  console.log(ui.info('Installing Ruby gems via bundle install...'));
  try {
    await runCommand('bundle', ['install'], { cwd: gemfileDir });
    return true;
  } catch (error) {
    console.warn(
      ui.warn(
        `bundle install failed: ${String(error)}. ` +
          'Ensure Bundler is installed and Ruby is configured properly.',
      ),
    );
    return false;
  }
}

async function installPods(podfilePath: string): Promise<void> {
  const podDir = path.dirname(podfilePath);
  const gemfile = findNearestGemfile(podDir);
  if (!gemfile) {
    console.warn(
      ui.warn(
        'Gemfile not found for iOS project. Ensure a Gemfile exists in the workspace (root or template) to pin CocoaPods.',
      ),
    );
  }

  const gemfileDir = gemfile ? path.dirname(gemfile) : podDir;
  const bundled = await ensureBundleInstall(gemfileDir);
  if (!bundled) {
    throw new Error(
      'bundle install failed. Please run `bundle install` manually to set up the pinned CocoaPods version.',
    );
  }

  try {
    const env = { ...(gemfile ? { BUNDLE_GEMFILE: gemfile } : {}) };
    await runCommand('bundle', ['exec', 'pod', 'install'], { cwd: podDir, env });
  } catch (error) {
    throw new Error(
      `bundle exec pod install failed: ${String(error)}. Older CocoaPods (<=1.11) cannot parse PBXFileSystemSynchronizedRootGroup; run \`bundle install\` to pick up the pinned version.`,
    );
  }
}

function listAvailableSimulators(): SimulatorDevice[] {
  try {
    const raw = execSync('xcrun simctl list devices available --json', { stdio: 'pipe' }).toString('utf8');
    const parsed = JSON.parse(raw) as { devices?: Record<string, SimulatorDevice[]> };
    const devices = Object.entries(parsed.devices ?? {})
      .filter(([runtime]) => runtime.toLowerCase().includes('ios'))
      .flatMap(([, list]) => list);
    return devices.filter(d => d.isAvailable);
  } catch (error) {
    console.warn(ui.warn(`Failed to list simulators, falling back to default: ${String(error)}`));
    return [];
  }
}

function parseRuntimeVersion(runtime?: string): number {
  if (!runtime) return 0;
  const match = runtime.match(/iOS-(\d+)-?(\d+)?/i);
  if (!match) return 0;
  const major = Number(match[1]) || 0;
  const minor = Number(match[2] ?? '0') || 0;
  return major + minor / 10;
}

function pickSimulator(preferred?: string): SimulatorDevice | null {
  const devices = listAvailableSimulators();
  if (!devices.length) {
    return null;
  }

  if (preferred) {
    const match = devices.find(d => d.name === preferred || d.udid === preferred);
    if (match) return match;
  }

  const booted = devices.filter(d => (d.state ?? '').toLowerCase() === 'booted');
  if (booted.length) {
    return booted.sort((a, b) => parseRuntimeVersion(b.runtime) - parseRuntimeVersion(a.runtime))[0] ?? null;
  }

  const preferredNames = [
    'iPhone 17 Pro',
    'iPhone 17',
    'iPhone 16 Pro',
    'iPhone 16',
    'iPhone 15 Pro',
    'iPhone 15',
    'iPhone 14 Pro',
    'iPhone 14',
    'iPhone 13',
    'iPhone SE',
  ];

  return devices
    .sort((a, b) => {
      const runtimeDiff = parseRuntimeVersion(b.runtime) - parseRuntimeVersion(a.runtime);
      if (runtimeDiff !== 0) return runtimeDiff;
      const aIdx = preferredNames.findIndex(n => a.name.includes(n));
      const bIdx = preferredNames.findIndex(n => b.name.includes(n));
      if (aIdx === -1 && bIdx === -1) return a.name.localeCompare(b.name);
      if (aIdx === -1) return 1;
      if (bIdx === -1) return -1;
      return aIdx - bIdx;
    })[0] ?? null;
}

export async function runIos(options: RunIosOptions): Promise<void> {
  const { config } = await loadAppConfig(options.cwd);
  const { devPort: configuredDevPort, lynxPort } = getConfiguredDevServerPorts(config);
  if (configuredDevPort !== undefined && lynxPort !== undefined && configuredDevPort !== lynxPort) {
    console.warn(ui.warn(
      `Port config mismatch detected: dev.server.port=${configuredDevPort} and lynxConfig.server.port=${lynxPort}. ` +
      `run:ios uses dev.server.port (${configuredDevPort}).`,
    ));
  }
  const devPort = resolveDevServerPort(config);
  await ensureDevServerRunning(options.cwd, devPort);
  if (isVerboseEnabled()) {
    verboseLog(`run:ios options -> skipCopy: ${options.skipCopy === true}, device: ${options.device ?? '(auto)'}, skipPodInstall: ${options.skipPodInstall === true}, devPort: ${devPort}`);
  }
  const preferredDevice = options.device ?? process.env.SPARKLING_IOS_SIMULATOR;
  const device = pickSimulator(preferredDevice);
  if (!device) {
    throw new Error('No available iOS simulators found. Install Xcode CLT / Simulator and try again.');
  }
  console.log(ui.headline(`Using simulator: ${device.name} (${device.udid})${device.runtime ? ` [${device.runtime}]` : ''}`));

  const bundleId = resolveBundleId(options.cwd);
  const podfilePath = path.resolve(options.cwd, 'ios', 'Podfile');
  const hasPodfile = fs.existsSync(podfilePath);
  if (isVerboseEnabled()) {
    verboseLog(`Podfile path: ${podfilePath} (exists: ${hasPodfile})`);
  }

  await autolink({ cwd: options.cwd, platform: 'ios' });

  if (hasPodfile) {
    if (options.skipPodInstall) {
      console.log(ui.tip('Skipping pod install (per flag). Run it manually if pods are out of date.'));
    } else {
      console.log(ui.info('Running bundle exec pod install...'));
      await installPods(podfilePath);
    }
  }

  await buildProject({ cwd: options.cwd, skipCopy: options.skipCopy });

  // If buildProject skipped the copy, we still need real files for iOS because
  // symlinks are not valid inside iOS app bundles – the simulator installer
  // (installd) rejects them with "invalid symlink" errors.
  if (options.skipCopy) {
    const distPath = path.resolve(options.cwd, 'dist');
    const assetsDir = path.resolve(options.cwd, 'ios/LynxResources/Assets');
    const assetsParent = path.dirname(assetsDir);
    if (isVerboseEnabled()) {
      verboseLog(`Copying Assets from ${distPath} to ${assetsDir} (iOS requires real files, not symlinks)`);
    }
    try {
      fs.mkdirSync(distPath, { recursive: true });
      fs.mkdirSync(assetsParent, { recursive: true });
      if (fs.existsSync(assetsDir)) {
        const stat = fs.lstatSync(assetsDir);
        if (stat.isSymbolicLink()) {
          // Remove stale symlink left by a previous run
          fs.unlinkSync(assetsDir);
        } else {
          fs.rmSync(assetsDir, { recursive: true, force: true });
        }
      }
      fs.cpSync(distPath, assetsDir, { recursive: true, force: true, dereference: true });
    } catch (error) {
      console.warn(ui.warn(`Failed to copy iOS Assets from dist: ${String(error)}`));
    }
  }

  let workspacePath = resolveWorkspacePath(options.cwd);
  if (!workspacePath && hasPodfile && !options.skipPodInstall) {
    console.log(ui.info('Installing iOS pods to generate workspace...'));
    await installPods(podfilePath);
    workspacePath = resolveWorkspacePath(options.cwd);
  }

  if (!workspacePath) {
    throw new Error('iOS workspace not found (expected SparklingGo.xcworkspace). Run `bundle exec pod install` inside the ios directory.');
  }
  if (isVerboseEnabled()) {
    verboseLog(`Using workspace at ${workspacePath}`);
  }

  // ── Step 1: Best-effort simulator boot (never blocks the build) ──
  // Boot timeout: 120s is generous but prevents indefinite hangs caused by
  // simdiskimaged or CoreSimulatorService issues (known macOS/Xcode bug).
  const BOOT_TIMEOUT_MS = 120_000;
  let simulatorReady = (device.state ?? '').toLowerCase() === 'booted';

  if (!simulatorReady) {
    console.log(ui.info(`Booting simulator ${device.name}...`));
    try {
      await runCommand('xcrun', ['simctl', 'boot', device.udid], {
        cwd: options.cwd,
        timeoutMs: BOOT_TIMEOUT_MS,
      });
      console.log(ui.info('Waiting for simulator to finish booting...'));
      // Do NOT pass -b here: we already issued the boot command above.
      // -b would attempt a second boot and can hang indefinitely.
      await runCommand('xcrun', ['simctl', 'bootstatus', device.udid], {
        cwd: options.cwd,
        timeoutMs: BOOT_TIMEOUT_MS,
      });
      simulatorReady = true;
    } catch (error) {
      console.warn(ui.warn(
        `Simulator boot failed: ${String(error)}\n` +
        'The Xcode build will continue. You can start the simulator manually or deploy to a physical device.',
      ));
    }
  } else {
    console.log(ui.info(`Simulator ${device.name} is already booted.`));
  }

  if (simulatorReady) {
    // Focus the chosen simulator only so we don't spawn the default device alongside it
    console.log(ui.info('Opening Simulator app...'));
    await runCommand('open', ['-a', 'Simulator', '--args', '-CurrentDeviceUDID', device.udid], {
      cwd: options.cwd,
      ignoreFailure: true,
      timeoutMs: 30_000,
    });
  }

  // ── Step 2: Build (always runs, regardless of simulator state) ──
  const destination = `id=${device.udid}`;
  const derivedDataPath = path.resolve(options.cwd, 'ios/build');
  console.log(ui.info('Building Xcode project (this may take a while)...'));
  await runCommand('xcodebuild', [
    '-workspace',
    workspacePath,
    '-scheme',
    'SparklingGo',
    '-configuration',
    'Debug',
    '-sdk',
    'iphonesimulator',
    '-derivedDataPath',
    derivedDataPath,
    '-destination',
    destination,
    'CODE_SIGN_IDENTITY=',
    'CODE_SIGNING_ALLOWED=NO',
    'CODE_SIGNING_REQUIRED=NO',
    'CODE_SIGN_ENTITLEMENTS=',
    'build',
  ], { cwd: options.cwd });

  // ── Step 3: Install & launch (best-effort, only if simulator is up) ──
  const appPath = path.resolve(options.cwd, 'ios/build/Build/Products/Debug-iphonesimulator/SparklingGo.app');
  if (!fs.existsSync(appPath)) {
    console.warn(ui.warn(`Built app not found at ${appPath}, skipped simulator install/launch.`));
  } else if (!simulatorReady) {
    console.log(ui.success(`Build succeeded. App is at: ${appPath}`));
    console.log(ui.tip(
      'The simulator is not running. To install manually:\n' +
      `  xcrun simctl boot "${device.udid}"\n` +
      `  xcrun simctl install "${device.udid}" "${appPath}"\n` +
      `  xcrun simctl launch "${device.udid}" ${bundleId}`,
    ));
  } else {
    console.log(ui.info('Installing app on simulator...'));
    await runCommand('xcrun', ['simctl', 'install', device.udid, appPath], { cwd: options.cwd, ignoreFailure: true });
    console.log(ui.info(`Launching ${bundleId}...`));
    await runCommand('xcrun', ['simctl', 'launch', device.udid, bundleId], {
      cwd: options.cwd,
      ignoreFailure: true,
      env: {
        SIMCTL_CHILD_SPARKLING_DEV_SERVER_PORT: String(devPort),
        SIMCTL_CHILD_SPARKLING_DEV_SERVER_HOST: '127.0.0.1',
      },
    });
  }
}
