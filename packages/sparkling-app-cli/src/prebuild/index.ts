// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import path from 'node:path';
import fs from 'fs-extra';

import { loadAppConfig } from '../config';
import type { AppConfig, ConfigPlugin } from '../types';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';
import { applyAndroidManifest } from './android';
import { generateAppIcons } from './app-icon';
import { XML_MARKERS, removeAllRegions } from './markers';
import { applyPlist, emptyPlist, urlTypes } from './plist';
import { plistMods, textMods } from './mods';

export interface PrebuildOptions {
  cwd: string;
  /** An already-loaded config, so a caller that has one does not load it twice. */
  config?: AppConfig;
  platform?: 'android' | 'ios' | 'all';
  /** Report what would change and fail instead of writing. */
  check?: boolean;
  /** Strip every prebuild region and write nothing new. */
  revert?: boolean;
}

export interface PrebuildResult {
  /** Files whose contents differ from what is on disk. */
  changed: string[];
  warnings: string[];
}

const ANDROID_MANIFEST = 'android/app/src/main/AndroidManifest.xml';
const IOS_INFO_PLIST = 'ios/Info.plist';
const IOS_ENTITLEMENTS = 'ios/SparklingGo/SparklingGo/SparklingGo.entitlements';

const INFO_PLIST_REGION = 'ios-info-plist';
const ENTITLEMENTS_REGION = 'ios-entitlements';

/**
 * Resolve a plugin entry to a function.
 *
 * A string is a module name or a path relative to the project; a tuple carries
 * options; a function is used as-is. Resolution failures are reported by name
 * rather than swallowed, because a plugin that silently does not run produces a
 * project that builds and is missing a permission.
 */
async function resolvePlugin(
  cwd: string,
  entry: string | [string, Record<string, unknown>] | ConfigPlugin,
): Promise<{ plugin: ConfigPlugin; options?: Record<string, unknown>; name: string }> {
  if (typeof entry === 'function') {
    return { plugin: entry, name: entry.name || '<inline>' };
  }
  if (entry === null || entry === undefined) {
    // An inline function does not survive the JSON round trip the ESM config
    // loader makes, and lands here as null. Naming the module is the fix.
    throw new Error(
      'A config plugin was written as an inline function in an ESM app.config.ts, where it ' +
      'cannot survive being loaded. Move it to its own file and name it: ' +
      "plugins: ['./plugins/with-thing']",
    );
  }
  const [specifier, options] = Array.isArray(entry) ? entry : [entry, undefined];
  const candidates = specifier.startsWith('.')
    ? [path.resolve(cwd, specifier)]
    : [specifier, path.resolve(cwd, 'node_modules', specifier)];

  let lastError: unknown;
  for (const candidate of candidates) {
    try {
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const loaded = require(candidate);
      const plugin = (loaded.default ?? loaded) as ConfigPlugin;
      if (typeof plugin !== 'function') {
        throw new Error('does not export a function');
      }
      return { plugin, options, name: specifier };
    } catch (error) {
      lastError = error;
    }
  }
  throw new Error(`Could not load config plugin "${specifier}": ${(lastError as Error)?.message}`);
}

async function applyPlugins(cwd: string, config: AppConfig): Promise<AppConfig> {
  let current = config;
  for (const entry of config.plugins ?? []) {
    const { plugin, options, name } = await resolvePlugin(cwd, entry);
    if (isVerboseEnabled()) {
      verboseLog(`Applying config plugin ${name}`);
    }
    current = await plugin(current, options);
  }
  return current;
}

interface FileEdit {
  filePath: string;
  next: string;
  warnings: string[];
}

async function androidEdits(cwd: string, config: AppConfig, revert: boolean): Promise<FileEdit[]> {
  const filePath = path.resolve(cwd, ANDROID_MANIFEST);
  if (!fs.existsSync(filePath)) {
    return [];
  }
  const original = await fs.readFile(filePath, 'utf8');

  if (revert) {
    return [{ filePath, next: removeAllRegions(original, XML_MARKERS), warnings: [] }];
  }

  const applied = applyAndroidManifest(original, {
    permissions: config.android?.permissions,
    intentFilters: config.android?.intentFilters,
  });

  let next = applied.content;
  const identifier = config.platform?.android?.packageName ?? '';
  for (const mod of textMods(config, 'androidManifest')) {
    next = await mod(next, { cwd, filePath, identifier });
  }
  return [{ filePath, next, warnings: applied.warnings }];
}

