// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import { saveConfig, type Config } from '../core/config';

describe('core/config', () => {
  let work: string;

  beforeEach(() => {
    work = fs.mkdtempSync(path.join(os.tmpdir(), 'csa-cfg-'));
  });

  afterEach(() => {
    fs.rmSync(work, { recursive: true, force: true });
  });

  test('saveConfig writes pretty-printed JSON with trailing newline', () => {
    const cfg: Config = {
      description: 'A demo app',
      name: 'demo-app',
      type: 'app',
      template: 'sparkling-app-template',
      packageManager: 'pnpm',
      tools: { biome: true, eslint: false, prettier: false, testing: true },
      androidGradleDsl: 'kts',
    };

    const file = path.join(work, '.sparkling.json');
    saveConfig(file, cfg);

    const written = fs.readFileSync(file, 'utf8');
    expect(written.endsWith('\n')).toBe(true);
    expect(JSON.parse(written)).toEqual(cfg);
    expect(written).toContain('  '); // pretty-printed
  });

  test('saveConfig handles optional customTemplatePath round-trip', () => {
    const cfg: Config = {
      description: 'desc',
      name: 'x',
      type: 'app',
      template: 'custom',
      customTemplatePath: '/abs/path/to/template',
      packageManager: 'npm',
      tools: {},
      androidGradleDsl: 'groovy',
    };
    const file = path.join(work, 'cfg.json');
    saveConfig(file, cfg);
    expect(JSON.parse(fs.readFileSync(file, 'utf8'))).toEqual(cfg);
  });
});
