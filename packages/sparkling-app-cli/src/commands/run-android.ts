// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execSync } from 'node:child_process';
import { getConfiguredDevServerPorts, loadAppConfig, resolveDevServerPort } from '../config';
import { autolink } from './autolink';
import { buildProject } from './build';
import { runCommand } from '../utils/exec';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';
import { ensureDevServerRunning } from '../utils/dev-server';

export interface RunAndroidOptions {
  cwd: string;
  skipCopy?: boolean;
}

interface AndroidDevice {
  serial: string;
  isEmulator: boolean;
}

function listConnectedAndroidDevices(): AndroidDevice[] {
  try {
    const output = execSync('adb devices', { encoding: 'utf8' });
    const lines = output.split(/\r?\n/).slice(1);
    const devices: AndroidDevice[] = [];
    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) continue;
      const [serial, state] = trimmed.split(/\s+/);
      if (state !== 'device') continue;
      devices.push({ serial, isEmulator: serial.startsWith('emulator-') });
    }
    return devices;
  } catch {
    return [];
  }
}

function resolveLocalIPv4(): string {
  const interfaces = os.networkInterfaces();
  const preferredOrder = ['en0', 'en1', 'wlan0'];

  for (const name of preferredOrder) {
    const candidates = interfaces[name] ?? [];
    for (const addr of candidates) {
      if (addr.family === 'IPv4' && !addr.internal) {
        return addr.address;
      }
    }
  }

  for (const candidates of Object.values(interfaces)) {
    for (const addr of candidates ?? []) {
      if (addr.family === 'IPv4' && !addr.internal) {
        return addr.address;
      }
    }
  }

  return '127.0.0.1';
}

function resolveAndroidAppGradle(cwd: string): string | null {
  const kts = path.resolve(cwd, 'android/app/build.gradle.kts');
  const groovy = path.resolve(cwd, 'android/app/build.gradle');
  if (fs.existsSync(kts)) return kts;
  if (fs.existsSync(groovy)) return groovy;
  return null;
}

function resolveGradle(cwd: string): string {
  const gradlew = path.resolve(cwd, 'android/gradlew');
  const gradleBat = path.resolve(cwd, 'android/gradlew.bat');
  if (process.platform === 'win32' && fs.existsSync(gradleBat)) {
    return gradleBat;
  }
  if (fs.existsSync(gradlew)) {
    return gradlew;
  }
  return 'gradlew';
}

function resolveAndroidPackageId(cwd: string): string {
  const gradlePath = resolveAndroidAppGradle(cwd);
  try {
    if (gradlePath && fs.existsSync(gradlePath)) {
      const content = fs.readFileSync(gradlePath, 'utf8');
      const m = content.match(/applicationId\s*=?\s*["']([^"']+)["']/);
      if (m) return m[1];
    }
  } catch {}
  return 'com.example.sparkling.go';
}

function resolveAndroidLaunchActivity(cwd: string, pkg: string): string {
  const manifestPath = path.resolve(cwd, 'android/app/src/main/AndroidManifest.xml');
  try {
    if (fs.existsSync(manifestPath)) {
      const xml = fs.readFileSync(manifestPath, 'utf8');
      const m = xml.match(/<activity[^>]*android:name="([^"]+)"[\s\S]*?<intent-filter>[\s\S]*?LAUNCHER[\s\S]*?<\/intent-filter>/);
      if (m) return m[1];
      const mAny = xml.match(/<activity[^>]*android:name="([^"]+)"/);
      if (mAny) return mAny[1];
    }
  } catch {}
  return `${pkg}.MainActivity`;
}

export async function runAndroid(options: RunAndroidOptions): Promise<void> {
  const { config } = await loadAppConfig(options.cwd);
  const { devPort: configuredDevPort, lynxPort } = getConfiguredDevServerPorts(config);
  if (configuredDevPort !== undefined && lynxPort !== undefined && configuredDevPort !== lynxPort) {
    console.warn(ui.warn(
      `Port config mismatch detected: dev.server.port=${configuredDevPort} and lynxConfig.server.port=${lynxPort}. ` +
      `run:android uses dev.server.port (${configuredDevPort}).`,
    ));
  }
  const devPort = resolveDevServerPort(config);
  const connectedDevices = listConnectedAndroidDevices();
  const emulatorSerials = connectedDevices.filter(d => d.isEmulator).map(d => d.serial);
  const devServerHostForApp = emulatorSerials.length > 0 ? '127.0.0.1' : resolveLocalIPv4();
  const devServerHostForCli = emulatorSerials.length > 0 ? '127.0.0.1' : '0.0.0.0';

  await ensureDevServerRunning(options.cwd, devPort, devServerHostForCli);

  for (const serial of emulatorSerials) {
    await runCommand('adb', ['-s', serial, 'reverse', `tcp:${devPort}`, `tcp:${devPort}`], {
      cwd: options.cwd,
      ignoreFailure: true,
    });
  }

  if (isVerboseEnabled()) {
    verboseLog(
      `run:android options -> skipCopy: ${options.skipCopy === true}, devPort: ${devPort}, devHost: ${devServerHostForApp}, emulatorCount: ${emulatorSerials.length}`,
    );
  }
  await autolink({ cwd: options.cwd, platform: 'android' });
  await buildProject({ cwd: options.cwd, skipCopy: options.skipCopy });

  const gradleCmd = resolveGradle(options.cwd);
  const androidDir = path.resolve(options.cwd, 'android');
  const env = { SPARKLING_USE_NATIVE_ASSETS: options.skipCopy ? 'false' : 'true' };
  if (isVerboseEnabled()) {
    verboseLog(`Resolved Gradle command: ${gradleCmd}`);
    verboseLog(`Android project directory: ${androidDir}`);
    verboseLog(`Env overrides: ${JSON.stringify(env)}`);
  }
  // Always build first so artifacts exist even when no device is connected
  const gradleArgs = [
    `-PsparklingDevServerHost=${devServerHostForApp}`,
    `-PsparklingDevServerPort=${devPort}`,
  ];

  await runCommand(gradleCmd, ['assembleDebug', ...gradleArgs], { cwd: androidDir, env });
  // Try to install if a device is connected; ignore failure so CI/headless environments are not blocked
  console.log(ui.tip('Attempting device install (if any devices are connected)...'));
  await runCommand(gradleCmd, ['installDebug', ...gradleArgs], { cwd: androidDir, ignoreFailure: true, env });
  // Attempt to launch the app on a connected device
  const pkg = resolveAndroidPackageId(options.cwd);
  const activity = resolveAndroidLaunchActivity(options.cwd, pkg);
  if (isVerboseEnabled()) {
    verboseLog(`Resolved Android package: ${pkg}`);
    verboseLog(`Resolved launch activity: ${activity}`);
  }
  console.log(ui.tip(`Attempting to launch ${pkg}/${activity} on device (if connected)...`));
  await runCommand('adb', ['shell', 'am', 'start', '-n', `${pkg}/${activity}`], { cwd: options.cwd, ignoreFailure: true });
  console.log(ui.success('Android debug build ready. If no device, manually install via `adb install` on the built APK.'));
}
