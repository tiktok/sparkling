import fs from 'fs-extra';
import os from 'os';
import path from 'path';
import * as p from '@clack/prompts';
import { runInit } from '../../create';
import type { InitOptions } from '../../create/types';

jest.mock('@clack/prompts', () => ({
  text: jest.fn(),
  select: jest.fn(),
  confirm: jest.fn(),
  multiselect: jest.fn(),
  isCancel: jest.fn(() => false),
}));

jest.mock('chalk', () => ({
  bold: jest.fn((str) => str),
  dim: jest.fn((str) => str),
  cyan: jest.fn((str) => str),
  green: jest.fn((str) => str),
  yellow: jest.fn((str) => str),
  red: jest.fn((str) => str),
  white: jest.fn((str) => str),
  gray: jest.fn((str) => str),
  grey: jest.fn((str) => str),
  magenta: jest.fn((str) => str),
  blue: jest.fn((str) => str)
}));

function mockModuleInfoPrompts(packageName: string, moduleName: string, androidDsl: string = 'kts') {
  (p.text as jest.Mock)
    .mockResolvedValueOnce(packageName)
    .mockResolvedValueOnce(moduleName);
  (p.select as jest.Mock).mockResolvedValueOnce(androidDsl);
}

async function withTempDir(fn: (dir: string) => Promise<void>): Promise<void> {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'create-test-'));
  const originalCwd = process.cwd();
  try {
    process.chdir(tempDir);
    await fn(tempDir);
  } finally {
    process.chdir(originalCwd);
    await fs.remove(tempDir);
  }
}

async function createMockTemplate(dir: string): Promise<string> {
  const templateDir = path.join(dir, 'template');
  await fs.ensureDir(path.join(templateDir, 'src'));
  await fs.writeFile(
    path.join(templateDir, 'src', 'method.d.ts'),
    'interface TestRequest { message: string; }\ninterface TestResponse { success: boolean; }\ndeclare function test(req: TestRequest, cb: (res: TestResponse) => void): void;'
  );
  await fs.ensureDir(path.join(templateDir, 'android'));
  await fs.ensureDir(path.join(templateDir, 'ios'));
  await fs.writeFile(path.join(templateDir, 'README.md'), '# Template README');
  return templateDir;
}

