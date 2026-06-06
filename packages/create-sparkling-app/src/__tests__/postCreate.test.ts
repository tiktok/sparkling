// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

jest.mock('node:child_process', () => ({
  execSync: jest.fn(),
}));

jest.mock('@clack/prompts', () => ({
  log: {
    warn: jest.fn(),
  },
  note: jest.fn(),
}));

jest.mock('../utils/spinner', () => ({
  createSpinner: jest.fn(() => ({
    start: jest.fn(),
    stop: jest.fn(),
  })),
}));

jest.mock('../utils/verbose', () => ({
  isVerboseEnabled: jest.fn(() => false),
  verboseLog: jest.fn(),
}));

jest.mock('../ui', () => ({
  ui: {
    tip: (t: string) => `tip: ${t}`,
  },
}));

import { execSync } from 'node:child_process';
import * as p from '@clack/prompts';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';
import {
  detectPackageManager,
  installDependencies,
  initializeGitRepo,
  showCompletionNotes,
} from '../create-app/post-create';

const mockedExecSync = execSync as jest.MockedFunction<typeof execSync>;
const mockedIsVerboseEnabled = isVerboseEnabled as jest.MockedFunction<typeof isVerboseEnabled>;
const mockedVerboseLog = verboseLog as jest.MockedFunction<typeof verboseLog>;
const mockedLogWarn = p.log.warn as jest.MockedFunction<typeof p.log.warn>;
const mockedNote = p.note as jest.MockedFunction<typeof p.note>;

describe('detectPackageManager', () => {
  const originalUserAgent = process.env.npm_config_user_agent;

  afterEach(() => {
    process.env.npm_config_user_agent = originalUserAgent;
    mockedExecSync.mockReset();
    mockedIsVerboseEnabled.mockReturnValue(false);
  });

  it('uses npm_config_user_agent when available', () => {
    process.env.npm_config_user_agent = 'pnpm/9.0.0 node/v20';

    const result = detectPackageManager();

    expect(result).toBe('pnpm');
    expect(mockedExecSync).not.toHaveBeenCalled();
  });

  it('detects yarn from user agent', () => {
    process.env.npm_config_user_agent = 'yarn/3.2.0 node/v18';

    expect(detectPackageManager()).toBe('yarn');
  });

  it('detects bun from user agent', () => {
    process.env.npm_config_user_agent = 'bun/1.0.0 node/v20';

    expect(detectPackageManager()).toBe('bun');
  });

  it('detects npm from user agent', () => {
    process.env.npm_config_user_agent = 'npm/10.0.0 node/v20';

    expect(detectPackageManager()).toBe('npm');
  });

  it('falls back to probing pnpm binary when user agent is not set', () => {
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

  it('detects yarn when pnpm is not available', () => {
    process.env.npm_config_user_agent = '';
    mockedExecSync.mockImplementation(((command: string) => {
      if (command.startsWith('pnpm')) throw new Error('not found');
      if (command.startsWith('yarn')) return Buffer.from('');
      throw new Error('not installed');
    }) as typeof execSync);

    expect(detectPackageManager()).toBe('yarn');
  });

  it('detects bun when pnpm and yarn are not available', () => {
    process.env.npm_config_user_agent = '';
    mockedExecSync.mockImplementation(((command: string) => {
      if (command.startsWith('pnpm')) throw new Error('not found');
      if (command.startsWith('yarn')) throw new Error('not found');
      if (command.startsWith('bun')) return Buffer.from('');
      throw new Error('not installed');
    }) as typeof execSync);

    expect(detectPackageManager()).toBe('bun');
  });

  it('returns npm when no managers are detected', () => {
    process.env.npm_config_user_agent = '';
    mockedExecSync.mockImplementation(() => {
      throw new Error('not installed');
    });

    const result = detectPackageManager();

    expect(result).toBe('npm');
  });

  it('logs detected package manager when verbose is enabled', () => {
    process.env.npm_config_user_agent = 'pnpm/9.0.0 node/v20';
    mockedIsVerboseEnabled.mockReturnValue(true);

    detectPackageManager();

    expect(mockedVerboseLog).toHaveBeenCalledWith(
      expect.stringContaining('pnpm')
    );
  });

  it('logs binary probe result when verbose is enabled', () => {
    process.env.npm_config_user_agent = '';
    mockedIsVerboseEnabled.mockReturnValue(true);
    mockedExecSync.mockImplementation(((command: string) => {
      if (command.startsWith('pnpm')) return Buffer.from('');
      throw new Error('not installed');
    }) as typeof execSync);

    detectPackageManager();

    expect(mockedVerboseLog).toHaveBeenCalledWith(
      expect.stringContaining('pnpm')
    );
  });

  it('logs fallback to npm when verbose is enabled', () => {
    process.env.npm_config_user_agent = '';
    mockedIsVerboseEnabled.mockReturnValue(true);
    mockedExecSync.mockImplementation(() => { throw new Error('not found'); });

    detectPackageManager();

    expect(mockedVerboseLog).toHaveBeenCalledWith(
      expect.stringContaining('npm')
    );
  });
});

describe('installDependencies', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedIsVerboseEnabled.mockReturnValue(false);
  });

  it('returns false when packageManager is undefined', async () => {
    const result = await installDependencies('/some/dir', undefined);

    expect(result).toBe(false);
    expect(mockedExecSync).not.toHaveBeenCalled();
  });

  it('runs package manager install and returns true on success', async () => {
    mockedExecSync.mockReturnValue(Buffer.from(''));

    const result = await installDependencies('/project', 'pnpm');

    expect(result).toBe(true);
    expect(mockedExecSync).toHaveBeenCalledWith(
      'pnpm install',
      expect.objectContaining({ cwd: '/project', stdio: 'pipe' })
    );
  });

  it('returns false and logs warning when install fails', async () => {
    mockedExecSync.mockImplementation(() => { throw new Error('install error'); });

    const result = await installDependencies('/project', 'npm');

    expect(result).toBe(false);
    expect(mockedLogWarn).toHaveBeenCalledWith(
      expect.stringContaining("npm install")
    );
  });

  it('uses inherit stdio in verbose mode', async () => {
    mockedIsVerboseEnabled.mockReturnValue(true);
    mockedExecSync.mockReturnValue(Buffer.from(''));

    await installDependencies('/project', 'yarn');

    expect(mockedExecSync).toHaveBeenCalledWith(
      'yarn install',
      expect.objectContaining({ stdio: 'inherit' })
    );
  });

  it('logs verbose message before executing install', async () => {
    mockedIsVerboseEnabled.mockReturnValue(true);
    mockedExecSync.mockReturnValue(Buffer.from(''));

    await installDependencies('/my/dir', 'npm');

    expect(mockedVerboseLog).toHaveBeenCalledWith(
      expect.stringContaining('/my/dir')
    );
  });

  it('logs verbose message on failure', async () => {
    mockedIsVerboseEnabled.mockReturnValue(true);
    mockedExecSync.mockImplementation(() => { throw new Error('ENOENT'); });

    await installDependencies('/project', 'bun');

    expect(mockedVerboseLog).toHaveBeenCalledWith(
      expect.stringContaining('ENOENT')
    );
  });
});