async function iosEdits(cwd: string, config: AppConfig, revert: boolean): Promise<FileEdit[]> {
  const edits: FileEdit[] = [];
  const identifier = config.platform?.ios?.bundleIdentifier ?? '';

  const infoPath = path.resolve(cwd, IOS_INFO_PLIST);
  if (fs.existsSync(infoPath)) {
    const original = await fs.readFile(infoPath, 'utf8');
    if (revert) {
      edits.push({ filePath: infoPath, next: removeAllRegions(original, XML_MARKERS), warnings: [] });
    } else {
      let values: Record<string, unknown> = { ...(config.ios?.infoPlist ?? {}) };
      const schemes = config.ios?.urlSchemes ?? [];
      if (schemes.length > 0 && values.CFBundleURLTypes === undefined) {
        values.CFBundleURLTypes = urlTypes(schemes, identifier || 'app');
      }
      for (const mod of plistMods(config, 'infoPlist')) {
        values = mod(values, { cwd, filePath: infoPath, identifier });
      }
      const applied = applyPlist(original, INFO_PLIST_REGION, values, 'Info.plist');
      let next = applied.content;
      for (const mod of textMods(config, 'infoPlist')) {
        next = await mod(next, { cwd, filePath: infoPath, identifier });
      }
      edits.push({ filePath: infoPath, next, warnings: applied.warnings });
    }
  }

  const entitlementValues: Record<string, unknown> = { ...(config.ios?.entitlements ?? {}) };
  const entitlementMods = plistMods(config, 'entitlements');
  const wantsEntitlements = Object.keys(entitlementValues).length > 0 || entitlementMods.length > 0;
  const entitlementsPath = path.resolve(cwd, IOS_ENTITLEMENTS);
  const entitlementsExist = fs.existsSync(entitlementsPath);

  if (revert) {
    if (entitlementsExist) {
      const original = await fs.readFile(entitlementsPath, 'utf8');
      edits.push({
        filePath: entitlementsPath,
        next: removeAllRegions(original, XML_MARKERS),
        warnings: [],
      });
    }
  } else if (wantsEntitlements || entitlementsExist) {
    const original = entitlementsExist
      ? await fs.readFile(entitlementsPath, 'utf8')
      : emptyPlist();
    let values = entitlementValues;
    for (const mod of entitlementMods) {
      values = mod(values, { cwd, filePath: entitlementsPath, identifier });
    }
    const applied = applyPlist(original, ENTITLEMENTS_REGION, values, 'entitlements');
    const warnings = [...applied.warnings];
    if (!entitlementsExist) {
      warnings.push(
        `Created ${IOS_ENTITLEMENTS}. Add it to the Xcode target's CODE_SIGN_ENTITLEMENTS ` +
        'build setting, which prebuild does not do for you.',
      );
    }
    edits.push({ filePath: entitlementsPath, next: applied.content, warnings });
  }

  return edits;
}

/**
 * Apply app.config.ts and its plugins to the native projects.
 *
 * The projects are source, so this writes into marked regions and leaves
 * everything else alone: running it twice produces the same file as running it
 * once, and `revert` returns the projects to what the developer wrote.
 */
export async function prebuild(options: PrebuildOptions): Promise<PrebuildResult> {
  const { cwd } = options;
  const platform = options.platform ?? 'all';
  const loaded = options.config ?? (await loadAppConfig(cwd)).config;
  const config = options.revert ? loaded : await applyPlugins(cwd, loaded);

  const edits: FileEdit[] = [];
  if (platform === 'android' || platform === 'all') {
    edits.push(...(await androidEdits(cwd, config, options.revert === true)));
  }
  if (platform === 'ios' || platform === 'all') {
    edits.push(...(await iosEdits(cwd, config, options.revert === true)));
  }

  const changed: string[] = [];
  const warnings: string[] = [];

  // Icons are generated files rather than regions in a file the developer owns,
  // so they are written outside the marker mechanism - and not written at all on
  // a revert, where the point is to leave the projects as they were.
  if (!options.revert) {
    const icons = await generateAppIcons(cwd, config, platform, options.check === true);
    warnings.push(...icons.warnings);
    changed.push(...icons.written);
  }
  for (const edit of edits) {
    warnings.push(...edit.warnings);
    const current = fs.existsSync(edit.filePath) ? await fs.readFile(edit.filePath, 'utf8') : null;
    if (current === edit.next) {
      continue;
    }
    changed.push(path.relative(cwd, edit.filePath));
    if (!options.check) {
      await fs.ensureDir(path.dirname(edit.filePath));
      await fs.writeFile(edit.filePath, edit.next, 'utf8');
    }
  }

  for (const warning of warnings) {
    console.warn(ui.warn(warning));
  }

  if (options.check) {
    if (changed.length > 0) {
      console.error(
        ui.error(
          `prebuild --check: ${changed.length} file(s) would change: ${changed.join(', ')}`,
        ),
      );
    } else {
      console.log(ui.success('prebuild --check: native projects match app.config.ts'));
    }
  } else if (changed.length > 0) {
    console.log(ui.success(`prebuild: updated ${changed.join(', ')}`));
  } else {
    console.log(ui.success('prebuild: native projects already up to date'));
  }

  return { changed, warnings };
}
