// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import type { AndroidIntentFilter, AndroidIntentFilterData } from '../types';
import { XML_MARKERS, lineStart, removeAllRegions, renderRegion, upsertRegion } from './markers';
import { escapeXmlAttribute, findElement, readAttribute } from './xml';

export const PERMISSIONS_REGION = 'android-permissions';
export const INTENT_FILTERS_REGION = 'android-intent-filters';

const ANDROID_NS = 'android:';

function qualify(value: string, prefix: string): string {
  return value.includes('.') ? value : `${prefix}${value}`;
}

function attribute(name: string, value: string): string {
  return `${ANDROID_NS}${name}="${escapeXmlAttribute(value)}"`;
}

function dataElement(data: AndroidIntentFilterData, indent: string): string {
  const attributes = (Object.entries(data) as [keyof AndroidIntentFilterData, string | undefined][])
    .filter(([, value]) => typeof value === 'string' && value.length > 0)
    .map(([key, value]) => attribute(key, value as string));
  return `${indent}<data ${attributes.join(' ')} />`;
}

function intentFilterLines(filter: AndroidIntentFilter, indent: string): string[] {
  const inner = `${indent}    `;
  const actions = Array.isArray(filter.action) ? filter.action : [filter.action];
  const lines = [
    filter.autoVerify
      ? `${indent}<intent-filter ${attribute('autoVerify', 'true')}>`
      : `${indent}<intent-filter>`,
  ];
  for (const action of actions) {
    lines.push(`${inner}<action ${attribute('name', qualify(action, 'android.intent.action.'))} />`);
  }
  for (const category of filter.categories ?? []) {
    lines.push(
      `${inner}<category ${attribute('name', qualify(category, 'android.intent.category.'))} />`,
    );
  }
  for (const data of filter.data ?? []) {
    lines.push(dataElement(data, inner));
  }
  lines.push(`${indent}</intent-filter>`);
  return lines;
}

/**
 * The activity an intent filter belongs to.
 *
 * Defaults to whichever activity already declares MAIN/LAUNCHER, because that
 * is the one an app's own deep links and file associations have to reach - it
 * is the process entry point. Naming an activity explicitly overrides it.
 */
function locateActivity(content: string, name: string | undefined): { start: number; beforeCloseTag: number; indent: string } | null {
  if (name) {
    const element = findElement(content, 'activity', { name: 'android:name', value: name });
    return element && element.beforeCloseTag >= 0 ? element : null;
  }

  const pattern = /<activity(?=[\s>])/g;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(content)) !== null) {
    const element = findElement(content.slice(match.index), 'activity');
    if (!element || element.beforeCloseTag < 0) {
      continue;
    }
    const body = content.slice(match.index, match.index + element.beforeCloseTag);
    if (body.includes('android.intent.action.MAIN') && body.includes('android.intent.category.LAUNCHER')) {
      return {
        start: match.index,
        beforeCloseTag: match.index + element.beforeCloseTag,
        // Measured against the whole document: the element was found in a
        // slice that starts at the tag, where every tag looks unindented.
        indent: indentOf(content, match.index),
      };
    }
  }
  return null;
}

/** Leading whitespace of the line `index` sits on, or '' if it shares the line. */
function indentOf(content: string, index: number): string {
  const lineStart = content.lastIndexOf('\n', index - 1) + 1;
  const prefix = content.slice(lineStart, index);
  return /^[ \t]*$/.test(prefix) ? prefix : '';
}

export interface ManifestEdit {
  content: string;
  warnings: string[];
}

/** Apply the declarative Android config to an AndroidManifest.xml. */
export function applyAndroidManifest(
  original: string,
  config: { permissions?: string[]; intentFilters?: AndroidIntentFilter[] },
): ManifestEdit {
  const warnings: string[] = [];
  let content = original;

  const manifest = findElement(content, 'manifest');
  if (!manifest) {
    return { content, warnings: ['No <manifest> element found; nothing was applied'] };
  }

  // Permissions sit directly under <manifest>, before <application>. Anchored
  // just after the opening tag so they land in the conventional place whether
  // or not the file already has any.
  const permissions = config.permissions ?? [];
  const declaredOutside = permissions.filter((permission) => {
    const qualified = qualify(permission, 'android.permission.');
    const withoutRegions = removeAllRegions(content, XML_MARKERS);
    return withoutRegions.includes(`"${qualified}"`);
  });
  for (const permission of declaredOutside) {
    warnings.push(
      `${permission} is already declared in AndroidManifest.xml outside a prebuild region; ` +
      'remove it from the manifest or from app.config.ts so it is declared once',
    );
  }

  const permissionIndent = '    ';
  const permissionLines = permissions
    .filter((permission) => !declaredOutside.includes(permission))
    .map((permission) => {
      const qualified = qualify(permission, 'android.permission.');
      return `${permissionIndent}<uses-permission ${attribute('name', qualified)} />`;
    });
  content = upsertRegion(
    content,
    XML_MARKERS,
    PERMISSIONS_REGION,
    renderRegion(XML_MARKERS, PERMISSIONS_REGION, permissionLines, permissionIndent),
    manifest.afterOpenTag + 1,
  );

  // Intent filters go inside an activity, so each target activity gets its own
  // region: one shared region could not be placed in two elements.
  const byActivity = new Map<string | undefined, AndroidIntentFilter[]>();
  for (const filter of config.intentFilters ?? []) {
    const list = byActivity.get(filter.activity) ?? [];
    list.push(filter);
    byActivity.set(filter.activity, list);
  }

  // Regions for activities that no longer have filters have to go, or removing
  // a filter from the config would leave it in the manifest forever.
  content = content.replace(
    /[ \t]*<!-- sparkling:begin\(android-intent-filters[^)]*\) -->[\s\S]*?<!-- sparkling:end\(android-intent-filters[^)]*\) -->[ \t]*\r?\n?/g,
    '',
  );

  for (const [activityName, filters] of byActivity) {
    const activity = locateActivity(content, activityName);
    if (!activity) {
      warnings.push(
        activityName
          ? `Activity ${activityName} not found in AndroidManifest.xml; its intent filters were skipped`
          : 'No launcher activity found in AndroidManifest.xml; intent filters were skipped',
      );
      continue;
    }
    const regionId = activityName
      ? `${INTENT_FILTERS_REGION}:${activityName}`
      : INTENT_FILTERS_REGION;
    const indent = `${activity.indent}    `;
    const lines = filters.flatMap((filter) => intentFilterLines(filter, indent));
    content = upsertRegion(
      content,
      XML_MARKERS,
      regionId,
      renderRegion(XML_MARKERS, regionId, lines, indent),
      lineStart(content, activity.beforeCloseTag),
    );
  }

  return { content, warnings };
}

/** Read an activity's `android:name`, for diagnostics. */
export function activityName(openTag: string): string | null {
  return readAttribute(openTag, 'android:name');
}
