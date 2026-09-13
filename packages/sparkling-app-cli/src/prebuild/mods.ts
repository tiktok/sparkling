// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import type { AppConfig } from '../types';

/**
 * The mod API a config plugin uses.
 *
 * A plugin never writes a file. It registers a transform against a named file,
 * and every transform for that file runs in registration order after all
 * plugins have been applied, each receiving the result of the one before it.
 * The file is written once, at the end. That is what lets two plugins touch the
 * same Info.plist without either of them losing.
 */

export type ModTarget =
  | 'androidManifest'
  | 'infoPlist'
  | 'entitlements'
  | 'podfile'
  | 'appBuildGradle'
  | 'gradleProperties';

export interface ModContext {
  /** Project root. */
  cwd: string;
  /** Absolute path of the file being modified. */
  filePath: string;
  /** Android package name / iOS bundle identifier, as configured. */
  identifier: string;
}

export type TextMod = (contents: string, context: ModContext) => string | Promise<string>;

export interface PlistMod {
  (values: Record<string, unknown>, context: ModContext): Record<string, unknown>;
}

interface ModRegistry {
  text: Partial<Record<ModTarget, TextMod[]>>;
  plist: Partial<Record<'infoPlist' | 'entitlements', PlistMod[]>>;
}

const REGISTRY = new WeakMap<AppConfig, ModRegistry>();

function registryFor(config: AppConfig): ModRegistry {
  let registry = REGISTRY.get(config);
  if (!registry) {
    registry = { text: {}, plist: {} };
    REGISTRY.set(config, registry);
  }
  return registry;
}

export function textMods(config: AppConfig, target: ModTarget): TextMod[] {
  return REGISTRY.get(config)?.text[target] ?? [];
}

export function plistMods(config: AppConfig, target: 'infoPlist' | 'entitlements'): PlistMod[] {
  return REGISTRY.get(config)?.plist[target] ?? [];
}

function addTextMod(config: AppConfig, target: ModTarget, mod: TextMod): AppConfig {
  const registry = registryFor(config);
  registry.text[target] = [...(registry.text[target] ?? []), mod];
  return config;
}

/**
 * Modify the values prebuild merges into Info.plist.
 *
 * Structured rather than textual: the values are merged into the marked region
 * as a unit, so two plugins adding different keys compose instead of one
 * overwriting the other's text.
 */
export function withInfoPlist(
  config: AppConfig,
  mod: PlistMod,
): AppConfig {
  const registry = registryFor(config);
  registry.plist.infoPlist = [...(registry.plist.infoPlist ?? []), mod];
  return config;
}

export function withEntitlements(config: AppConfig, mod: PlistMod): AppConfig {
  const registry = registryFor(config);
  registry.plist.entitlements = [...(registry.plist.entitlements ?? []), mod];
  return config;
}

export function withAndroidManifest(config: AppConfig, mod: TextMod): AppConfig {
  return addTextMod(config, 'androidManifest', mod);
}

export function withPodfile(config: AppConfig, mod: TextMod): AppConfig {
  return addTextMod(config, 'podfile', mod);
}

export function withAppBuildGradle(config: AppConfig, mod: TextMod): AppConfig {
  return addTextMod(config, 'appBuildGradle', mod);
}

export function withGradleProperties(config: AppConfig, mod: TextMod): AppConfig {
  return addTextMod(config, 'gradleProperties', mod);
}

/**
 * Modify a file prebuild has no structured knowledge of.
 *
 * Named to be uncomfortable: the transform gets raw text with no guarantee that
 * another plugin has not already rewritten it, and it is on the plugin to be
 * idempotent. Use a marked region (see markers.ts) rather than appending.
 */
export function withDangerousMod(
  config: AppConfig,
  target: ModTarget,
  mod: TextMod,
): AppConfig {
  return addTextMod(config, target, mod);
}
