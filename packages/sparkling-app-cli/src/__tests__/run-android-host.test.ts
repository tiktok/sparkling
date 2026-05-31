// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
/// <reference types="jest" />
import os from 'node:os';
import { runAndroid } from '../commands/run-android';
import { loadAppConfig } from '../config';
import { ensureDevServerRunning } from '../utils/dev-server';
import { runCommand } from '../utils/exec';

const mockExecSync = jest.fn();

jest.mock('node:child_process', () => {
  const actual = jest.requireActual('node:child_process');
  return {
    ...actual,
    execSync: (...args: unknown[]) => mockExecSync(...args),
  };
});

jest.mock('../commands/autolink', () => ({
  autolink: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../commands/build', () => ({
  buildProject: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../config', () => {
  const actual = jest.requireActual('../config');
  return {
    ...actual,
    loadAppConfig: jest.fn(),
  };
});

jest.mock('../utils/dev-server', () => ({
  ensureDevServerRunning: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../utils/exec', () => ({
  runCommand: jest.fn().mockResolvedValue(undefined),
}));

describe('runAndroid dev server host selection', () => {
  const networkSpy = jest.spyOn(os, 'networkInterfaces');

  beforeEach(() => {
    jest.clearAllMocks();
    mockExecSync.mockReturnValue('List of devices attached\nR58M1234\tdevice\n');
    networkSpy.mockReturnValue({
      en0: [{ family: 'IPv4', internal: false, address: '192.168.1.25' }],
    } as never);
    (loadAppConfig as jest.Mock).mockResolvedValue({
      config: { lynxConfig: {} },
    });
  });

  afterAll(() => {
    networkSpy.mockRestore();
  });

  it('binds physical-device dev servers to the resolved LAN IPv4 instead of 0.0.0.0', async () => {
    await runAndroid({ cwd: '/tmp/project' });

    expect(ensureDevServerRunning).toHaveBeenCalledWith('/tmp/project', 5969, '192.168.1.25');
    expect(runCommand).toHaveBeenCalledWith(
      'gradlew',
      expect.arrayContaining(['-PsparklingDevServerHost=192.168.1.25']),
      expect.any(Object),
    );
    expect(ensureDevServerRunning).not.toHaveBeenCalledWith('/tmp/project', 5969, '0.0.0.0');
  });

  it('keeps emulator dev servers on localhost and applies adb reverse', async () => {
    mockExecSync.mockReturnValue('List of devices attached\nemulator-5554\tdevice\n');

    await runAndroid({ cwd: '/tmp/project' });

    expect(ensureDevServerRunning).toHaveBeenCalledWith('/tmp/project', 5969, '127.0.0.1');
    expect(runCommand).toHaveBeenCalledWith(
      'adb',
      ['-s', 'emulator-5554', 'reverse', 'tcp:5969', 'tcp:5969'],
      expect.objectContaining({ cwd: '/tmp/project', ignoreFailure: true }),
    );
  });

  it('lets --host override app config and automatic LAN detection', async () => {
    (loadAppConfig as jest.Mock).mockResolvedValue({
      config: { lynxConfig: {}, dev: { server: { host: '192.168.9.9' } } },
    });

    await runAndroid({ cwd: '/tmp/project', host: '10.1.2.3' });

    expect(ensureDevServerRunning).toHaveBeenCalledWith('/tmp/project', 5969, '10.1.2.3');
    expect(runCommand).toHaveBeenCalledWith(
      'gradlew',
      expect.arrayContaining(['-PsparklingDevServerHost=10.1.2.3']),
      expect.any(Object),
    );
  });

  it('uses app config dev.server.host before automatic LAN detection', async () => {
    (loadAppConfig as jest.Mock).mockResolvedValue({
      config: { lynxConfig: {}, dev: { server: { host: '192.168.9.9' } } },
    });

    await runAndroid({ cwd: '/tmp/project' });

    expect(ensureDevServerRunning).toHaveBeenCalledWith('/tmp/project', 5969, '192.168.9.9');
    expect(runCommand).toHaveBeenCalledWith(
      'gradlew',
      expect.arrayContaining(['-PsparklingDevServerHost=192.168.9.9']),
      expect.any(Object),
    );
  });

  it('warns when an explicit host exposes all network interfaces', async () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

    await runAndroid({ cwd: '/tmp/project', host: '0.0.0.0' });

    expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('0.0.0.0'));

    warnSpy.mockRestore();
  });
});
