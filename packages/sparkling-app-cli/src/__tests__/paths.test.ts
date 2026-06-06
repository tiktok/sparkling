// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'node:path';

import { relativeTo, resolveProjectPath, toPosixPath } from '../utils/paths';

describe('utils/paths', () => {
  describe('resolveProjectPath', () => {
    test('joins valid segments under cwd', () => {
      const out = resolveProjectPath('/proj', 'a', 'b', 'c.txt');
      expect(out).toBe(path.resolve('/proj', 'a', 'b', 'c.txt'));
    });

    test('drops empty segments', () => {
      const out = resolveProjectPath('/proj', '', 'a', '');
      expect(out).toBe(path.resolve('/proj', 'a'));
    });

    test('throws when cwd is empty or non-string', () => {
      expect(() => resolveProjectPath('')).toThrow(/cwd must be a non-empty string/);
      // @ts-expect-error testing runtime guard
      expect(() => resolveProjectPath(null)).toThrow(/cwd must be a non-empty string/);
    });
  });

  describe('toPosixPath', () => {
    test('returns empty string for falsy input', () => {
      expect(toPosixPath('')).toBe('');
      // @ts-expect-error testing runtime guard
      expect(toPosixPath(null)).toBe('');
    });

    test('converts platform separators to forward slash', () => {
      const input = ['a', 'b', 'c'].join(path.sep);
      expect(toPosixPath(input)).toBe('a/b/c');
    });
  });

  describe('relativeTo', () => {
    test('returns empty string when target is missing', () => {
      expect(relativeTo('/a', '')).toBe('');
    });

    test('returns posix-converted absolute target when from is missing', () => {
      const target = ['', 'abs', 'path'].join(path.sep);
      expect(relativeTo('', target)).toBe(toPosixPath(target));
    });

    test('produces posix relative path', () => {
      const out = relativeTo('/proj', '/proj/sub/file.txt');
      expect(out).toBe('sub/file.txt');
    });
  });
});