describe('initializeGitRepo', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedIsVerboseEnabled.mockReturnValue(false);
  });

  it('runs git init when git is available', async () => {
    mockedExecSync.mockReturnValue(Buffer.from('git version 2.40.0'));

    await initializeGitRepo('/project');

    expect(mockedExecSync).toHaveBeenCalledWith(
      'git init',
      expect.objectContaining({ cwd: '/project' })
    );
  });

  it('warns and skips when git is not available', async () => {
    mockedExecSync.mockImplementation(((cmd: string) => {
      if (cmd === 'git --version') throw new Error('not found');
      return Buffer.from('');
    }) as typeof execSync);

    await initializeGitRepo('/project');

    expect(mockedLogWarn).toHaveBeenCalledWith(
      expect.stringContaining('git')
    );
    expect(mockedExecSync).not.toHaveBeenCalledWith(
      'git init',
      expect.anything()
    );
  });

  it('warns on git init failure', async () => {
    mockedExecSync.mockImplementation(((cmd: string) => {
      if (cmd === 'git --version') return Buffer.from('git version 2.40.0');
      throw new Error('git init failed');
    }) as typeof execSync);

    await initializeGitRepo('/project');

    expect(mockedLogWarn).toHaveBeenCalledWith(
      expect.stringContaining('git init')
    );
  });

  it('uses inherit stdio in verbose mode', async () => {
    mockedIsVerboseEnabled.mockReturnValue(true);
    mockedExecSync.mockReturnValue(Buffer.from(''));

    await initializeGitRepo('/project');

    expect(mockedExecSync).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ stdio: 'inherit' })
    );
  });
});

describe('showCompletionNotes', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows note with cd command', () => {
    showCompletionNotes('my-app');

    expect(mockedNote).toHaveBeenCalledWith(
      expect.stringContaining('cd my-app'),
      expect.any(String)
    );
  });

  it('uses npm run for npm package manager', () => {
    showCompletionNotes('my-app', 'npm', false);

    const noteContent = (mockedNote.mock.calls[0][0] as string);
    expect(noteContent).toContain('npm run run:ios');
    expect(noteContent).toContain('npm run run:android');
  });

  it('uses short format for pnpm package manager', () => {
    showCompletionNotes('my-app', 'pnpm', false);

    const noteContent = (mockedNote.mock.calls[0][0] as string);
    expect(noteContent).toContain('pnpm run:ios');
    expect(noteContent).toContain('pnpm run:android');
  });

  it('uses short format for yarn package manager', () => {
    showCompletionNotes('my-app', 'yarn', false);

    const noteContent = (mockedNote.mock.calls[0][0] as string);
    expect(noteContent).toContain('yarn run:ios');
  });

  it('shows install instruction when dependencies not installed', () => {
    showCompletionNotes('my-app', 'npm', false);

    const noteContent = (mockedNote.mock.calls[0][0] as string);
    expect(noteContent).toContain('Install dependencies');
  });

  it('shows installed notice when dependencies were installed', () => {
    showCompletionNotes('my-app', 'npm', true);

    const noteContent = (mockedNote.mock.calls[0][0] as string);
    expect(noteContent).toContain('Dependencies installed');
  });

  it('defaults to npm when no package manager given', () => {
    showCompletionNotes('my-app', undefined, false);

    const noteContent = (mockedNote.mock.calls[0][0] as string);
    expect(noteContent).toContain('npm run run:ios');
  });

  it('includes tips about Xcode and Android SDK', () => {
    showCompletionNotes('my-app');

    const noteContent = (mockedNote.mock.calls[0][0] as string);
    expect(noteContent).toContain('iOS');
    expect(noteContent).toContain('Android');
  });

  it('includes target directory in note title', () => {
    showCompletionNotes('awesome-project');

    const noteTitle = mockedNote.mock.calls[0][1];
    expect(noteTitle).toContain('awesome-project');
  });
});