describe('Project Creation (runInit)', () => {
  let consoleLogSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    consoleLogSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleLogSpy.mockRestore();
  });

  describe('basic project creation', () => {
    it('should create project with provided name', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.test', 'TestModule', 'kts');

        await runInit('test-project', options);

        const projectDir = path.join(tmpDir, 'test-project');
        expect(await fs.pathExists(projectDir)).toBe(true);

        // Check package.json
        const pkgJsonPath = path.join(projectDir, 'package.json');
        expect(await fs.pathExists(pkgJsonPath)).toBe(true);
        const pkgJson = await fs.readJson(pkgJsonPath);
        expect(pkgJson.name).toBe('test-project');

        // Check module config
        const configPath = path.join(projectDir, 'module.config.json');
        expect(await fs.pathExists(configPath)).toBe(true);
        const config = await fs.readJson(configPath);
        expect(config.packageName).toBe('com.example.test');
        expect(config.moduleName).toBe('TestModule');
      });
    });

    it('should prompt for project name when not provided', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        // First text call is for promptProjectName, then two more for promptModuleInfo
        (p.text as jest.Mock)
          .mockResolvedValueOnce('prompted-project')
          .mockResolvedValueOnce('com.example.prompted')
          .mockResolvedValueOnce('PromptedModule');
        (p.select as jest.Mock).mockResolvedValueOnce('kts');

        await runInit(undefined, options);

        expect(p.text).toHaveBeenCalledTimes(3);

        const projectDir = path.join(tmpDir, 'prompted-project');
        expect(await fs.pathExists(projectDir)).toBe(true);

        const pkgJson = await fs.readJson(path.join(projectDir, 'package.json'));
        expect(pkgJson.name).toBe('prompted-project');
      });
    });

    it('should trim whitespace from project name', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.trimmed', 'TrimmedModule', 'kts');

        await runInit('  trimmed-project  ', options);

        const projectDir = path.join(tmpDir, 'trimmed-project');
        expect(await fs.pathExists(projectDir)).toBe(true);

        const pkgJson = await fs.readJson(path.join(projectDir, 'package.json'));
        expect(pkgJson.name).toBe('trimmed-project');
      });
    });
  });

  describe('template handling', () => {
    it('should copy template files to project directory', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.copy', 'CopyModule', 'kts');

        await runInit('copy-test', options);

        const projectDir = path.join(tmpDir, 'copy-test');

        // Check template files were copied
        expect(await fs.pathExists(path.join(projectDir, 'src', 'method.d.ts'))).toBe(true);
        expect(await fs.pathExists(path.join(projectDir, 'android'))).toBe(true);
        expect(await fs.pathExists(path.join(projectDir, 'ios'))).toBe(true);
        expect(await fs.pathExists(path.join(projectDir, 'README.md'))).toBe(true);

        const methodDts = await fs.readFile(path.join(projectDir, 'src', 'method.d.ts'), 'utf8');
        expect(methodDts).toContain('TestRequest');
        expect(methodDts).toContain('TestResponse');
      });
    });

    it('should handle missing template directory gracefully', async () => {
      await withTempDir(async () => {
        const nonExistentTemplate = '/non/existent/template';
        const options: InitOptions = { template: nonExistentTemplate };

        await expect(runInit('fail-project', options)).rejects.toThrow();
      });
    });

    it('should use default template when none specified', async () => {
      await withTempDir(async (tmpDir) => {
        // Mock the template resolution to return a valid path
        const defaultTemplateDir = await createMockTemplate(tmpDir);
        jest.doMock('../../create/scaffold', () => ({
          ...jest.requireActual('../../create/scaffold'),
          resolveTemplateDir: jest.fn().mockResolvedValue(defaultTemplateDir)
        }));

        const options: InitOptions = {};

        mockModuleInfoPrompts('com.example.default', 'DefaultModule', 'kts');

        await runInit('default-template-test', options);

        const projectDir = path.join(tmpDir, 'default-template-test');
        expect(await fs.pathExists(projectDir)).toBe(true);
      });
    });
  });

  describe('module configuration', () => {
    it('should use provided module configuration', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.custom.package', 'CustomModule', 'groovy');

        await runInit('config-test', options);

        const configPath = path.join(tmpDir, 'config-test', 'module.config.json');
        const config = await fs.readJson(configPath);

        expect(config.packageName).toBe('com.custom.package');
        expect(config.moduleName).toBe('CustomModule');
        expect(config.androidDsl).toBe('groovy');
      });
    });

    it('should generate default module name from project name', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.toast', 'ToastModule', 'kts');

        await runInit('toast-module', options);

        const configPath = path.join(tmpDir, 'toast-module', 'module.config.json');
        const config = await fs.readJson(configPath);
        expect(config.moduleName).toBe('ToastModule');
      });
    });
  });

  describe('platform scaffolds', () => {
    it('should create Android platform structure', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.android', 'AndroidModule', 'kts');

        await runInit('android-test', options);

        const androidDir = path.join(tmpDir, 'android-test', 'android');
        expect(await fs.pathExists(androidDir)).toBe(true);

        // Check for Android-specific files that should be created
        const packagePath = path.join(androidDir, 'src', 'main', 'java', 'com', 'example', 'android');
        expect(await fs.pathExists(packagePath)).toBe(true);
      });
    });

    it('should create iOS platform structure', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.ios', 'IosModule', 'kts');

        await runInit('ios-test', options);

        const iosDir = path.join(tmpDir, 'ios-test', 'ios');
        expect(await fs.pathExists(iosDir)).toBe(true);

        // Check for iOS-specific structure
        const iosSourceDir = path.join(iosDir, 'Source', 'Core', 'IosModule');
        expect(await fs.pathExists(iosSourceDir)).toBe(true);
      });
    });
  });

  describe('error handling', () => {
    it('should handle file system errors gracefully', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);

        // Create a directory with the same name as the project to cause conflict
        const conflictDir = path.join(tmpDir, 'conflict-project');
        await fs.ensureDir(conflictDir);
        await fs.writeFile(path.join(conflictDir, 'package.json'), '{}');

        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.conflict', 'ConflictModule', 'kts');

        // Without force option, should handle existing directory
        await expect(runInit('conflict-project', options)).rejects.toThrow();
      });
    });

    it('should handle invalid package names', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('', 'TestModule', 'kts');

        // Should still create project but with potentially invalid config
        await runInit('invalid-package', options);

        const configPath = path.join(tmpDir, 'invalid-package', 'module.config.json');
        const config = await fs.readJson(configPath);
        expect(config.packageName).toBe('');
      });
    });

    it('should handle prompt cancellation', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        (p.text as jest.Mock).mockRejectedValueOnce(new Error('User cancelled'));

        await expect(runInit('cancelled-project', options)).rejects.toThrow('User cancelled');
      });
    });
  });

  describe('console output', () => {
    it('should display creation progress messages', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.output', 'OutputModule', 'kts');

        await runInit('output-test', options);

        expect(consoleLogSpy).toHaveBeenCalledWith(
          expect.stringContaining('Initializing sparkling method project')
        );
        expect(consoleLogSpy).toHaveBeenCalledWith(
          expect.stringContaining('Project created successfully')
        );
        expect(consoleLogSpy).toHaveBeenCalledWith(
          expect.stringContaining('Tip cd output-test')
        );
        expect(consoleLogSpy).toHaveBeenCalledWith(
          expect.stringContaining('Tip Edit and rename src/method.d.ts')
        );
        expect(consoleLogSpy).toHaveBeenCalledWith(
          expect.stringContaining('Tip Run `npm run codegen`')
        );
      });
    });

    it('should include project path in output', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.example.path', 'PathModule', 'kts');

        await runInit('path-test', options);

        const expectedPath = path.resolve(tmpDir, 'path-test');
        expect(consoleLogSpy).toHaveBeenCalledWith(
          expect.stringContaining(expectedPath)
        );
      });
    });
  });

  describe('integration scenarios', () => {
    it('should create complete project structure', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.integration.test', 'IntegrationModule', 'kts');

        await runInit('integration-test', options);

        const projectDir = path.join(tmpDir, 'integration-test');

        // Verify complete structure
        expect(await fs.pathExists(path.join(projectDir, 'package.json'))).toBe(true);
        expect(await fs.pathExists(path.join(projectDir, 'module.config.json'))).toBe(true);
        expect(await fs.pathExists(path.join(projectDir, 'src', 'method.d.ts'))).toBe(true);
        expect(await fs.pathExists(path.join(projectDir, 'android'))).toBe(true);
        expect(await fs.pathExists(path.join(projectDir, 'ios'))).toBe(true);

        // Verify package.json structure
        const pkgJson = await fs.readJson(path.join(projectDir, 'package.json'));
        expect(pkgJson).toHaveProperty('name', 'integration-test');
        expect(pkgJson).toHaveProperty('scripts');
        expect(pkgJson).toHaveProperty('dependencies');
      });
    });

    it('should handle complex package names and module names', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.complex.package.name.with.many.parts', 'VeryLongModuleNameWithCamelCase', 'kts');

        await runInit('complex-names-test', options);

        const configPath = path.join(tmpDir, 'complex-names-test', 'module.config.json');
        const config = await fs.readJson(configPath);

        expect(config.packageName).toBe('com.complex.package.name.with.many.parts');
        expect(config.moduleName).toBe('VeryLongModuleNameWithCamelCase');

        // Verify package structure was created correctly
        const packagePath = path.join(
          tmpDir, 'complex-names-test', 'android', 'src', 'main', 'java',
          'com', 'complex', 'package', 'name', 'with', 'many', 'parts'
        );
        expect(await fs.pathExists(packagePath)).toBe(true);
      });
    });
  });

  describe('non-interactive mode', () => {
    it('should create project without prompts when all options are provided', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = {
          template: templateDir,
          packageName: 'com.noninteractive.test',
          moduleName: 'NonInteractiveModule',
          androidDsl: 'kts',
        };

        await runInit('non-interactive-test', options);

        // Prompts should NOT have been called
        expect(p.text).not.toHaveBeenCalled();
        expect(p.select).not.toHaveBeenCalled();

        const projectDir = path.join(tmpDir, 'non-interactive-test');
        expect(await fs.pathExists(projectDir)).toBe(true);

        // Verify module config
        const config = await fs.readJson(path.join(projectDir, 'module.config.json'));
        expect(config.packageName).toBe('com.noninteractive.test');
        expect(config.moduleName).toBe('NonInteractiveModule');
        expect(config.androidDsl).toBe('kts');
        expect(config.projectName).toBe('non-interactive-test');

        // Verify package.json
        const pkgJson = await fs.readJson(path.join(projectDir, 'package.json'));
        expect(pkgJson.name).toBe('non-interactive-test');
      });
    });

    it('should create project with groovy DSL in non-interactive mode', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = {
          template: templateDir,
          packageName: 'com.groovy.test',
          moduleName: 'GroovyModule',
          androidDsl: 'groovy',
        };

        await runInit('groovy-test', options);

        expect(p.text).not.toHaveBeenCalled();
        expect(p.select).not.toHaveBeenCalled();

        const config = await fs.readJson(path.join(tmpDir, 'groovy-test', 'module.config.json'));
        expect(config.androidDsl).toBe('groovy');

        // Verify groovy build file was created (not kts)
        const gradlePath = path.join(tmpDir, 'groovy-test', 'android', 'build.gradle');
        expect(await fs.pathExists(gradlePath)).toBe(true);
      });
    });

    it('should default androidDsl to kts when not provided in non-interactive mode', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = {
          template: templateDir,
          packageName: 'com.default.dsl',
          moduleName: 'DefaultDslModule',
          // androidDsl intentionally omitted
        };

        await runInit('default-dsl-test', options);

        expect(p.text).not.toHaveBeenCalled();
        expect(p.select).not.toHaveBeenCalled();

        const config = await fs.readJson(path.join(tmpDir, 'default-dsl-test', 'module.config.json'));
        expect(config.androidDsl).toBe('kts');
      });
    });

    it('should reject invalid androidDsl value in non-interactive mode', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = {
          template: templateDir,
          packageName: 'com.invalid.dsl',
          moduleName: 'InvalidDslModule',
          androidDsl: 'maven' as any,
        };

        await expect(runInit('invalid-dsl-test', options)).rejects.toThrow(
          'Invalid --android-dsl value "maven". Must be "kts" or "groovy".'
        );
      });
    });

    it('should create correct platform scaffolds in non-interactive mode', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = {
          template: templateDir,
          packageName: 'com.platform.scaffold',
          moduleName: 'PlatformModule',
          androidDsl: 'kts',
        };

        await runInit('platform-test', options);

        const projectDir = path.join(tmpDir, 'platform-test');

        // Verify Android structure
        const androidDir = path.join(projectDir, 'android', 'src', 'main', 'java', 'com', 'platform', 'scaffold', 'platformmodule');
        expect(await fs.pathExists(androidDir)).toBe(true);

        // Verify iOS structure
        const iosDir = path.join(projectDir, 'ios', 'Source', 'Core', 'platformmodule');
        expect(await fs.pathExists(iosDir)).toBe(true);
      });
    });

    it('should fall back to interactive prompts when only packageName is provided', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = {
          template: templateDir,
          packageName: 'com.partial.test',
          // moduleName intentionally omitted
        };

        mockModuleInfoPrompts('com.partial.test', 'PartialModule', 'kts');

        await runInit('partial-test', options);

        // Prompts should have been called because moduleName was missing
        expect(p.text).toHaveBeenCalled();
      });
    });

    it('should fall back to interactive prompts when only moduleName is provided', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = {
          template: templateDir,
          moduleName: 'OnlyModule',
          // packageName intentionally omitted
        };

        mockModuleInfoPrompts('org.sparkling', 'OnlyModule', 'kts');

        await runInit('only-module-test', options);

        // Prompts should have been called because packageName was missing
        expect(p.text).toHaveBeenCalled();
      });
    });

    it('should trim whitespace from non-interactive option values', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = {
          template: templateDir,
          packageName: '  com.whitespace.test  ',
          moduleName: '  WhitespaceModule  ',
          androidDsl: 'kts',
        };

        await runInit('whitespace-test', options);

        expect(p.text).not.toHaveBeenCalled();
        expect(p.select).not.toHaveBeenCalled();

        const config = await fs.readJson(path.join(tmpDir, 'whitespace-test', 'module.config.json'));
        expect(config.packageName).toBe('com.whitespace.test');
        expect(config.moduleName).toBe('WhitespaceModule');
      });
    });

    it('should work with --force in non-interactive mode', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);

        // Create existing directory
        const existingDir = path.join(tmpDir, 'force-test');
        await fs.ensureDir(existingDir);
        await fs.writeFile(path.join(existingDir, 'old-file.txt'), 'old content');

        const options: InitOptions = {
          template: templateDir,
          force: true,
          packageName: 'com.force.test',
          moduleName: 'ForceModule',
          androidDsl: 'kts',
        };

        await runInit('force-test', options);

        expect(p.text).not.toHaveBeenCalled();
        expect(p.select).not.toHaveBeenCalled();

        // Old file should be gone, new project should exist
        expect(await fs.pathExists(path.join(existingDir, 'old-file.txt'))).toBe(false);
        expect(await fs.pathExists(path.join(existingDir, 'package.json'))).toBe(true);
      });
    });
  });

  describe('snapshot testing', () => {
    it('should create consistent project structure', async () => {
      await withTempDir(async (tmpDir) => {
        const templateDir = await createMockTemplate(tmpDir);
        const options: InitOptions = { template: templateDir };

        mockModuleInfoPrompts('com.snapshot.test', 'SnapshotModule', 'kts');

        await runInit('snapshot-test', options);

        const projectDir = path.join(tmpDir, 'snapshot-test');

        // Get file tree structure for snapshot
        const getFileTree = async (dir: string, relativeTo: string = dir): Promise<any> => {
          const items = await fs.readdir(dir);
          const tree: any = {};

          for (const item of items) {
            const itemPath = path.join(dir, item);
            const relativePath = path.relative(relativeTo, itemPath);
            const stat = await fs.stat(itemPath);

            if (stat.isDirectory()) {
              tree[item] = await getFileTree(itemPath, relativeTo);
            } else {
              tree[item] = 'file';
            }
          }

          return tree;
        };

        const projectStructure = await getFileTree(projectDir);
        expect(projectStructure).toMatchSnapshot('project-structure');
      });
    });
  });
});
