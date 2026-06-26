// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'fs-extra';
import path from 'node:path';
import os from 'node:os';
import { autolink } from '../commands/autolink';

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Create a unique temp directory for each test. */
function makeTmpDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling-autolink-test-'));
}

/** Minimal Podfile with the sparkling_methods_dep placeholder. */
function podfileTemplate(existingPods = ''): string {
  return [
    "platform :ios, '12.0'",
    '',
    'def sparkling_methods_dep',
    existingPods || '  # placeholder',
    'end',
    '',
    "target 'SparklingGo' do",
    '  sparkling_methods_dep',
    'end',
  ].join('\n');
}

/** Minimal settings.gradle.kts with optional existing autolink block. */
function settingsTemplate(existingBlock = ''): string {
  return [
    'rootProject.name = "Sparkling"',
    'include(":app")',
    existingBlock,
  ].join('\n');
}

/** Minimal build.gradle.kts with a dependencies block. */
function buildGradleTemplate(existingBlock = ''): string {
  return [
    'plugins {',
    '    alias(libs.plugins.android.application)',
    '}',
    '',
    'android {',
    '    namespace = "com.example.sparkling.go"',
    '}',
    '',
    'dependencies {',
    '    implementation(libs.androidx.core.ktx)',
    existingBlock,
    '}',
  ].join('\n');
}

/** Write a module.config.json inside a sparkling method package in node_modules. */
function createMethodModule(
  cwd: string,
  name: string,
  opts: {
    iosModuleName?: string;
    iosClassName?: string;
    androidPackage?: string;
    androidClassName?: string;
    androidMavenDependency?: string;
    devtool?: boolean;
    packageVersion?: string;
    methods?: string[];
  } = {},
): string {
  const moduleDir = path.join(cwd, 'node_modules', name);
  fs.mkdirpSync(path.join(moduleDir, 'ios'));
  fs.mkdirpSync(path.join(moduleDir, 'android'));

  // Write a minimal podspec so resolvePodName can derive the pod name
  const podspecContent = `Pod::Spec.new do |s|\n  s.name = '${name}'\nend`;
  fs.writeFileSync(path.join(moduleDir, 'ios', `${name}.podspec`), podspecContent);

  // Write a minimal build.gradle.kts
  fs.writeFileSync(path.join(moduleDir, 'android', 'build.gradle.kts'), 'plugins {}');

  fs.writeFileSync(path.join(moduleDir, 'package.json'), JSON.stringify({
    name,
    version: opts.packageVersion ?? '1.0.0',
  }, null, 2));

  const config: Record<string, unknown> = { name };
  if (opts.devtool) {
    config.devtool = true;
  }
  if (opts.methods?.length) {
    config.methods = Object.fromEntries(opts.methods.map(method => [method, {}]));
  }
  if (opts.iosModuleName || opts.iosClassName || opts.devtool) {
    config.ios = {
      moduleName: opts.iosModuleName ?? name,
      className: opts.iosClassName ?? `${name}Module`,
      podspecPath: path.join(moduleDir, 'ios', `${name}.podspec`),
    };
  }
  if (opts.androidPackage || opts.androidClassName || opts.androidMavenDependency) {
    config.android = {
      packageName: opts.androidPackage ?? `com.sparkling.${name}`,
      className: opts.androidClassName ?? `${name}Module`,
      ...(opts.androidMavenDependency ? { mavenDependency: opts.androidMavenDependency } : {}),
    };
  }

  fs.writeFileSync(path.join(moduleDir, 'module.config.json'), JSON.stringify(config, null, 2));
  return moduleDir;
}

/** Scaffold the minimal iOS + Android project structure. */
function scaffoldProject(cwd: string, opts?: { podfileContent?: string; settingsContent?: string; buildGradleContent?: string }): void {
  // iOS
  const iosDir = path.join(cwd, 'ios');
  const swiftDir = path.join(iosDir, 'SparklingGo', 'SparklingGo');
  fs.mkdirpSync(swiftDir);
  fs.writeFileSync(path.join(iosDir, 'Podfile'), opts?.podfileContent ?? podfileTemplate());

  // Android
  const androidAppDir = path.join(cwd, 'android', 'app');
  fs.mkdirpSync(androidAppDir);
  fs.writeFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), opts?.settingsContent ?? settingsTemplate());
  fs.writeFileSync(path.join(androidAppDir, 'build.gradle.kts'), opts?.buildGradleContent ?? buildGradleTemplate());

  // node_modules (empty)
  fs.mkdirpSync(path.join(cwd, 'node_modules'));
}

