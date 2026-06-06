// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import { createSparklingApp } from '../create-app';

jest.mock('@clack/prompts', () => ({
  confirm: jest.fn(),
  intro: jest.fn(),
  isCancel: jest.fn(() => false),
  multiselect: jest.fn(),
  note: jest.fn(),
  outro: jest.fn(),
  select: jest.fn(),
  text: jest.fn(),
}));

describe('createSparklingApp', () => {
  let work: string;

  beforeEach(() => {
    work = fs.mkdtempSync(path.join(os.tmpdir(), 'csa-create-'));
    jest.clearAllMocks();
  });

  afterEach(() => {
    fs.rmSync(work, { recursive: true, force: true });
  });

  function createMinimalTemplate(): string {
    const template = path.join(work, 'template');
    fs.mkdirSync(template, { recursive: true });
    fs.writeFileSync(
      path.join(template, 'package.json'),
      `${JSON.stringify({ name: 'template-app', version: '0.0.0' }, null, 2)}\n`,
    );
    fs.writeFileSync(
      path.join(template, 'app.config.ts'),
      [
        "export default {",
        "  platform: {",
        "    android: { packageName: '{{packageNamespace}}' },",
        "    ios: { bundleIdentifier: '{{packageNamespace}}' },",
        "  },",
        "};",
        "",
      ].join('\n'),
    );
    return template;
  }

  it('replaces packageNamespace placeholders in template files', async () => {
    const template = createMinimalTemplate();

    await createSparklingApp({
      args: { name: 'demo-app' },
      cwd: work,
      flags: {
        git: false,
        install: false,
        namespace: 'com.demo.app',
        pm: 'npm',
        template,
        yes: true,
      },
    });

    const appConfig = fs.readFileSync(path.join(work, 'demo-app', 'app.config.ts'), 'utf8');
    expect(appConfig).toContain("android: { packageName: 'com.demo.app' }");
    expect(appConfig).toContain("ios: { bundleIdentifier: 'com.demo.app' }");
    expect(appConfig).not.toContain('{{packageNamespace}}');
  });
});
