// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { applyZephyrSupport } from '../create-app/zephyr';

describe('applyZephyrSupport', () => {
  it('patches package.json and app.config.ts for the default template shape', () => {
    const projectDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling-zephyr-'));

    fs.writeFileSync(
      path.join(projectDir, 'package.json'),
      JSON.stringify({
        scripts: {
          build: 'sparkling-app-cli build --copy',
        },
        devDependencies: {},
      }),
    );

    fs.writeFileSync(
      path.join(projectDir, 'app.config.ts'),
      `import { defineConfig } from '@lynx-js/rspeedy'\nimport { pluginReactLynx } from '@lynx-js/react-rsbuild-plugin'\n\nconst lynxConfig = defineConfig({\n  plugins: [pluginReactLynx()],\n})\n\nconst config = {\n  router: {\n    main: {\n      path: './lynxPages/main',\n    },\n  },\n}\n\nexport default config\n`,
    );

    applyZephyrSupport(projectDir, 'demo-app');

    const packageJson = JSON.parse(fs.readFileSync(path.join(projectDir, 'package.json'), 'utf8')) as {
      scripts: Record<string, string>;
      devDependencies: Record<string, string>;
    };
    const appConfig = fs.readFileSync(path.join(projectDir, 'app.config.ts'), 'utf8');

    expect(packageJson.scripts['deploy:zephyr:ios']).toBe('sparkling-app-cli deploy:zephyr --target ios');
    expect(packageJson.scripts['deploy:zephyr:android']).toBe('sparkling-app-cli deploy:zephyr --target android');
    expect(packageJson.devDependencies['zephyr-cli']).toBeUndefined();
    expect(packageJson.devDependencies['zephyr-rspack-plugin']).toBeUndefined();
    expect(appConfig).not.toContain("import { withZephyr } from 'zephyr-rspack-plugin'");
    expect(appConfig).not.toContain('config = await withZephyr()(config)');
    expect(appConfig).toContain("appId: 'demo-app'");
    expect(appConfig).toContain("versionUrl: process.env.ZEPHYR_VERSION_URL");
    expect(appConfig).toContain("strategy: 'next-launch'");
    expect(appConfig).toContain("fallback: 'bundled'");
  });
});
