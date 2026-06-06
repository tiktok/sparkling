// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import { isEmptyDir, mergeJsonFiles, readJSON, readPackageJson } from '../utils/common';

function tmpDir(prefix = 'csa-common-'): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), prefix));
}

describe('utils/common', () => {
  let work: string;

  beforeEach(() => {
    work = tmpDir();
  });

  afterEach(() => {
    fs.rmSync(work, { recursive: true, force: true });
  });

  test('readJSON parses JSON file', async () => {
    const file = path.join(work, 'a.json');
    fs.writeFileSync(file, '{"a":1}');
    await expect(readJSON(file)).resolves.toEqual({ a: 1 });
  });

  test('readPackageJson reads version from a directory', async () => {
    fs.writeFileSync(path.join(work, 'package.json'), JSON.stringify({ version: '1.2.3' }));
    await expect(readPackageJson(work)).resolves.toEqual({ version: '1.2.3' });
  });

  test('isEmptyDir treats missing dirs as empty', () => {
    expect(isEmptyDir(path.join(work, 'missing'))).toBe(true);
  });

  test('isEmptyDir treats empty dir as empty', () => {
    const sub = path.join(work, 'empty');
    fs.mkdirSync(sub);
    expect(isEmptyDir(sub)).toBe(true);
  });

  test('isEmptyDir treats {.git}-only dir as empty', () => {
    const sub = path.join(work, 'gitonly');
    fs.mkdirSync(sub);
    fs.mkdirSync(path.join(sub, '.git'));
    expect(isEmptyDir(sub)).toBe(true);
  });

  test('isEmptyDir returns false for dir with files', () => {
    const sub = path.join(work, 'has');
    fs.mkdirSync(sub);
    fs.writeFileSync(path.join(sub, 'README'), 'x');
    expect(isEmptyDir(sub)).toBe(false);
  });

  test('mergeJsonFiles is no-op when target is missing', () => {
    const target = path.join(work, 'missing.json');
    const extra = path.join(work, 'extra.json');
    fs.writeFileSync(extra, '{"a":1}');
    mergeJsonFiles(target, extra);
    expect(fs.existsSync(target)).toBe(false);
  });

  test('mergeJsonFiles target keys win over extra keys (target overrides extra)', () => {
    const target = path.join(work, 'target.json');
    const extra = path.join(work, 'extra.json');
    fs.writeFileSync(target, JSON.stringify({ a: 1, c: 3 }));
    fs.writeFileSync(extra, JSON.stringify({ a: 99, b: 2 }));
    mergeJsonFiles(target, extra);
    expect(JSON.parse(fs.readFileSync(target, 'utf8'))).toEqual({ a: 1, b: 2, c: 3 });
  });
});
