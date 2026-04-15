// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
/// <reference types="jest" />
import { ensureDevServerRunning } from '../utils/dev-server';

const spawnMock = jest.fn();

jest.mock('node:child_process', () => ({
  spawn: (...args: unknown[]) => spawnMock(...args),
}));

describe('ensureDevServerRunning', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    spawnMock.mockReset();
    spawnMock.mockReturnValue({ unref: jest.fn() });
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('does not start a new process when dev server is already running', async () => {
    global.fetch = jest.fn().mockResolvedValue({ ok: true }) as unknown as typeof fetch;

    await ensureDevServerRunning('/tmp/project', 5969);

    expect(spawnMock).not.toHaveBeenCalled();
  });

  it('treats non-2xx HTTP responses as running server', async () => {
    global.fetch = jest.fn().mockResolvedValue({ ok: false, status: 404 }) as unknown as typeof fetch;

    await ensureDevServerRunning('/tmp/project', 5969);

    expect(spawnMock).not.toHaveBeenCalled();
  });

  it('starts sparkling-app-cli dev when server is not running', async () => {
    global.fetch = jest
      .fn()
      .mockRejectedValueOnce(new Error('connect ECONNREFUSED'))
      .mockRejectedValueOnce(new Error('connect ECONNREFUSED'))
      .mockResolvedValueOnce({ ok: true }) as unknown as typeof fetch;

    const argvBackup = process.argv.slice();
    process.argv[1] = '/tmp/mock-cli-entry.js';

    await ensureDevServerRunning('/tmp/project', 5969);

    const expectedCommand = process.platform === 'darwin' ? 'script' : process.execPath;
    const expectedArgs = process.platform === 'darwin'
      ? ['-q', '/dev/null', process.execPath, '/tmp/mock-cli-entry.js', 'dev', '--port', '5969', '--host', '127.0.0.1']
      : ['/tmp/mock-cli-entry.js', 'dev', '--port', '5969', '--host', '127.0.0.1'];

    expect(spawnMock).toHaveBeenCalledTimes(1);
    expect(spawnMock).toHaveBeenCalledWith(
      expectedCommand,
      expectedArgs,
      expect.objectContaining({
        cwd: '/tmp/project',
        detached: true,
        stdio: 'ignore',
      }),
    );

    process.argv = argvBackup;
  });
});
