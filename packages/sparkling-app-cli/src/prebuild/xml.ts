// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * Just enough XML to find an anchor.
 *
 * Prebuild does not rewrite these documents - it inserts marked regions into
 * them (see markers.ts) - so it needs to locate an element and the indentation
 * around it, not to parse a tree. A real parser would mean a dependency and a
 * round-trip that reformats the developer's own file, which is exactly what
 * must not happen to a file they maintain by hand.
 */

export interface ElementRange {
  /** Index of the `<` that opens the start tag. */
  start: number;
  /** Index just past the `>` that closes the start tag. */
  afterOpenTag: number;
  /** Index of the `<` that opens the end tag, or -1 for a self-closing element. */
  beforeCloseTag: number;
  /** Indentation of the line the start tag is on. */
  indent: string;
  selfClosing: boolean;
}

function indentAt(content: string, index: number): string {
  const lineStart = content.lastIndexOf('\n', index - 1) + 1;
  const line = content.slice(lineStart, index);
  return /^[ \t]*$/.test(line) ? line : '';
}

/**
 * Find an element by tag name, optionally requiring an attribute value.
 *
 * Scans start tags rather than matching a regex across the whole document, so a
 * tag name that appears inside an attribute value or a comment cannot be
 * mistaken for an element.
 */
export function findElement(
  content: string,
  tagName: string,
  requiredAttribute?: { name: string; value: string },
): ElementRange | null {
  const search = new RegExp(`<${tagName}(?=[\\s>/])`, 'g');
  let match: RegExpExecArray | null;

  while ((match = search.exec(content)) !== null) {
    const start = match.index;
    const afterOpenTag = endOfStartTag(content, start);
    if (afterOpenTag < 0) {
      return null;
    }
    const openTag = content.slice(start, afterOpenTag);
    const selfClosing = /\/>$/.test(openTag.trim());

    if (requiredAttribute) {
      const value = readAttribute(openTag, requiredAttribute.name);
      if (value !== requiredAttribute.value) {
        continue;
      }
    }

    return {
      start,
      afterOpenTag,
      beforeCloseTag: selfClosing ? -1 : findCloseTag(content, tagName, afterOpenTag),
      indent: indentAt(content, start),
      selfClosing,
    };
  }
  return null;
}

/** Index just past the `>` that ends the start tag beginning at `start`. */
function endOfStartTag(content: string, start: number): number {
  let quote: string | null = null;
  for (let i = start; i < content.length; i += 1) {
    const c = content[i];
    if (quote) {
      if (c === quote) {
        quote = null;
      }
      continue;
    }
    if (c === '"' || c === "'") {
      quote = c;
      continue;
    }
    if (c === '>') {
      return i + 1;
    }
  }
  return -1;
}

/** Index of the `<` that opens the matching end tag, accounting for nesting. */
function findCloseTag(content: string, tagName: string, from: number): number {
  const pattern = new RegExp(`<(/?)${tagName}(?=[\\s>/])`, 'g');
  pattern.lastIndex = from;
  let depth = 1;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(content)) !== null) {
    if (match[1] === '/') {
      depth -= 1;
      if (depth === 0) {
        return match.index;
      }
      continue;
    }
    const tagEnd = endOfStartTag(content, match.index);
    const openTag = content.slice(match.index, tagEnd);
    if (!/\/>$/.test(openTag.trim())) {
      depth += 1;
    }
  }
  return -1;
}

export function readAttribute(openTag: string, name: string): string | null {
  const pattern = new RegExp(`\\s${name.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&')}\\s*=\\s*("([^"]*)"|'([^']*)')`);
  const match = pattern.exec(openTag);
  if (!match) {
    return null;
  }
  return match[2] ?? match[3] ?? null;
}

export function escapeXmlAttribute(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export function escapeXmlText(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