// ── Mock loadAppConfig so we don't need a real app.config.ts ─────────────────

jest.mock('../config', () => ({
  loadAppConfig: jest.fn().mockResolvedValue({
    config: {
      lynxConfig: {},
      platform: {
        android: { packageName: 'com.test.app' },
        ios: { bundleIdentifier: 'com.test.app' },
      },
    },
    configPath: '/mock/app.config.ts',
  }),
}));

// ── Tests ────────────────────────────────────────────────────────────────────

describe('autolink', () => {
  let cwd: string;

  beforeEach(() => {
    cwd = makeTmpDir();
  });

  afterEach(() => {
    fs.removeSync(cwd);
  });

  // ── One module ──────────────────────────────────────────────────────────

  describe('one sparkling method module', () => {
    beforeEach(() => {
      scaffoldProject(cwd);
      createMethodModule(cwd, 'sparkling-navigation', {
        iosModuleName: 'Router',
        iosClassName: 'RouterModule',
        androidPackage: 'com.sparkling.navigation',
        androidClassName: 'NavigationModule',
      });
    });

    it('injects the module into the Podfile', async () => {
      await autolink({ cwd, platform: 'ios' });

      const podfile = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      expect(podfile).toContain('# BEGIN SPARKLING AUTOLINK');
      expect(podfile).toContain('# END SPARKLING AUTOLINK');
      expect(podfile).toContain("pod 'sparkling-navigation'");
      expect(podfile).not.toContain('# No Sparkling methods found');
    });

    it('writes the iOS registry with one entry', async () => {
      await autolink({ cwd, platform: 'ios' });

      const registry = fs.readFileSync(
        path.join(cwd, 'ios', 'SparklingGo', 'SparklingGo', 'SparklingAutolink.swift'),
        'utf8',
      );
      expect(registry).toContain('sparkling-navigation');
      expect(registry).toContain('Router');
      expect(registry).toContain('RouterModule');
    });

    it('injects the module into settings.gradle.kts', async () => {
      await autolink({ cwd, platform: 'android' });

      const settings = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      expect(settings).toContain('// BEGIN SPARKLING AUTOLINK');
      expect(settings).toContain('// END SPARKLING AUTOLINK');
      expect(settings).toContain('"sparkling-navigation"');
    });

    it('injects the module into build.gradle.kts dependencies', async () => {
      await autolink({ cwd, platform: 'android' });

      const gradle = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');
      expect(gradle).toContain('// BEGIN SPARKLING AUTOLINK');
      expect(gradle).toContain('// END SPARKLING AUTOLINK');
      expect(gradle).toContain(':sparkling-navigation');
    });

    it('writes the Android registry with one entry', async () => {
      await autolink({ cwd, platform: 'android' });

      const registryPath = path.join(cwd, 'android', 'app', 'src', 'main', 'java', 'com', 'test', 'app', 'SparklingAutolink.kt');
      expect(fs.existsSync(registryPath)).toBe(true);
      const registry = fs.readFileSync(registryPath, 'utf8');
      expect(registry).toContain('sparkling-navigation');
      expect(registry).toContain('com.sparkling.navigation');
      expect(registry).toContain('NavigationModule');
    });

    it('writes ktlint-compliant Android autolink output', async () => {
      await autolink({ cwd, platform: 'android' });

      const settings = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      expect(settings).toContain([
        'val sparklingAutolinkProjects =',
        '    listOf<Pair<String, java.io.File>>(',
        '        "sparkling-navigation" to file("../node_modules/sparkling-navigation/android"),',
        '    )',
      ].join('\n'));

      const gradle = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');
      expect(gradle).toContain([
        '    listOf(',
        '        project(":sparkling-navigation"),',
        '    ).forEach { dep -> add("implementation", dep) }',
      ].join('\n'));

      const registryPath = path.join(cwd, 'android', 'app', 'src', 'main', 'java', 'com', 'test', 'app', 'SparklingAutolink.kt');
      const registry = fs.readFileSync(registryPath, 'utf8');
      expect(registry).toContain([
        'data class SparklingAutolinkModule(',
        '    val name: String,',
        '    val androidPackage: String?,',
        '    val className: String?,',
        '    val methodClassNames: List<String> = emptyList(),',
        ')',
      ].join('\n'));
      expect(registry).toContain([
        '            SparklingAutolinkModule(',
        '                name = "sparkling-navigation",',
        '                androidPackage = "com.sparkling.navigation",',
        '                className = "NavigationModule",',
        '            ),',
      ].join('\n'));
    });

    it('writes inferred Android method class names for modules with multiple methods', async () => {
      fs.removeSync(path.join(cwd, 'node_modules', 'sparkling-navigation'));
      createMethodModule(cwd, 'sparkling-storage', {
        androidPackage: 'com.tiktok.sparkling.method.storage',
        androidClassName: 'StorageMethod',
        methods: ['setItem', 'getItem', 'removeItem'],
      });

      await autolink({ cwd, platform: 'android' });

      const registryPath = path.join(cwd, 'android', 'app', 'src', 'main', 'java', 'com', 'test', 'app', 'SparklingAutolink.kt');
      const registry = fs.readFileSync(registryPath, 'utf8');
      expect(registry).toContain(['methodClassNames =', '                    listOf('].join('\n'));
      expect(registry).toContain('"com.tiktok.sparkling.method.storage.setItem.StorageSetItemMethod"');
      expect(registry).toContain('"com.tiktok.sparkling.method.storage.getItem.StorageGetItemMethod"');
      expect(registry).toContain('"com.tiktok.sparkling.method.storage.removeItem.StorageRemoveItemMethod"');
    });

    it('uses a remote Maven dependency for Android devtool modules', async () => {
      fs.removeSync(path.join(cwd, 'node_modules', 'sparkling-navigation'));
      createMethodModule(cwd, 'sparkling-debug-tool', {
        devtool: true,
        androidPackage: 'com.tiktok.sparkling.debugtool',
        androidClassName: 'SparklingDebugTool',
        androidMavenDependency: 'com.tiktok.sparkling:sparkling-debug-tool',
        packageVersion: '9.9.9-rc.1',
      });

      await autolink({ cwd, platform: 'android' });

      const settings = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      expect(settings).not.toContain('sparkling-debug-tool');

      const gradle = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');
      expect(gradle).toContain('debugImplementation("com.tiktok.sparkling:sparkling-debug-tool:9.9.9-rc.1")');
      expect(gradle).not.toContain('project(":sparkling-debug-tool")');

      const registryPath = path.join(cwd, 'android', 'app', 'src', 'main', 'java', 'com', 'test', 'app', 'SparklingAutolink.kt');
      const registry = fs.readFileSync(registryPath, 'utf8');
      expect(registry).not.toContain('sparkling-debug-tool');
    });

    it('preserves fixed remote Android debug-tool deps and restores method links', async () => {
      const staleSettingsBlock = [
        '// BEGIN SPARKLING AUTOLINK',
        'val sparklingAutolinkProjects =',
        '    listOf<Pair<String, java.io.File>>(',
        '        "sparkling-navigation" to file("../node_modules/sparkling-navigation/android"),',
        '        "sparkling-debug-tool" to file("../node_modules/sparkling-debug-tool/android"),',
        '    )',
        'sparklingAutolinkProjects.forEach { (name, dir) ->',
        '    include(":$name")',
        '    project(":$name").projectDir = dir',
        '}',
        '// END SPARKLING AUTOLINK',
      ].join('\n');
      const fixedRemoteDependency = '    debugImplementation("com.tiktok.sparkling:sparkling-debug-tool:9.9.9-rc.1")';
      const staleDependenciesBlock = [
        fixedRemoteDependency,
        '    // BEGIN SPARKLING AUTOLINK',
        '    implementation(project(":sparkling-navigation"))',
        '    debugImplementation(project(":sparkling-debug-tool"))',
        '    // END SPARKLING AUTOLINK',
      ].join('\n');

      scaffoldProject(cwd, {
        settingsContent: settingsTemplate(staleSettingsBlock),
        buildGradleContent: buildGradleTemplate(staleDependenciesBlock),
      });
      createMethodModule(cwd, 'sparkling-navigation', {
        iosModuleName: 'Router',
        iosClassName: 'RouterModule',
        androidPackage: 'com.sparkling.navigation',
        androidClassName: 'NavigationModule',
      });

      await autolink({ cwd, platform: 'android' });

      const settings = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      expect(settings).toContain('"sparkling-navigation"');
      expect(settings).not.toContain('sparkling-debug-tool');

      const gradle = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');
      expect(gradle).toContain(fixedRemoteDependency.trim());
      expect(gradle).toContain('project(":sparkling-navigation")');
      expect(gradle).not.toContain('project(":sparkling-debug-tool")');
    });

    it('returns the discovered module', async () => {
      const modules = await autolink({ cwd, platform: 'all' });
      expect(modules).toHaveLength(1);
      expect(modules[0].name).toBe('sparkling-navigation');
    });
  });

  // ── Multiple modules ───────────────────────────────────────────────────

  describe('multiple sparkling method modules', () => {
    beforeEach(() => {
      scaffoldProject(cwd);
      createMethodModule(cwd, 'sparkling-navigation', {
        iosModuleName: 'Router',
        iosClassName: 'RouterModule',
        androidPackage: 'com.sparkling.navigation',
        androidClassName: 'NavigationModule',
      });
      createMethodModule(cwd, 'sparkling-storage', {
        iosModuleName: 'Storage',
        iosClassName: 'StorageModule',
        androidPackage: 'com.sparkling.storage',
        androidClassName: 'StorageModule',
      });
      createMethodModule(cwd, 'sparkling-media', {
        iosModuleName: 'Media',
        iosClassName: 'MediaModule',
        androidPackage: 'com.sparkling.media',
        androidClassName: 'MediaModule',
      });
    });

    it('injects all modules into the Podfile', async () => {
      await autolink({ cwd, platform: 'ios' });

      const podfile = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      expect(podfile).toContain("pod 'sparkling-navigation'");
      expect(podfile).toContain("pod 'sparkling-storage'");
      expect(podfile).toContain("pod 'sparkling-media'");
    });

    it('writes the iOS registry with all entries', async () => {
      await autolink({ cwd, platform: 'ios' });

      const registry = fs.readFileSync(
        path.join(cwd, 'ios', 'SparklingGo', 'SparklingGo', 'SparklingAutolink.swift'),
        'utf8',
      );
      expect(registry).toContain('sparkling-navigation');
      expect(registry).toContain('sparkling-storage');
      expect(registry).toContain('sparkling-media');
    });

    it('injects all modules into settings.gradle.kts', async () => {
      await autolink({ cwd, platform: 'android' });

      const settings = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      expect(settings).toContain('"sparkling-navigation"');
      expect(settings).toContain('"sparkling-storage"');
      expect(settings).toContain('"sparkling-media"');
      expect(settings).not.toContain(',,');
    });

    it('injects all modules into build.gradle.kts dependencies', async () => {
      await autolink({ cwd, platform: 'android' });

      const gradle = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');
      expect(gradle).toContain(':sparkling-navigation');
      expect(gradle).toContain(':sparkling-storage');
      expect(gradle).toContain(':sparkling-media');
      expect(gradle).not.toContain(',,');
    });

    it('writes the Android registry with all entries', async () => {
      await autolink({ cwd, platform: 'android' });

      const registryPath = path.join(cwd, 'android', 'app', 'src', 'main', 'java', 'com', 'test', 'app', 'SparklingAutolink.kt');
      const registry = fs.readFileSync(registryPath, 'utf8');
      expect(registry).toContain('sparkling-navigation');
      expect(registry).toContain('sparkling-storage');
      expect(registry).toContain('sparkling-media');
    });

    it('returns all discovered modules', async () => {
      const modules = await autolink({ cwd, platform: 'all' });
      expect(modules).toHaveLength(3);
      const names = modules.map(m => m.name).sort();
      expect(names).toEqual(['sparkling-media', 'sparkling-navigation', 'sparkling-storage']);
    });
  });

  // ── Zero modules ───────────────────────────────────────────────────────

  describe('no sparkling method modules', () => {
    it('cleans stale Podfile entries when all modules are removed', async () => {
      // Start with one module linked
      scaffoldProject(cwd, {
        podfileContent: podfileTemplate([
          '  # BEGIN SPARKLING AUTOLINK',
          "  pod 'sparkling-navigation', :path => '../node_modules/sparkling-navigation/ios'",
          '  # END SPARKLING AUTOLINK',
        ].join('\n')),
      });

      // No modules in node_modules — simulate user removing sparkling-navigation
      await autolink({ cwd, platform: 'ios' });

      const podfile = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      expect(podfile).toContain('# BEGIN SPARKLING AUTOLINK');
      expect(podfile).toContain('# No Sparkling methods found');
      expect(podfile).not.toContain('sparkling-navigation');
    });

    it('preserves fixed remote iOS debug-tool pods when the npm package is not installed', async () => {
      const podfileContent = [
        "platform :ios, '12.0'",
        '',
        'def sparkling_methods_dep',
        '  # BEGIN SPARKLING AUTOLINK',
        "  pod 'sparkling-navigation', :path => '../node_modules/sparkling-navigation/ios'",
        '  # END SPARKLING AUTOLINK',
        'end',
        '',
        'def sparkling_devtool',
        "  pod 'Sparkling-DebugTool', '9.9.9-rc.1'",
        '  # BEGIN SPARKLING AUTOLINK',
        "  pod 'sparkling-debug-tool', :path => '../node_modules/sparkling-debug-tool/ios'",
        '  # END SPARKLING AUTOLINK',
        "  pod 'DebugRouter', '5.0.15'",
        'end',
        '',
        "target 'SparklingGoInHouse' do",
        '  sparkling_devtool',
        'end',
      ].join('\n');
      scaffoldProject(cwd, { podfileContent });

      await autolink({ cwd, platform: 'ios' });

      const podfile = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      expect(podfile).toContain("pod 'Sparkling-DebugTool', '9.9.9-rc.1'");
      expect(podfile).toContain("pod 'DebugRouter', '5.0.15'");
      expect(podfile).toContain('# No extra devtool modules');
      expect(podfile).not.toContain("pod 'sparkling-debug-tool', :path");
      expect(podfile).not.toContain("pod 'sparkling-navigation', :path");
    });

    it('cleans stale settings.gradle.kts entries when all modules are removed', async () => {
      const staleBlock = [
        '',
        '// BEGIN SPARKLING AUTOLINK',
        'val sparklingAutolinkProjects = listOf<Pair<String, java.io.File>>()',
        'sparklingAutolinkProjects.forEach { (name, dir) ->',
        '    include(":$name")',
        '    project(":$name").projectDir = dir',
        '}',
        '// END SPARKLING AUTOLINK',
      ].join('\n');

      scaffoldProject(cwd, { settingsContent: settingsTemplate(staleBlock) });

      await autolink({ cwd, platform: 'android' });

      const settings = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      expect(settings).not.toContain('// BEGIN SPARKLING AUTOLINK');
      expect(settings).not.toContain('// END SPARKLING AUTOLINK');
    });

    it('cleans stale build.gradle.kts entries when all modules are removed', async () => {
      const staleBlock = [
        '    // BEGIN SPARKLING AUTOLINK',
        '    listOf(',
        '        project(":sparkling-navigation")',
        '    ).forEach { dep -> add("implementation", dep) }',
        '    // END SPARKLING AUTOLINK',
      ].join('\n');

      scaffoldProject(cwd, { buildGradleContent: buildGradleTemplate(staleBlock) });

      await autolink({ cwd, platform: 'android' });

      const gradle = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');
      expect(gradle).not.toContain('// BEGIN SPARKLING AUTOLINK');
      expect(gradle).not.toContain('sparkling-navigation');
    });

    it('writes empty iOS registry when no modules exist', async () => {
      scaffoldProject(cwd);

      await autolink({ cwd, platform: 'ios' });

      const registryPath = path.join(cwd, 'ios', 'SparklingGo', 'SparklingGo', 'SparklingAutolink.swift');
      expect(fs.existsSync(registryPath)).toBe(true);
      const registry = fs.readFileSync(registryPath, 'utf8');
      expect(registry).toContain('sparklingAutolinkModules');
      // The array should be empty (only whitespace between brackets)
      const match = registry.match(/sparklingAutolinkModules.*?=\s*\[([\s\S]*?)\]/);
      expect(match).toBeTruthy();
      expect(match![1].trim()).toBe('');
    });

    it('writes empty Android registry when no modules exist', async () => {
      scaffoldProject(cwd);

      await autolink({ cwd, platform: 'android' });

      const registryPath = path.join(cwd, 'android', 'app', 'src', 'main', 'java', 'com', 'test', 'app', 'SparklingAutolink.kt');
      expect(fs.existsSync(registryPath)).toBe(true);
      const registry = fs.readFileSync(registryPath, 'utf8');
      expect(registry).toContain('val modules = emptyList<SparklingAutolinkModule>()');
    });

    it('returns empty array', async () => {
      scaffoldProject(cwd);
      const modules = await autolink({ cwd, platform: 'all' });
      expect(modules).toHaveLength(0);
    });
  });

  // ── Idempotency ────────────────────────────────────────────────────────

  describe('idempotency', () => {
    it('running autolink twice produces the same result', async () => {
      scaffoldProject(cwd);
      createMethodModule(cwd, 'sparkling-navigation', {
        iosModuleName: 'Router',
        iosClassName: 'RouterModule',
      });

      await autolink({ cwd, platform: 'all' });
      const podfileAfterFirst = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      const settingsAfterFirst = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      const gradleAfterFirst = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');

      await autolink({ cwd, platform: 'all' });
      const podfileAfterSecond = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      const settingsAfterSecond = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      const gradleAfterSecond = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');

      expect(podfileAfterSecond).toBe(podfileAfterFirst);
      expect(settingsAfterSecond).toBe(settingsAfterFirst);
      expect(gradleAfterSecond).toBe(gradleAfterFirst);
    });

    it('adding then removing a module restores clean state', async () => {
      scaffoldProject(cwd);

      // First: link a module
      createMethodModule(cwd, 'sparkling-navigation', {
        iosModuleName: 'Router',
        iosClassName: 'RouterModule',
      });
      await autolink({ cwd, platform: 'all' });

      // Verify it was registered for Sparkling runtime metadata.
      const registryWithModule = fs.readFileSync(
        path.join(cwd, 'ios', 'SparklingGo', 'SparklingGo', 'SparklingAutolink.swift'),
        'utf8',
      );
      expect(registryWithModule).toContain('sparkling-navigation');

      // Second: remove the module and re-autolink
      fs.removeSync(path.join(cwd, 'node_modules', 'sparkling-navigation'));
      await autolink({ cwd, platform: 'all' });

      // Verify stale entries are cleaned
      const podfileClean = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      expect(podfileClean).not.toContain('sparkling-navigation');
      expect(podfileClean).toContain('# BEGIN SPARKLING AUTOLINK');
      expect(podfileClean).toContain('# No Sparkling methods found');

      const settings = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      expect(settings).not.toContain('sparkling-navigation');

      const gradle = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');
      expect(gradle).not.toContain('sparkling-navigation');
    });
  });

  // ── Debug tool ────────────────────────────────────────────────────────

  describe('debug tool module', () => {
    beforeEach(() => {
      scaffoldProject(cwd);
      createMethodModule(cwd, 'sparkling-debug-tool', {
        iosModuleName: 'DebugTool',
        iosClassName: 'SparklingDebugTool',
        androidPackage: 'com.sparkling.debugtool',
        androidClassName: 'SparklingDebugTool',
        devtool: true,
      });
    });

    it('injects debug-only Android linking for devtool modules', async () => {
      await autolink({ cwd, platform: 'android' });

      const settings = fs.readFileSync(path.join(cwd, 'android', 'settings.gradle.kts'), 'utf8');
      const gradle = fs.readFileSync(path.join(cwd, 'android', 'app', 'build.gradle.kts'), 'utf8');
      expect(settings).toContain('"sparkling-debug-tool"');
      expect(gradle).toContain('debugImplementation(project(":sparkling-debug-tool"))');
    });

    it('prefers local Android project linking for workspace devtool modules', async () => {
      const workspace = makeTmpDir();
      try {
        const appDir = path.join(workspace, 'apps', 'playground');
        const moduleDir = path.join(workspace, 'packages', 'sparkling-debug-tool');
        fs.mkdirpSync(path.join(moduleDir, 'android'));
        fs.writeFileSync(
          path.join(workspace, 'pnpm-workspace.yaml'),
          ['packages:', '  - "apps/*"', '  - "packages/*"', ''].join('\n'),
        );
        fs.writeFileSync(path.join(moduleDir, 'android', 'build.gradle.kts'), 'plugins {}');
        fs.writeFileSync(path.join(moduleDir, 'package.json'), JSON.stringify({
          name: 'sparkling-debug-tool',
          version: '9.9.9-rc.1',
        }, null, 2));
        fs.writeFileSync(path.join(moduleDir, 'module.config.json'), JSON.stringify({
          name: 'sparkling-debug-tool',
          devtool: true,
          android: {
            packageName: 'com.sparkling.debugtool',
            className: 'SparklingDebugTool',
            mavenDependency: 'com.tiktok.sparkling:sparkling-debug-tool',
            buildGradle: 'android/build.gradle.kts',
          },
        }, null, 2));
        scaffoldProject(appDir);

        await autolink({ cwd: appDir, platform: 'android' });

        const settings = fs.readFileSync(path.join(appDir, 'android', 'settings.gradle.kts'), 'utf8');
        const gradle = fs.readFileSync(path.join(appDir, 'android', 'app', 'build.gradle.kts'), 'utf8');
        expect(settings).toContain('"sparkling-debug-tool"');
        expect(gradle).toContain('debugImplementation(project(":sparkling-debug-tool"))');
        expect(gradle).not.toContain('com.tiktok.sparkling:sparkling-debug-tool');
      } finally {
        fs.removeSync(workspace);
      }
    });

    it('injects debug-only iOS pods for devtool modules', async () => {
      await autolink({ cwd, platform: 'ios' });

      const podfile = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      expect(podfile).toContain("pod 'sparkling-debug-tool'");
    });

    it('skips iOS autolink without the methods block', async () => {
      scaffoldProject(cwd, {
        podfileContent: [
          "platform :ios, '12.0'",
          '',
          "target 'SparklingGo' do",
          'end',
        ].join('\n'),
      });
      createMethodModule(cwd, 'sparkling-debug-tool', {
        iosModuleName: 'DebugTool',
        iosClassName: 'SparklingDebugTool',
        devtool: true,
      });

      await autolink({ cwd, platform: 'ios' });

      const podfile = fs.readFileSync(path.join(cwd, 'ios', 'Podfile'), 'utf8');
      expect(podfile).not.toContain('def sparkling_devtool');
      expect(podfile).not.toContain("pod 'sparkling-debug-tool'");
    });

    it('does not include devtool modules in Sparkling registries', async () => {
      await autolink({ cwd, platform: 'all' });

      const androidRegistry = fs.readFileSync(
        path.join(cwd, 'android', 'app', 'src', 'main', 'java', 'com', 'test', 'app', 'SparklingAutolink.kt'),
        'utf8',
      );
      const iosRegistry = fs.readFileSync(
        path.join(cwd, 'ios', 'SparklingGo', 'SparklingGo', 'SparklingAutolink.swift'),
        'utf8',
      );
      expect(androidRegistry).not.toContain('sparkling-debug-tool');
      expect(iosRegistry).not.toContain('sparkling-debug-tool');
    });
  });
});
