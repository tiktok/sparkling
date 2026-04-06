// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import init from '../init';
import { createSparklingApp } from '../create-app';

jest.mock('../create-app');

const mockedCreateSparklingApp = createSparklingApp as jest.MockedFunction<typeof createSparklingApp>;

describe('init command', () => {
  beforeEach(() => {
    mockedCreateSparklingApp.mockResolvedValue();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('forwards parsed name and flags to createSparklingApp', async () => {
    await init(['my-app', '--template', 'sparkling-bare', '--pm', 'pnpm', '--no-install', '--with-zephyr']);

    expect(mockedCreateSparklingApp).toHaveBeenCalledWith(expect.objectContaining({
      args: { name: 'my-app' },
      flags: expect.objectContaining({
        install: false,
        pm: 'pnpm',
        template: 'sparkling-bare',
        withZephyr: true,
      }),
      cwd: process.cwd(),
    }));
  });

  it('passes undefined when no positional name provided', async () => {
    await init([]);

    expect(mockedCreateSparklingApp).toHaveBeenCalledWith(expect.objectContaining({
      args: { name: undefined },
      flags: expect.any(Object),
      cwd: process.cwd(),
    }));
  });

  it('rejects deprecated language flags', async () => {
    await expect(init(['my-app', '--js'])).rejects.toMatchObject({
      message: expect.stringContaining('unknown option'),
    });
  });
});
