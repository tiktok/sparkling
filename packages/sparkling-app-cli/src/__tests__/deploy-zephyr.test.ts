jest.mock('../config', () => ({
  loadAppConfig: jest.fn(),
}));

jest.mock('../commands/build', () => ({
  buildProject: jest.fn(),
}));

jest.mock('../commands/copy-assets', () => ({
  copyAssets: jest.fn(),
}));

jest.mock('../commands/zephyr-runtime-config', () => ({
  writeZephyrRuntimeConfig: jest.fn(),
}));

jest.mock('zephyr-agent', () => ({
  uploadOutputToZephyr: jest.fn(),
}));

import { loadAppConfig } from '../config';
import { buildProject } from '../commands/build';
import { copyAssets } from '../commands/copy-assets';
import { deployToZephyr, resolveZephyrDeployTarget } from '../commands/deploy-zephyr';
import { writeZephyrRuntimeConfig } from '../commands/zephyr-runtime-config';
import { uploadOutputToZephyr } from 'zephyr-agent';

const mockedLoadAppConfig = loadAppConfig as jest.MockedFunction<typeof loadAppConfig>;
const mockedBuildProject = buildProject as jest.MockedFunction<typeof buildProject>;
const mockedCopyAssets = copyAssets as jest.MockedFunction<typeof copyAssets>;
const mockedWriteZephyrRuntimeConfig =
  writeZephyrRuntimeConfig as jest.MockedFunction<typeof writeZephyrRuntimeConfig>;
const mockedUploadOutputToZephyr = uploadOutputToZephyr as jest.MockedFunction<typeof uploadOutputToZephyr>;

describe('resolveZephyrDeployTarget', () => {
  it('uses the explicit target when provided', () => {
    expect(resolveZephyrDeployTarget({ lynxConfig: {} } as any, 'ios')).toBe('ios');
  });

  it('uses the configured target when exactly one exists', () => {
    expect(resolveZephyrDeployTarget({
      lynxConfig: {},
      zephyr: {
        ios: { target: 'ios' },
      },
    } as any)).toBe('ios');
  });
});

describe('deployToZephyr', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedLoadAppConfig.mockResolvedValue({
      config: {
        lynxConfig: {},
        zephyr: {
          ios: { target: 'ios' },
        },
      } as any,
      configPath: '/tmp/app.config.ts',
    });
    mockedBuildProject.mockResolvedValue();
    mockedCopyAssets.mockResolvedValue();
    mockedWriteZephyrRuntimeConfig.mockReturnValue('/tmp/demo/dist/sparkling.zephyr.json');
    mockedUploadOutputToZephyr.mockResolvedValue({
      deploymentUrl: 'https://deploy.example.com',
    });
  });

  it('builds and uploads with ssr disabled', async () => {
    await deployToZephyr({
      cwd: '/tmp/demo',
    });

    expect(mockedBuildProject).toHaveBeenCalledWith({
      cwd: '/tmp/demo',
      configFile: 'app.config.ts',
      skipCopy: true,
    });
    expect(mockedUploadOutputToZephyr).toHaveBeenCalledWith({
      rootDir: '/tmp/demo',
      outputDir: 'dist',
      builder: 'rspack',
      target: 'ios',
      ssr: false,
    });
    expect(mockedWriteZephyrRuntimeConfig).toHaveBeenCalledWith({
      cwd: '/tmp/demo',
      config: {
        lynxConfig: {},
        zephyr: {
          ios: { target: 'ios' },
        },
      },
      sourceDir: 'dist',
      versionUrlOverride: 'https://deploy.example.com',
    });
    expect(mockedCopyAssets).toHaveBeenCalledWith({
      cwd: '/tmp/demo',
      source: 'dist',
      androidDest: 'android/app/src/main/assets',
      iosDest: 'ios/LynxResources/Assets',
    });
  });

  it('preserves configured versionUrl for OTA when present', async () => {
    mockedLoadAppConfig.mockResolvedValueOnce({
      config: {
        lynxConfig: {},
        zephyr: {
          versionUrl: 'https://stable.example.com',
          ios: { target: 'ios' },
        },
      } as any,
      configPath: '/tmp/app.config.ts',
    });

    await deployToZephyr({
      cwd: '/tmp/demo',
    });

    expect(mockedWriteZephyrRuntimeConfig).toHaveBeenCalledWith({
      cwd: '/tmp/demo',
      config: {
        lynxConfig: {},
        zephyr: {
          versionUrl: 'https://stable.example.com',
          ios: { target: 'ios' },
        },
      },
      sourceDir: 'dist',
      versionUrlOverride: 'https://stable.example.com',
    });
  });
});
