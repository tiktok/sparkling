// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

jest.mock('node:child_process', () => ({
  execSync: jest.fn(),
}));

import { execSync } from 'node:child_process';
import { detectPackageManager } from '../create-app/post-create';

const mockedExecSync = execSync as jest.MockedFunction<typeof execSync>;

describe('post-create detectPackageManager', () => {
  const originalUserAgent = process.env.npm_config_user_agent;

  afterEach(() => {
    process.env.npm_config_user_agent = originalUserAgent;
    mockedExecSync.mockReset();
  });

  it('uses npm_config_user_agent when available', () => {
    process.env.npm_config_user_agent = 'pnpm/9.0.0 node/v20';

    const result = detectPackageManager();

    expect(result).toBe('pnpm');
    expect(mockedExecSync).not.toHaveBeenCalled();
  });

  it('falls back to probing binaries when user agent is not set', () => {
    process.env.npm_config_user_agent = '';
    mockedExecSync.mockImplementation(((command: string) => {
      if (command.startsWith('pnpm')) {
        return Buffer.from('');
      }
      throw new Error('not installed');
    }) as typeof execSync);

    const result = detectPackageManager();

    expect(result).toBe('pnpm');
    expect(mockedExecSync).toHaveBeenCalled();
  });

  it('returns npm when no managers are detected', () => {
    process.env.npm_config_user_agent = '';
    mockedExecSync.mockImplementation(() => {
      throw new Error('not installed');
    });

    const result = detectPackageManager();

    expect(result).toBe('npm');
  });
});
