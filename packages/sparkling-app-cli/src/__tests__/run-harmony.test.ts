// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

import { runHarmony } from '../commands/run-harmony';
import { buildProject } from '../commands/build';
import { autolink } from '../commands/autolink';
import { loadAppConfig } from '../config';
import { runCommand } from '../utils/exec';

jest.mock('../commands/build', () => ({
  buildProject: jest.fn().mockResolvedValue(undefined),
}));
jest.mock('../commands/autolink', () => ({
  autolink: jest.fn().mockResolvedValue([]),
}));
jest.mock('../config', () => ({
  loadAppConfig: jest.fn(),
}));
jest.mock('../utils/exec', () => ({
  runCommand: jest.fn().mockResolvedValue(undefined),
}));
jest.mock('node:child_process', () => ({
  execFileSync: jest.fn(),
}));

describe('runHarmony', () => {
  let work: string;

  beforeEach(() => {
    jest.clearAllMocks();
    work = fs.mkdtempSync(path.join(os.tmpdir(), 'spk-harmony-'));
    fs.mkdirSync(path.join(work, 'harmony'), { recursive: true });
    fs.writeFileSync(path.join(work, 'harmony', 'build-profile.json5'), '{}');
    (loadAppConfig as jest.Mock).mockResolvedValue({
      config: { platform: { harmony: { bundleName: 'com.demo.app' } } },
    });
  });

  afterEach(() => {
    fs.rmSync(work, { recursive: true, force: true });
  });

  it('builds bundles, installs OHPM dependencies, and assembles a HAP', async () => {
    await runHarmony({ cwd: work, skipCopy: false });

    expect(autolink).toHaveBeenCalledWith({ cwd: work, platform: 'harmony' });
    expect(buildProject).toHaveBeenCalledWith({ cwd: work, skipCopy: false });
    expect(runCommand).toHaveBeenNthCalledWith(1, expect.stringMatching(/ohpm$/), ['install'], {
      cwd: path.join(work, 'harmony'),
    });
    expect(runCommand).toHaveBeenNthCalledWith(
      2,
      expect.stringMatching(/hvigorw$/),
      expect.arrayContaining(['assembleHap', 'module=entry@default', 'buildMode=debug']),
      expect.objectContaining({ cwd: path.join(work, 'harmony') }),
    );
  });

  it('fails clearly when the generated HarmonyOS project is missing', async () => {
    fs.rmSync(path.join(work, 'harmony'), { recursive: true, force: true });

    await expect(runHarmony({ cwd: work })).rejects.toThrow('HarmonyOS project not found');
  });

  it('installs and launches an unsigned HAP when an emulator is connected', async () => {
    const outputDir = path.join(work, 'harmony/entry/build/default/outputs/default');
    const hapPath = path.join(outputDir, 'entry-default-unsigned.hap');
    fs.mkdirSync(outputDir, { recursive: true });
    fs.writeFileSync(hapPath, 'hap');
    (execFileSync as jest.Mock).mockReturnValue('emulator-id\n');

    await runHarmony({ cwd: work });

    expect(runCommand).toHaveBeenNthCalledWith(3, expect.stringMatching(/hdc$/), [
      'install',
      '-r',
      hapPath,
    ], { cwd: path.join(work, 'harmony') });
    expect(runCommand).toHaveBeenNthCalledWith(
      4,
      expect.stringMatching(/hdc$/),
      ['shell', 'aa', 'start', '-a', 'EntryAbility', '-b', 'com.demo.app'],
      { cwd: path.join(work, 'harmony') },
    );
  });

  it('does not try to install a signed HAP when no device is connected', async () => {
    const outputDir = path.join(work, 'harmony/entry/build/default/outputs/default');
    fs.mkdirSync(outputDir, { recursive: true });
    fs.writeFileSync(path.join(outputDir, 'entry-default-signed.hap'), 'hap');
    (execFileSync as jest.Mock).mockReturnValue('[Empty]\n');

    await runHarmony({ cwd: work });

    expect(runCommand).toHaveBeenCalledTimes(2);
  });

  it('installs and launches a signed HAP on a connected device', async () => {
    const outputDir = path.join(work, 'harmony/entry/build/default/outputs/default');
    const hapPath = path.join(outputDir, 'entry-default-signed.hap');
    fs.mkdirSync(outputDir, { recursive: true });
    fs.writeFileSync(hapPath, 'hap');
    (execFileSync as jest.Mock).mockReturnValue('device-id\n');

    await runHarmony({ cwd: work });

    expect(runCommand).toHaveBeenNthCalledWith(3, expect.stringMatching(/hdc$/), [
      'install',
      '-r',
      hapPath,
    ], { cwd: path.join(work, 'harmony') });
    expect(runCommand).toHaveBeenNthCalledWith(
      4,
      expect.stringMatching(/hdc$/),
      ['shell', 'aa', 'start', '-a', 'EntryAbility', '-b', 'com.demo.app'],
      { cwd: path.join(work, 'harmony') },
    );
  });

  it('selects the newest HAP instead of a stale signed artifact', async () => {
    const outputDir = path.join(work, 'harmony/entry/build/default/outputs/default');
    const staleSigned = path.join(outputDir, 'entry-default-signed.hap');
    const freshUnsigned = path.join(outputDir, 'entry-default-unsigned.hap');
    fs.mkdirSync(outputDir, { recursive: true });
    fs.writeFileSync(staleSigned, 'old');
    fs.writeFileSync(freshUnsigned, 'new');
    fs.utimesSync(staleSigned, new Date(1_000), new Date(1_000));
    fs.utimesSync(freshUnsigned, new Date(2_000), new Date(2_000));
    (execFileSync as jest.Mock).mockReturnValue('device-id\n');

    await runHarmony({ cwd: work });

    expect(runCommand).toHaveBeenNthCalledWith(3, expect.stringMatching(/hdc$/), [
      'install',
      '-r',
      freshUnsigned,
    ], { cwd: path.join(work, 'harmony') });
  });

  it('does not claim to launch when bundleName is missing', async () => {
    const outputDir = path.join(work, 'harmony/entry/build/default/outputs/default');
    const hapPath = path.join(outputDir, 'entry-default-signed.hap');
    fs.mkdirSync(outputDir, { recursive: true });
    fs.writeFileSync(hapPath, 'hap');
    (loadAppConfig as jest.Mock).mockResolvedValue({ config: {} });
    (execFileSync as jest.Mock).mockReturnValue('device-id\n');
    const log = jest.spyOn(console, 'log').mockImplementation();

    await runHarmony({ cwd: work });

    expect(runCommand).toHaveBeenCalledTimes(3);
    expect(log.mock.calls.flat().join('\n')).toContain('installed successfully');
    expect(log.mock.calls.flat().join('\n')).not.toContain('installed and launched successfully');
    log.mockRestore();
  });
});
