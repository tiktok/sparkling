// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { resolveRspeedyCommand } from '../utils/rspeedy';

describe('resolveRspeedyCommand', () => {
  it('resolves the JavaScript bin from package metadata without losing spaces', () => {
    const packageDir = fs.mkdtempSync(path.join(os.tmpdir(), 'fake rspeedy package '));
    const manifestPath = path.join(packageDir, 'package.json');
    const binPath = path.join(packageDir, 'bin', 'custom-rspeedy.js');
    fs.mkdirSync(path.dirname(binPath));
    fs.writeFileSync(binPath, '');
    fs.writeFileSync(manifestPath, JSON.stringify({
      bin: {
        rspeedy: './bin/custom-rspeedy.js',
      },
    }));

    expect(resolveRspeedyCommand(manifestPath)).toEqual({
      command: process.execPath,
      args: [binPath],
    });
  });

  it('rejects package metadata without a valid rspeedy bin', () => {
    const packageDir = fs.mkdtempSync(path.join(os.tmpdir(), 'fake-rspeedy-'));
    const manifestPath = path.join(packageDir, 'package.json');
    fs.writeFileSync(manifestPath, JSON.stringify({ bin: { rspeedy: '' } }));

    expect(() => resolveRspeedyCommand(manifestPath)).toThrow('bin.rspeedy');
  });
});
