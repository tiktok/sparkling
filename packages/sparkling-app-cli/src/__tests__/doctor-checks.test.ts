// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { execFileSync, spawnSync } from 'node:child_process';

import { checkJdk } from '../commands/doctor/checks';

jest.mock('node:child_process', () => ({
  execFileSync: jest.fn(),
  spawnSync: jest.fn(),
}));

const execFileSyncMock = jest.mocked(execFileSync);
const spawnSyncMock = jest.mocked(spawnSync);

const JAVA_NOT_INSTALLED_RESULT = {
  name: 'JDK',
  category: 'android',
  expected: '>= 11',
  status: 'fail',
  message: 'Java is not installed or not found in PATH',
  fixHint: 'Java (JDK) is not installed. I need JDK 11 or higher.',
} as const;

function createWindowsJavaNotFoundResult(streams: Record<string, unknown>): ReturnType<typeof spawnSync> {
  const error = Object.assign(new Error('spawnSync java ENOENT'), {
    errno: -4058,
    code: 'ENOENT',
    syscall: 'spawnSync java',
    path: 'java',
    spawnargs: ['-version'],
  });

  return {
    pid: 0,
    output: null,
    status: null,
    signal: null,
    error,
    ...streams,
  } as unknown as ReturnType<typeof spawnSync>;
}

function createJavaVersionResult(
  stdout: string | Buffer | null | undefined,
  stderr: string | Buffer | null | undefined,
): ReturnType<typeof spawnSync> {
  return {
    pid: 123,
    output: [null, stdout, stderr],
    stdout,
    stderr,
    status: 0,
    signal: null,
  } as unknown as ReturnType<typeof spawnSync>;
}

describe('checkJdk', () => {
  beforeEach(() => {
    execFileSyncMock.mockReset();
    execFileSyncMock.mockReturnValue('');
    spawnSyncMock.mockReset();
  });

  it.each([
    ['missing streams', {}],
    ['undefined streams', { stdout: undefined, stderr: undefined }],
    ['null streams', { stdout: null, stderr: null }],
  ])('reports Java as not installed for a Windows ENOENT error with %s', (_description, streams) => {
    spawnSyncMock.mockReturnValue(createWindowsJavaNotFoundResult(streams));

    expect(checkJdk()).toEqual(JAVA_NOT_INSTALLED_RESULT);
  });

  it('reads an installed Java version from stderr when the process exits successfully', () => {
    spawnSyncMock.mockReturnValue(createJavaVersionResult('', 'openjdk version "11.0.18" 2023-01-17'));

    expect(checkJdk()).toEqual({
      name: 'JDK',
      category: 'android',
      expected: '>= 11',
      status: 'pass',
      version: '11.0.18',
    });
    expect(spawnSyncMock).toHaveBeenCalledWith('java', ['-version'], {
      encoding: 'utf8',
      shell: false,
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: 15_000,
    });
  });

  it.each([
    ['string', 'openjdk version "17.0.9" 2023-10-17'],
    ['Buffer', Buffer.from('openjdk version "21.0.2" 2024-01-16')],
  ])('falls back to a %s Java version on stdout', (_description, stdout) => {
    spawnSyncMock.mockReturnValue(createJavaVersionResult(stdout, undefined));

    expect(checkJdk()).toEqual(expect.objectContaining({
      status: 'pass',
      version: stdout instanceof Buffer ? '21.0.2' : '17.0.9',
    }));
  });

  it('falls back to stdout when stderr is empty', () => {
    spawnSyncMock.mockReturnValue(
      createJavaVersionResult('openjdk version "11.0.18" 2023-01-17', Buffer.from('  \n')),
    );

    expect(checkJdk()).toEqual(expect.objectContaining({
      status: 'pass',
      version: '11.0.18',
    }));
  });

  it('prefers a non-empty stderr version over stdout', () => {
    spawnSyncMock.mockReturnValue(
      createJavaVersionResult('java version "1.8.0_392"', 'openjdk version "17.0.9" 2023-10-17'),
    );

    expect(checkJdk()).toEqual(expect.objectContaining({
      status: 'pass',
      version: '17.0.9',
    }));
  });
});
