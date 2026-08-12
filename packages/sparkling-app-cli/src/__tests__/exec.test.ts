// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
/// <reference types="jest" />
import { EventEmitter } from 'node:events';
import { runCommand } from '../utils/exec';

const mockSpawn = jest.fn();

jest.mock('node:child_process', () => ({
  spawn: (...args: unknown[]) => mockSpawn(...args),
}));

describe('runCommand', () => {
  it('keeps generic commands shell-free', async () => {
    const child = new EventEmitter();
    mockSpawn.mockReturnValue(child);
    process.nextTick(() => child.emit('close', 0));

    await runCommand('adb', ['devices']);

    expect(mockSpawn).toHaveBeenCalledWith(
      'adb',
      ['devices'],
      expect.objectContaining({ shell: false }),
    );
  });
});
