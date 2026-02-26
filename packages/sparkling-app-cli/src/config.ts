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
  const tempConfigPath = path.join(tempDir, 'lynx.config.ts');
  const rel = path.relative(tempDir, path.resolve(appConfigPath)).split(path.sep).join('/');
  const content = [
    `import cfgModule from '${rel.startsWith('.') ? rel : './' + rel}'`,
    'const cfg: any = (cfgModule as any).default ?? cfgModule',
    'export default (cfg.lynxConfig ?? cfg) as any',
  ].join('\n');
  fs.writeFileSync(tempConfigPath, content);
  return tempConfigPath;
}

export function createDevLynxConfig(cwd: string, appConfigPath: string, port: number): string {
  const tempDir = path.resolve(cwd, '.sparkling');
  fs.mkdirSync(tempDir, { recursive: true });
  const tempConfigPath = path.join(tempDir, 'lynx.config.ts');
  const rel = path.relative(tempDir, path.resolve(appConfigPath)).split(path.sep).join('/');
  const content = [
    `import cfgModule from '${rel.startsWith('.') ? rel : './' + rel}'`,
    'const cfg: any = (cfgModule as any).default ?? cfgModule',
    'const lynxCfg = cfg.lynxConfig ?? cfg',
    `export default { ...lynxCfg, server: { ...lynxCfg.server, port: ${port}, strictPort: true } } as any`,
  ].join('\n');
  fs.writeFileSync(tempConfigPath, content);
  return tempConfigPath;
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
    'const out = { lynxConfig: cfg.lynxConfig ?? {}, platform: cfg.platform ?? {}, paths: cfg.paths ?? {}, appName: cfg.appName };',
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
