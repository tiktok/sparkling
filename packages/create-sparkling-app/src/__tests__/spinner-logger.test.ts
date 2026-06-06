// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import * as p from '@clack/prompts';

import { defaultLogger } from '../logger';
import { createSpinner } from '../utils/spinner';

jest.mock('@clack/prompts', () => ({
  spinner: jest.fn(),
}));

describe('spinner and logger utilities', () => {
  const originalIsTTY = process.stdout.isTTY;
  let warnSpy: jest.SpyInstance;

  beforeEach(() => {
    warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    Object.defineProperty(process.stdout, 'isTTY', {
      configurable: true,
      value: originalIsTTY,
    });
    warnSpy.mockRestore();
    jest.clearAllMocks();
  });

  it('uses console output when stdout is not a TTY', () => {
    Object.defineProperty(process.stdout, 'isTTY', {
      configurable: true,
      value: false,
    });

    const spinner = createSpinner();
    spinner.start('start');
    spinner.message('progress');
    spinner.stop();

    expect(console.log).toHaveBeenCalledWith('start');
    expect(console.log).toHaveBeenCalledWith('progress');
  });

  it('delegates to clack spinner when stdout is a TTY', () => {
    Object.defineProperty(process.stdout, 'isTTY', {
      configurable: true,
      value: true,
    });
    const clackSpinner = {
      message: jest.fn(),
      start: jest.fn(),
      stop: jest.fn(),
    };
    (p.spinner as jest.Mock).mockReturnValueOnce(clackSpinner);

    const spinner = createSpinner();
    spinner.start('start');
    spinner.message('progress');
    spinner.stop('done');

    expect(clackSpinner.start).toHaveBeenCalledWith('start');
    expect(clackSpinner.message).toHaveBeenCalledWith('progress');
    expect(clackSpinner.stop).toHaveBeenCalledWith('done');
  });

  it('routes logger levels to console methods', () => {
    defaultLogger.info('hello', 'world');
    defaultLogger.message('message');
    defaultLogger.warn('warning');
    defaultLogger.error('problem');
    defaultLogger.log?.('warn', 'from-log');
    defaultLogger.log?.('error', 'from-log-error');
    defaultLogger.log?.('info', 'from-log-info');
    defaultLogger.clear();

    expect(console.log).toHaveBeenCalledWith('hello world');
    expect(console.log).toHaveBeenCalledWith('message');
    expect(console.warn).toHaveBeenCalledWith('warning');
    expect(console.error).toHaveBeenCalledWith('problem');
  });
});
