// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
/// <reference types="jest" />
import { EventEmitter } from 'node:events';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { devProject } from '../commands/dev';

const mockSpawn = jest.fn();

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

describe('devProject host config', () => {
  beforeEach(() => {
    mockSpawn.mockReset();
    mockSpawn.mockReturnValue(createClosingChild());
  });

  it('uses app config dev.server.host when --host is not provided', async () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'spk-dev-host-'));
    const configPath = path.join(dir, 'app.config.js');
    fs.writeFileSync(
      configPath,
      'module.exports = { lynxConfig: {}, dev: { server: { host: "192.168.9.9", port: 6060 } } };\n',
    );

    await devProject({ cwd: dir, configFile: 'app.config.js' });

    const generatedConfig = fs.readFileSync(path.join(dir, '.sparkling', 'lynx.dev.config.ts'), 'utf8');
    expect(generatedConfig).toContain('port: 6060');
    expect(generatedConfig).toContain('host: "192.168.9.9"');
  });

  it('warns when app config binds the dev server to every interface', async () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'spk-dev-host-'));
    const configPath = path.join(dir, 'app.config.js');
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    fs.writeFileSync(
      configPath,
      'module.exports = { lynxConfig: {}, dev: { server: { host: "0.0.0.0", port: 6060 } } };\n',
    );

    await devProject({ cwd: dir, configFile: 'app.config.js' });

    expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('0.0.0.0'));

    warnSpy.mockRestore();
  });
});
