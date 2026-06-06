// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { isVerboseEnabled, enableVerboseLogging, verboseLog } from '../utils/verbose';

describe('utils/verbose', () => {
  const env = process.env;
  let logSpy: jest.SpyInstance;

  beforeEach(() => {
    process.env = { ...env };
    delete process.env.SPARKLING_VERBOSE;
    logSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    process.env = env;
    logSpy.mockRestore();
  });

  test('isVerboseEnabled is false when env var unset', () => {
    expect(isVerboseEnabled()).toBe(false);
  });

  test.each(['1', 'true', 'TRUE'])('isVerboseEnabled returns true for %s', value => {
    process.env.SPARKLING_VERBOSE = value;
    expect(isVerboseEnabled()).toBe(true);
  });

  test.each(['0', 'false', 'no', '', 'random'])(
    'isVerboseEnabled returns false for %s',
    value => {
      process.env.SPARKLING_VERBOSE = value;
      expect(isVerboseEnabled()).toBe(false);
    },
  );

  test('enableVerboseLogging is a no-op when not enabled', () => {
    enableVerboseLogging(false);
    expect(process.env.SPARKLING_VERBOSE).toBeUndefined();
    enableVerboseLogging();
    expect(process.env.SPARKLING_VERBOSE).toBeUndefined();
  });

  test('enableVerboseLogging sets env var when enabled', () => {
    enableVerboseLogging(true);
    expect(process.env.SPARKLING_VERBOSE).toBe('true');
  });

  test('verboseLog logs when verbose enabled, otherwise silent', () => {
    verboseLog('quiet');
    expect(logSpy).not.toHaveBeenCalled();

    process.env.SPARKLING_VERBOSE = '1';
    verboseLog('loud');
    expect(logSpy).toHaveBeenCalledWith('[verbose] loud');
  });
});
