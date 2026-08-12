// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
/// <reference types="jest" />
import { EventEmitter } from 'node:events';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { buildProject } from '../commands/build';
import { devProject } from '../commands/dev';

const fakeRspeedyBin = path.join(os.tmpdir(), 'fake rspeedy package', 'bin', 'rspeedy.js');
const mockRunCommand = jest.fn().mockResolvedValue(undefined);
const mockResolveRspeedyCommand = jest.fn();
const mockSpawn = jest.fn();

jest.mock('../utils/exec', () => ({
  runCommand: (...args: unknown[]) => mockRunCommand(...args),
}));

jest.mock('../utils/rspeedy', () => ({
  resolveRspeedyCommand: () => mockResolveRspeedyCommand(),
}));

jest.mock('node:child_process', () => {
  const actual = jest.requireActual('node:child_process');
  return {
    ...actual,
    spawn: (...args: unknown[]) => mockSpawn(...args),
  };
});

function createClosingChild(): EventEmitter & { kill: jest.Mock } {
  const child = new EventEmitter() as EventEmitter & { kill: jest.Mock };
  child.kill = jest.fn();
  process.nextTick(() => child.emit('close', 0));
  return child;
}

function createApp(): { cwd: string } {
  const cwd = fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling app '));
  const configPath = path.join(cwd, 'app.config.js');
  fs.writeFileSync(configPath, 'module.exports = { lynxConfig: {} };\n');
  return { cwd };
}

describe('Rspeedy command invocation', () => {
  beforeEach(() => {
    mockRunCommand.mockClear();
    mockResolveRspeedyCommand.mockReset().mockReturnValue({
      command: process.execPath,
      args: [fakeRspeedyBin],
    });
    mockSpawn.mockReset();
    mockSpawn.mockReturnValue(createClosingChild());
  });

  it('build invokes the resolved JavaScript bin with Node and separate arguments', async () => {
    const { cwd } = createApp();

    await buildProject({ cwd, configFile: 'app.config.js', skipCopy: true });

    expect(mockRunCommand).toHaveBeenCalledWith(
      process.execPath,
      [
        fakeRspeedyBin,
        'build',
        '--config',
        path.join(cwd, '.sparkling', 'lynx.build.config.ts'),
      ],
      { cwd },
    );
  });

  it('dev invokes the resolved JavaScript bin with Node and shell disabled', async () => {
    const { cwd } = createApp();

    await devProject({ cwd, configFile: 'app.config.js' });

    expect(mockSpawn).toHaveBeenCalledWith(
      process.execPath,
      [
        fakeRspeedyBin,
        'dev',
        '--config',
        path.join(cwd, '.sparkling', 'lynx.dev.config.ts'),
      ],
      {
        cwd,
        env: process.env,
        stdio: 'inherit',
        shell: false,
      },
    );
  });

  it('dev loads the app config before resolving Rspeedy', async () => {
    const cwd = fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling missing config '));
    mockResolveRspeedyCommand.mockImplementation(() => {
      throw new Error('Rspeedy resolution failed');
    });

    await expect(devProject({ cwd, configFile: 'missing.config.js' }))
      .rejects.toThrow(`App config not found at ${path.join(cwd, 'missing.config.js')}`);
    expect(mockResolveRspeedyCommand).not.toHaveBeenCalled();
  });
});
