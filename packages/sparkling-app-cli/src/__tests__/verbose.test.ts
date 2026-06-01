// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { enableVerboseLogging, isVerboseEnabled, verboseLog } from '../utils/verbose';

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

  test('isVerboseEnabled false by default, true on truthy values', () => {
    expect(isVerboseEnabled()).toBe(false);
    process.env.SPARKLING_VERBOSE = '1';
    expect(isVerboseEnabled()).toBe(true);
    process.env.SPARKLING_VERBOSE = 'true';
    expect(isVerboseEnabled()).toBe(true);
    process.env.SPARKLING_VERBOSE = 'TRUE';
    expect(isVerboseEnabled()).toBe(true);
    process.env.SPARKLING_VERBOSE = 'no';
    expect(isVerboseEnabled()).toBe(false);
  });

  test('enableVerboseLogging respects nullish input', () => {
    enableVerboseLogging();
    expect(process.env.SPARKLING_VERBOSE).toBeUndefined();
    enableVerboseLogging(false);
    expect(process.env.SPARKLING_VERBOSE).toBeUndefined();
    enableVerboseLogging(true);
    expect(process.env.SPARKLING_VERBOSE).toBe('true');
  });

  test('verboseLog only emits when verbose is enabled', () => {
    verboseLog('off');
    expect(logSpy).not.toHaveBeenCalled();
    process.env.SPARKLING_VERBOSE = 'true';
    verboseLog('on');
    expect(logSpy).toHaveBeenCalledWith('[verbose] on');
  });
});
