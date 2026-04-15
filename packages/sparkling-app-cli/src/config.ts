/// <reference types="node" />
// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'fs';
import path from 'path';
import { spawnSync } from 'child_process';
import { pathToFileURL } from 'url';
import { createRequire } from 'module';
// no url import needed when using require()
import type { AppConfig } from './types';
import { DEV_SERVER_PORT } from './constants';

let registeredTsNode = false;
const pkgRequire = createRequire(__filename);

function isEsmProject(cwd: string): boolean {
  try {
    let dir = cwd;
    while (true) {
      const pkg = path.join(dir, 'package.json');
      if (fs.existsSync(pkg)) {
        const json = JSON.parse(fs.readFileSync(pkg, 'utf8'));
        return json?.type === 'module';
      }
      const parent = path.dirname(dir);
      if (parent === dir) break;
      dir = parent;
    }
  } catch {
    // ignore, assume CJS
  }
  return false;
}

function ensureTsNodeRegistered() {
  if (registeredTsNode) {
    return;
  }

  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const tsNode = pkgRequire('ts-node');
  tsNode.register({
    transpileOnly: true,
    compilerOptions: {
      module: 'commonjs',
      moduleResolution: 'node',
      esModuleInterop: true,
      jsx: 'react-jsx',
    },
  });
  registeredTsNode = true;
}

export function createTempLynxConfig(cwd: string, appConfigPath: string): string {
  const tempDir = path.resolve(cwd, '.sparkling');
  fs.mkdirSync(tempDir, { recursive: true });
  const tempConfigPath = path.join(tempDir, 'lynx.build.config.ts');
  const rel = path.relative(tempDir, path.resolve(appConfigPath)).split(path.sep).join('/');
  const content = [
    `import cfgModule from '${rel.startsWith('.') ? rel : './' + rel}'`,
    'const cfg: any = (cfgModule as any).default ?? cfgModule',
    'export default (cfg.lynxConfig ?? cfg) as any',
  ].join('\n');
  fs.writeFileSync(tempConfigPath, content);
  return tempConfigPath;
}

export function createDevLynxConfig(cwd: string, appConfigPath: string, port: number, host?: string): string {
  const tempDir = path.resolve(cwd, '.sparkling');
  fs.mkdirSync(tempDir, { recursive: true });
  const tempConfigPath = path.join(tempDir, 'lynx.dev.config.ts');
  const rel = path.relative(tempDir, path.resolve(appConfigPath)).split(path.sep).join('/');
  const content = [
    `import cfgModule from '${rel.startsWith('.') ? rel : './' + rel}'`,
    'const cfg: any = (cfgModule as any).default ?? cfgModule',
    'const lynxCfg = cfg.lynxConfig ?? cfg',
    `export default { ...lynxCfg, server: { ...lynxCfg.server, port: ${port}, strictPort: true${host ? `, host: ${JSON.stringify(host)}` : ''} } } as any`,
  ].join('\n');
  fs.writeFileSync(tempConfigPath, content);
  return tempConfigPath;
}

function isValidPort(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value > 0 && value <= 65535;
}

function toValidPort(value: unknown): number | undefined {
  if (isValidPort(value)) {
    return value;
  }
  if (typeof value === 'string') {
    const parsed = Number.parseInt(value, 10);
    if (isValidPort(parsed)) {
      return parsed;
    }
  }
  return undefined;
}

export function getConfiguredDevServerPorts(config: AppConfig): { devPort?: number; lynxPort?: number } {
  const devPort = toValidPort((config as { dev?: { server?: { port?: unknown } } }).dev?.server?.port);
  const lynxCfg = (config as { lynxConfig?: unknown }).lynxConfig as { server?: { port?: unknown } } | undefined;
  const lynxPort = toValidPort(lynxCfg?.server?.port);
  return { devPort, lynxPort };
}

export function resolveDevServerPort(config: AppConfig): number {
  const { devPort, lynxPort } = getConfiguredDevServerPorts(config);
  if (devPort !== undefined) {
    return devPort;
  }
  if (lynxPort !== undefined) {
    return lynxPort;
  }
  return DEV_SERVER_PORT;
}

