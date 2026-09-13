// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { XML_MARKERS, lineStart, removeAllRegions, renderRegion, upsertRegion } from './markers';
import { escapeXmlText, findElement } from './xml';

/**
 * Property lists, written into a marked region of the root dict.
 *
 * An Info.plist is a file the developer edits too - it carries the bundle name,
 * the orientations, whatever Xcode put there - so prebuild adds its keys inside
 * markers rather than rewriting the document. A key the developer wrote
 * themselves is left alone and reported, because two declarations of the same
 * key in one dict is a file Xcode reads unpredictably.
 */

export type PlistValue = unknown;

function serialize(value: PlistValue, indent: string): string[] {
  if (typeof value === 'boolean') {
    return [`${indent}<${value}/>`];
  }
  if (typeof value === 'number') {
    return Number.isInteger(value)
      ? [`${indent}<integer>${value}</integer>`]
      : [`${indent}<real>${value}</real>`];
  }
  if (typeof value === 'string') {
    return [`${indent}<string>${escapeXmlText(value)}</string>`];
  }
  if (Array.isArray(value)) {
    if (value.length === 0) {
      return [`${indent}<array/>`];
    }
    return [
      `${indent}<array>`,
      ...value.flatMap((item) => serialize(item, `${indent}\t`)),
      `${indent}</array>`,
    ];
  }
  if (value && typeof value === 'object') {
    const entries = Object.entries(value as Record<string, PlistValue>);
    if (entries.length === 0) {
      return [`${indent}<dict/>`];
    }
    return [
      `${indent}<dict>`,
      ...entries.flatMap(([key, item]) => [
        `${indent}\t<key>${escapeXmlText(key)}</key>`,
        ...serialize(item, `${indent}\t`),
      ]),
      `${indent}</dict>`,
    ];
  }
  // `null` and `undefined` have no plist spelling; the caller filters them out.
  return [`${indent}<string></string>`];
}

export interface PlistEdit {
  content: string;
  warnings: string[];
}

/**
 * Merge `values` into the root dict of a plist document, inside `regionId`.
 *
 * An empty `values` still writes an empty region, so that removing the last key
 * from the config removes it from the file.
 */
export function applyPlist(
  original: string,
  regionId: string,
  values: Record<string, PlistValue>,
  label: string,
): PlistEdit {
  const warnings: string[] = [];
  const root = findElement(original, 'dict');
  if (!root || root.beforeCloseTag < 0) {
    return { content: original, warnings: [`No root <dict> in ${label}; nothing was applied`] };
  }

  const outside = removeAllRegions(original, XML_MARKERS);
  const keys = Object.keys(values).filter((key) => values[key] !== undefined && values[key] !== null);
  for (const key of keys) {
    if (outside.includes(`<key>${key}</key>`)) {
      warnings.push(
        `${label} already declares ${key} outside a prebuild region; ` +
        'remove it from the file or from app.config.ts so it is declared once',
      );
    }
  }

  const indent = '\t';
  const lines = keys
    .filter((key) => !outside.includes(`<key>${key}</key>`))
    .flatMap((key) => [`${indent}<key>${escapeXmlText(key)}</key>`, ...serialize(values[key], indent)]);

  const content = upsertRegion(
    original,
    XML_MARKERS,
    regionId,
    renderRegion(XML_MARKERS, regionId, lines, indent),
    lineStart(original, root.beforeCloseTag),
  );
  return { content, warnings };
}

/** An empty plist document, for creating an entitlements file that is absent. */
export function emptyPlist(): string {
  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">',
    '<plist version="1.0">',
    '<dict>',
    '</dict>',
    '</plist>',
    '',
  ].join('\n');
}

/** `CFBundleURLTypes` for a list of custom schemes. */
export function urlTypes(schemes: string[], bundleId: string): unknown[] {
  return schemes.map((scheme) => ({
    CFBundleURLName: `${bundleId}.${scheme}`,
    CFBundleURLSchemes: [scheme],
  }));
}
