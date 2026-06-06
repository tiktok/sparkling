// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import { FileTemplater } from '../utils/file-templater';

describe('FileTemplater', () => {
  let work: string;

  beforeEach(() => {
    work = fs.mkdtempSync(path.join(os.tmpdir(), 'csa-templater-'));
  });

  afterEach(() => {
    fs.rmSync(work, { recursive: true, force: true });
  });

  it('replaces placeholders in file contents', async () => {
    const file = path.join(work, 'README.md');
    fs.writeFileSync(file, 'Hello {{appName}} from {{namespace}}');

    const rendered = await FileTemplater.replaceInFile(file, {
      appName: 'Sparkling',
      namespace: 'com.example.app',
    });

    expect(rendered).toBe('Hello Sparkling from com.example.app');
  });

  it('updates files after replacement', async () => {
    const file = path.join(work, 'config.txt');
    fs.writeFileSync(file, 'name={{name}}');

    await FileTemplater.replaceInFileAndUpdate(file, { name: 'demo' });

    expect(fs.readFileSync(file, 'utf8')).toBe('name=demo');
  });

  it('renames paths and creates parent directories when placeholders expand', async () => {
    const original = path.join(work, 'src', '{{module}}', 'index.ts');
    fs.mkdirSync(path.dirname(original), { recursive: true });
    fs.writeFileSync(original, 'export {};');

    const renamed = await FileTemplater.renamePathWithVariables(original, {
      module: 'media',
    });

    expect(renamed).toBe(path.join(work, 'src', 'media', 'index.ts'));
    expect(fs.existsSync(renamed)).toBe(true);
    expect(fs.existsSync(original)).toBe(false);
  });

  it('recursively renames directories containing placeholders', async () => {
    const nested = path.join(work, 'android', '{{packagePath}}', 'Feature');
    fs.mkdirSync(nested, { recursive: true });
    fs.writeFileSync(path.join(nested, 'Marker.kt'), 'class Marker');

    await FileTemplater.renameDirectoryContentsWithVariables(work, {
      packagePath: 'com/example/app',
    });

    expect(fs.existsSync(path.join(work, 'android', 'com/example/app', 'Feature', 'Marker.kt'))).toBe(true);
  });

  it('returns resolved path without renaming when no placeholder changes the path', async () => {
    const file = path.join(work, 'plain.txt');
    fs.writeFileSync(file, 'plain');

    await expect(FileTemplater.renamePathWithVariables(file, { name: 'demo' })).resolves.toBe(file);
  });

  it('throws clear errors for invalid paths', async () => {
    await expect(FileTemplater.replaceInFile('relative.txt', {})).rejects.toThrow('absolute');
    await expect(FileTemplater.renameDirectoryContentsWithVariables('relative', {})).rejects.toThrow('absolute');
    await expect(FileTemplater.renamePathWithVariables('', {})).rejects.toThrow('cannot be empty');
    await expect(FileTemplater.renamePathWithVariables(path.join(work, 'missing-{{name}}'), { name: 'x' }))
      .rejects.toThrow('Error accessing original path');
  });
});
