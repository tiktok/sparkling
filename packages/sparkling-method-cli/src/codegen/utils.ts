// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { ModuleConfig } from './types';

/**
 * Where a generated method's Kotlin lands.
 *
 * The method segment keeps the method's own spelling rather than being
 * lowercased, because that is the package the built-in method packages use and
 * the one `sparkling autolink` builds its registration from:
 * `com.tiktok.sparkling.method.storage.getItem.StorageGetItemMethod`. Folding
 * it to `getitem` put the generated abstract class in a package the concrete
 * implementation could not sit in.
 */
export function buildPackageSegments(config: ModuleConfig, methodName: string): string[] {
  const base = config.packageName.split('.').filter(Boolean);
  const moduleSegment = sanitizePackageSegment(config.moduleName);
  const methodSegment = sanitizeMethodSegment(methodName);
  return [...base, moduleSegment, methodSegment];
}

export function sanitizePackageSegment(value: string): string {
  return value.replace(/[^a-zA-Z0-9]/g, '').toLowerCase() || 'method';
}

/** Like {@link sanitizePackageSegment}, but keeps the method's own casing. */
export function sanitizeMethodSegment(value: string): string {
  return value.replace(/[^a-zA-Z0-9]/g, '') || 'method';
}

export function toPascalCase(value: string): string {
  const parts = splitIntoWords(value);
  if (parts.length === 0) {
    return '';
  }
  return parts.map((chunk) => chunk.charAt(0).toUpperCase() + chunk.slice(1).toLowerCase()).join('');
}

export function splitIntoWords(value: string): string[] {
  if (typeof value !== 'string' || !value) {
    return [];
  }
  const chars = Array.from(value);
  const words: string[] = [];
  let current = '';
  let currentHasLetter = false;
  let prevInWord = '';
  const isLetter = (char: string) => /\p{L}/u.test(char);
  const isNumber = (char: string) => /\p{N}/u.test(char);
  const isUpper = (char: string) => /\p{Lu}/u.test(char);
  const isLower = (char: string) => /\p{Ll}/u.test(char);
  const pushCurrent = () => {
    if (current) {
      words.push(current);
      current = '';
      currentHasLetter = false;
      prevInWord = '';
    }
  };

  for (let i = 0; i < chars.length; i += 1) {
    const char = chars[i];
    if (!isLetter(char) && !isNumber(char)) {
      pushCurrent();
      continue;
    }

    if (!current) {
      current = char;
      currentHasLetter = isLetter(char);
      prevInWord = char;
      continue;
    }

    const nextChar = chars[i + 1] ?? '';
    let boundary = false;

    if (isUpper(char)) {
      if (prevInWord && isUpper(prevInWord) && nextChar && isLower(nextChar)) {
        boundary = true;
      } else if (prevInWord && isLower(prevInWord)) {
        boundary = true;
      } else if (prevInWord && isNumber(prevInWord) && currentHasLetter) {
        boundary = true;
      }
    } else if (isLower(char)) {
      if (prevInWord && isNumber(prevInWord) && currentHasLetter) {
        boundary = true;
      }
    }

    if (boundary) {
      pushCurrent();
      current = char;
      currentHasLetter = isLetter(char);
      prevInWord = char;
      continue;
    }

    current += char;
    if (isLetter(char)) {
      currentHasLetter = true;
    }
    prevInWord = char;
  }

  pushCurrent();
  return words;
}

export function toCamelCase(value: string): string {
  const parts = splitIntoWords(value);
  if (parts.length === 0) {
    return '';
  }
  return parts[0].toLowerCase() + parts.slice(1).map((chunk) =>
    chunk.charAt(0).toUpperCase() + chunk.slice(1).toLowerCase()
  ).join('');
}

export function toKebabCase(value: string): string {
  const parts = splitIntoWords(value);
  if (parts.length === 0) {
    return '';
  }
  return parts.map(part => part.toLowerCase()).join('-');
}

export function mapPrimitiveToTypeScript(kind: string): string {
  switch (kind) {
    case 'string':
      return 'string';
    case 'number':
      return 'number';
    case 'boolean':
      return 'boolean';
    case 'void':
      return 'void';
    case 'object':
      return 'object';
    case 'any':
      return 'any';
    default:
      return 'any';
  }
}

export function mapPrimitiveToJSType(kind: string): string {
  switch (kind) {
    case 'string':
      return 'string';
    case 'number':
      return 'number';
    case 'boolean':
      return 'boolean';
    case 'void':
      return 'undefined';
    case 'object':
      return 'object';
    case 'any':
      return 'undefined';
    default:
      return 'undefined';
  }
}
