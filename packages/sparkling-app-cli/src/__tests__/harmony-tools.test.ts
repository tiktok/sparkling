// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import {
  resolveHarmonyJavaHome,
  resolveHarmonySdkHome,
  resolveHarmonyTool,
} from '../utils/harmony-tools';

describe('HarmonyOS tool resolution', () => {
  let work: string;
  let originalEnvironment: NodeJS.ProcessEnv;

  beforeEach(() => {
    work = fs.mkdtempSync(path.join(os.tmpdir(), 'spk-harmony-tools-'));
    originalEnvironment = { ...process.env };
    for (const key of [
      'DEVECO_HOME',
      'DEVECO_STUDIO_HOME',
      'DEVECO_SDK_HOME',
      'HOS_SDK_HOME',
      'OHOS_HOME',
      'OHOS_SDK_HOME',
      'JAVA_HOME',
    ]) {
      delete process.env[key];
    }
    process.env.PATH = '';
  });

  afterEach(() => {
    process.env = originalEnvironment;
    fs.rmSync(work, { recursive: true, force: true });
  });

  function executable(tool: 'ohpm' | 'hvigorw' | 'hdc' | 'java'): string {
    if (process.platform !== 'win32') return tool;
    if (tool === 'hdc' || tool === 'java') return `${tool}.exe`;
    return `${tool}.bat`;
  }

  function touch(relativePath: string): string {
    const filePath = path.join(work, relativePath);
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, '');
    return filePath;
  }

  it('resolves tools, SDK, and JBR from a configured DevEco root', () => {
    const ohpm = touch(path.join('tools/ohpm/bin', executable('ohpm')));
    const hvigor = touch(path.join('tools/hvigor/bin', executable('hvigorw')));
    const hdc = touch(path.join('sdk/default/openharmony/toolchains', executable('hdc')));
    const javaHome = path.join(work, process.platform === 'darwin' ? 'jbr/Contents/Home' : 'jbr');
    touch(path.relative(work, path.join(javaHome, 'bin', executable('java'))));
    process.env.DEVECO_HOME = work;

    expect(resolveHarmonyTool('ohpm')).toBe(ohpm);
    expect(resolveHarmonyTool('hvigorw')).toBe(hvigor);
    expect(resolveHarmonyTool('hdc')).toBe(hdc);
    expect(resolveHarmonySdkHome()).toBe(path.join(work, 'sdk'));
    expect(resolveHarmonyJavaHome()).toBe(javaHome);
  });

  it('supports standalone Command Line Tools layouts', () => {
    const ohpm = touch(path.join('ohpm/bin', executable('ohpm')));
    const hvigor = touch(path.join('hvigor/bin', executable('hvigorw')));
    process.env.DEVECO_HOME = work;

    expect(resolveHarmonyTool('ohpm')).toBe(ohpm);
    expect(resolveHarmonyTool('hvigorw')).toBe(hvigor);
  });
});
