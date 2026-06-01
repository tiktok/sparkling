// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import { isDirEmpty } from '../create-app/fs-utils';

describe('create-app/fs-utils', () => {
  let work: string;
  beforeEach(() => {
    work = fs.mkdtempSync(path.join(os.tmpdir(), 'csa-fs-'));
  });
  afterEach(() => {
    fs.rmSync(work, { recursive: true, force: true });
  });

  test('isDirEmpty returns true for empty dir', () => {
    expect(isDirEmpty(work)).toBe(true);
  });

  test('isDirEmpty returns false for non-empty dir', () => {
    fs.writeFileSync(path.join(work, 'file.txt'), 'x');
    expect(isDirEmpty(work)).toBe(false);
  });

  test('isDirEmpty returns true for non-existent path (defensively)', () => {
    expect(isDirEmpty(path.join(work, 'never-existed'))).toBe(true);
  });
});