export function updateDevServerPortInAppConfig(configPath: string, port: number): boolean {
  if (!isValidPort(port) || !fs.existsSync(configPath)) {
    return false;
  }

  const original = fs.readFileSync(configPath, 'utf8');

  if (/\bdev\s*:\s*\{[\s\S]*?\bserver\s*:\s*\{[\s\S]*?\bport\s*:/.test(original)) {
    const replaced = original.replace(/(\bdev\s*:\s*\{[\s\S]*?\bserver\s*:\s*\{[\s\S]*?\bport\s*:\s*)\d+/, `$1${port}`);
    if (replaced !== original) {
      fs.writeFileSync(configPath, replaced);
      return true;
    }
    return false;
  }

  if (/\bdev\s*:\s*\{[\s\S]*?\bserver\s*:\s*\{/.test(original)) {
    const replaced = original.replace(/(\bdev\s*:\s*\{[\s\S]*?\bserver\s*:\s*\{)/, `$1\n      port: ${port},`);
    if (replaced !== original) {
      fs.writeFileSync(configPath, replaced);
      return true;
    }
  }

  if (/\bdev\s*:\s*\{/.test(original)) {
    const replaced = original.replace(/(\bdev\s*:\s*\{)/, `$1\n    server: {\n      port: ${port},\n    },`);
    if (replaced !== original) {
      fs.writeFileSync(configPath, replaced);
      return true;
    }
  }

  const configDecl = original.match(/const\s+config\s*(?::[^=]+)?=\s*\{/);
  if (!configDecl || configDecl.index === undefined) {
    return false;
  }

  const insertPos = configDecl.index + configDecl[0].length;
  const after = original.slice(insertPos);
  const nextLineIndent = after.match(/\n(\s*)\S/);
  const indent = nextLineIndent?.[1] ?? '  ';
  const insertion = `\n${indent}dev: {\n${indent}  server: {\n${indent}    port: ${port},\n${indent}  },\n${indent}},`;
  const updated = `${original.slice(0, insertPos)}${insertion}${original.slice(insertPos)}`;
  fs.writeFileSync(configPath, updated);
  return true;
}

export async function loadAppConfig(cwd: string, configFile = 'app.config.ts'): Promise<{ config: AppConfig; configPath: string }> {
  const configPath = path.resolve(cwd, configFile);
  if (!fs.existsSync(configPath)) {
    throw new Error(`App config not found at ${configPath}`);
  }

  // If the project is ESM, jump straight to the ESM loader path.
  if (isEsmProject(cwd)) {
    return loadAppConfigViaEsm(cwd, configPath);
  }

  let loaderPath = configPath;
  if (path.extname(configPath) === '.ts') {
    // Create a temporary CommonJS wrapper to load the TS config reliably even in ESM packages
    const tempDir = path.resolve(cwd, '.sparkling');
    fs.mkdirSync(tempDir, { recursive: true });
    loaderPath = path.join(tempDir, 'app.config.cjs');
    const escaped = path.resolve(configPath);
    const content = [
      'const { register } = require("ts-node");',
      'register({',
      '  transpileOnly: true,',
      '  compilerOptions: {',
      "    module: 'commonjs',",
      "    moduleResolution: 'node',",
      '    esModuleInterop: true,',
      "    jsx: 'react-jsx',",
      '  },',
      '});',
      `const mod = require(${JSON.stringify(escaped)});`,
      'module.exports = mod.default ?? mod;',
    ].join('\n');
    fs.writeFileSync(loaderPath, content);
  }

  // Try CommonJS require first; if the project is ESM, fall back to an ESM loader
  try {
    ensureTsNodeRegistered();
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const mod = require(loaderPath);
    const config = (mod.default ?? mod) as AppConfig;
    if (!config || !config.lynxConfig) {
      throw new Error(`Invalid AppConfig in ${configPath}: missing lynxConfig`);
    }
    return { config, configPath };
  } catch (err) {
    // Fallback to ESM loader
    return loadAppConfigViaEsm(cwd, configPath, err);
  }
}

function loadAppConfigViaEsm(cwd: string, configPath: string, originalError?: unknown): { config: AppConfig; configPath: string } {
  const tempDir = path.resolve(cwd, '.sparkling');
  fs.mkdirSync(tempDir, { recursive: true });
  const readerScript = path.join(tempDir, 'read-app-config.mjs');
  const fileUrl = pathToFileURL(configPath).href;
  const script = [
    `const url = ${JSON.stringify(fileUrl)};`,
    'const mod = await import(url);',
    'const cfg = (mod.default ?? mod);',
    'const out = { lynxConfig: cfg.lynxConfig ?? {}, platform: cfg.platform ?? {}, paths: cfg.paths ?? {}, appName: cfg.appName, devtool: cfg.devtool, dev: cfg.dev };',
    'process.stdout.write(JSON.stringify(out));',
  ].join('\n');
  fs.writeFileSync(readerScript, script);

  // Resolve ts-node/esm loader relative to this package to avoid relying on the app's node_modules
  const esmReq = pkgRequire;
  let esmLoader = 'ts-node/esm';
  try {
    esmLoader = esmReq.resolve('ts-node/esm');
  } catch {}

  const res = spawnSync('node', ['--loader', esmLoader, readerScript], {
    cwd,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: process.env,
  });

  if (res.status !== 0) {
    const stderr = res.stderr?.toString('utf8') ?? '';
    const stdout = res.stdout?.toString('utf8') ?? '';
    const message = [stderr.trim(), stdout.trim(), originalError ? String(originalError) : '']
      .filter(Boolean)
      .join('\n');
    throw new Error(`Failed to load app config via ESM: ${message}`);
  }

  const raw = res.stdout?.toString('utf8') || '{}';
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (jsonErr) {
    throw new Error(`Failed to parse app config JSON: ${String(jsonErr)}\nRaw: ${raw}`);
  }

  if (!parsed || typeof parsed !== 'object') {
    throw new Error('Invalid app config structure: expected an object');
  }

  const config: Partial<AppConfig> = {
    ...(parsed as Record<string, unknown>),
  };

  if (!config.lynxConfig || typeof config.lynxConfig !== 'object') {
    config.lynxConfig = {};
  }

  return { config: config as AppConfig, configPath };
}
