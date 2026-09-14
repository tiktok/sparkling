// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

export type HarmonyTool = 'ohpm' | 'hvigorw' | 'hdc';

const SDK_ENV_KEYS = [
  'DEVECO_SDK_HOME',
  'HOS_SDK_HOME',
  'OHOS_HOME',
  'OHOS_SDK_HOME',
] as const;

function configuredHarmonyRoots(): string[] {
  const sdkRoots = SDK_ENV_KEYS.map(key => process.env[key])
    .filter((value): value is string => Boolean(value))
    .map(value => (path.basename(value) === 'sdk' ? path.dirname(value) : value));
  return [process.env.DEVECO_HOME, process.env.DEVECO_STUDIO_HOME, ...sdkRoots].filter(
    (value): value is string => Boolean(value),
  );
}

function defaultDevEcoRoots(): string[] {
  if (process.platform === 'darwin') {
    return [
      '/Applications/DevEco-Studio.app/Contents',
      path.join(os.homedir(), 'Applications/DevEco-Studio.app/Contents'),
      path.join(os.homedir(), 'command-line-tools'),
    ];
  }
  if (process.platform === 'win32') {
    return [
      ...(process.env.ProgramFiles ? [path.join(process.env.ProgramFiles, 'Huawei', 'DevEco Studio')] : []),
      ...(process.env.LOCALAPPDATA ? [path.join(process.env.LOCALAPPDATA, 'Huawei', 'DevEcoStudio')] : []),
      path.join(os.homedir(), 'command-line-tools'),
    ];
  }
  return [path.join(os.homedir(), 'command-line-tools')];
}

const TOOL_PATHS: Record<Exclude<HarmonyTool, 'hdc'>, string[]> = {
  ohpm: ['tools/ohpm/bin', 'ohpm/bin', 'bin'],
  hvigorw: ['tools/hvigor/bin', 'hvigor/bin', 'bin'],
};

function executableName(tool: HarmonyTool): string {
  if (process.platform !== 'win32') return tool;
  if (tool === 'hdc') return 'hdc.exe';
  return `${tool}.bat`;
}

function findOnPath(tool: HarmonyTool): string | undefined {
  const name = executableName(tool);
  for (const directory of (process.env.PATH ?? '').split(path.delimiter)) {
    if (!directory) continue;
    const candidate = path.join(directory, name);
    if (fs.existsSync(candidate)) return fs.realpathSync(candidate);
  }
  return undefined;
}

function findHdcInSdk(sdkHome: string): string | undefined {
  const name = executableName('hdc');
  const versions = fs.existsSync(sdkHome)
    ? fs
        .readdirSync(sdkHome, { withFileTypes: true })
        .filter(entry => entry.isDirectory())
        .map(entry => entry.name)
    : [];
  const candidates = versions.flatMap(version => [
    path.join(sdkHome, version, 'openharmony', 'toolchains', name),
    path.join(sdkHome, version, 'base', 'toolchains', name),
    path.join(sdkHome, version, 'toolchains', name),
  ]);
  return candidates.find(candidate => fs.existsSync(candidate));
}

function sdkHomeFromEnvironment(): string | undefined {
  for (const key of SDK_ENV_KEYS) {
    const configured = process.env[key];
    if (!configured) continue;
    const nestedSdk = path.join(configured, 'sdk');
    if (fs.existsSync(nestedSdk)) return nestedSdk;
    if (fs.existsSync(configured)) return configured;
  }
  return undefined;
}

export function resolveHarmonyTool(tool: HarmonyTool): string {
  for (const root of configuredHarmonyRoots()) {
    if (tool === 'hdc') {
      const hdc = findHdcInSdk(path.basename(root) === 'sdk' ? root : path.join(root, 'sdk'));
      if (hdc) return hdc;
      continue;
    }
    for (const relativePath of TOOL_PATHS[tool]) {
      const candidate = path.join(root, relativePath, executableName(tool));
      if (fs.existsSync(candidate)) return candidate;
    }
  }

  const pathTool = findOnPath(tool);
  if (pathTool) return pathTool;

  for (const root of defaultDevEcoRoots()) {
    if (tool === 'hdc') {
      const hdc = findHdcInSdk(path.join(root, 'sdk'));
      if (hdc) return hdc;
      continue;
    }
    for (const relativePath of TOOL_PATHS[tool]) {
      const candidate = path.join(root, relativePath, executableName(tool));
      if (fs.existsSync(candidate)) return candidate;
    }
  }
  return executableName(tool);
}

export function resolveHarmonySdkHome(): string | undefined {
  const configured = sdkHomeFromEnvironment();
  if (configured) return configured;

  for (const root of configuredHarmonyRoots()) {
    const candidate = path.basename(root) === 'sdk' ? root : path.join(root, 'sdk');
    if (fs.existsSync(candidate)) return candidate;
  }

  const hdc = findOnPath('hdc');
  if (hdc) {
    let current = path.dirname(hdc);
    while (current !== path.dirname(current)) {
      if (path.basename(current) === 'sdk') return current;
      current = path.dirname(current);
    }
  }

  for (const root of defaultDevEcoRoots()) {
    const candidate = path.join(root, 'sdk');
    if (fs.existsSync(candidate)) return candidate;
  }
  return undefined;
}

export function resolveHarmonyJavaHome(): string | undefined {
  if (process.env.JAVA_HOME) return process.env.JAVA_HOME;
  for (const root of [...configuredHarmonyRoots(), ...defaultDevEcoRoots()]) {
    for (const candidate of [path.join(root, 'jbr/Contents/Home'), path.join(root, 'jbr')]) {
      if (fs.existsSync(path.join(candidate, 'bin', process.platform === 'win32' ? 'java.exe' : 'java'))) {
        return candidate;
      }
    }
  }
  return undefined;
}
