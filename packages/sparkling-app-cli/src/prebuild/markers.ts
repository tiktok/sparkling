// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * Marked regions inside a file the developer also owns.
 *
 * Sparkling's native projects are source, not build output: they are created
 * once from the template, committed, and then edited by hand. Prebuild can
 * therefore not regenerate them the way Expo does - it has to write into files
 * that already contain someone else's work, and keep doing so on every run
 * without accumulating.
 *
 * The rule is that prebuild owns the text between its own markers and nothing
 * else. Writing a region replaces whatever was between them last time; reverting
 * removes the markers and their contents; everything outside is untouched.
 */

export interface MarkerSyntax {
  /** Rendered as `${open}sparkling:begin(id)${close}`. */
  open: string;
  close: string;
}

export const XML_MARKERS: MarkerSyntax = { open: '<!-- ', close: ' -->' };
export const HASH_MARKERS: MarkerSyntax = { open: '# ', close: '' };

function escapeForRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function beginMarker(syntax: MarkerSyntax, id: string): string {
  return `${syntax.open}sparkling:begin(${id})${syntax.close}`;
}

export function endMarker(syntax: MarkerSyntax, id: string): string {
  return `${syntax.open}sparkling:end(${id})${syntax.close}`;
}

/** Matches a whole region including its markers and the newline after it. */
function regionPattern(syntax: MarkerSyntax, id: string): RegExp {
  const begin = escapeForRegExp(beginMarker(syntax, id));
  const end = escapeForRegExp(endMarker(syntax, id));
  return new RegExp(`[ \\t]*${begin}[\\s\\S]*?${end}[ \\t]*\\r?\\n?`, 'g');
}

/** Every region prebuild has ever written, whatever its id. */
function anyRegionPattern(syntax: MarkerSyntax): RegExp {
  const begin = escapeForRegExp(`${syntax.open}sparkling:begin(`);
  const end = escapeForRegExp(`${syntax.open}sparkling:end(`);
  const close = escapeForRegExp(syntax.close);
  return new RegExp(
    `[ \\t]*${begin}[^)]*\\)${close}[\\s\\S]*?${end}[^)]*\\)${close}[ \\t]*\\r?\\n?`,
    'g',
  );
}

export function hasRegion(content: string, syntax: MarkerSyntax, id: string): boolean {
  return regionPattern(syntax, id).test(content);
}

export function removeRegion(content: string, syntax: MarkerSyntax, id: string): string {
  return content.replace(regionPattern(syntax, id), '');
}

export function removeAllRegions(content: string, syntax: MarkerSyntax): string {
  return content.replace(anyRegionPattern(syntax), '');
}

/**
 * Render a region. `lines` are written verbatim, each already indented by the
 * caller; an empty list produces an empty region rather than no region, so that
 * removing every permission from the config still removes them from the file.
 */
export function renderRegion(
  syntax: MarkerSyntax,
  id: string,
  lines: string[],
  indent: string,
): string {
  return [
    `${indent}${beginMarker(syntax, id)}`,
    ...lines,
    `${indent}${endMarker(syntax, id)}`,
    '',
  ].join('\n');
}

/**
 * The start of the line `index` sits on.
 *
 * Regions are inserted here rather than at the index itself, so that the
 * indentation already in front of the element we are inserting before stays in
 * front of that element instead of ending up in front of our first marker.
 */
export function lineStart(content: string, index: number): number {
  return content.lastIndexOf('\n', Math.max(0, index - 1)) + 1;
}

/**
 * Put `region` in the file: replacing the existing one if this id has been
 * written before, otherwise inserting it at `anchorIndex`.
 */
export function upsertRegion(
  content: string,
  syntax: MarkerSyntax,
  id: string,
  region: string,
  anchorIndex: number,
): string {
  if (hasRegion(content, syntax, id)) {
    let replaced = false;
    return content.replace(regionPattern(syntax, id), () => {
      if (replaced) {
        // A duplicated region can only come from a hand edit; collapse it.
        return '';
      }
      replaced = true;
      return region;
    });
  }
  return content.slice(0, anchorIndex) + region + content.slice(anchorIndex);
}
