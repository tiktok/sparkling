// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import {
  applyPackageNamespace,
  deriveDefaultNamespace,
  isValidNamespace,
} from '../create-app/namespace';

const DEFAULT_ANDROID_NAMESPACE = 'com.example.sparkling.go';
const DEFAULT_IOS_BUNDLE_ID = 'com.sparkling.app.SparklingGo';
const DEFAULT_IOS_TEST_BUNDLE_ID = 'com.sparkling.app.SparklingGoTests';
const DEFAULT_IOS_UITEST_BUNDLE_ID = 'com.sparkling.app.SparklingGoUITests';
const DEFAULT_IOS_METHOD_TEST_BUNDLE_ID = 'com.sparkling.app.SparklingMethodTests';

describe('create-app/namespace', () => {
  describe('deriveDefaultNamespace', () => {
    test('lowercases and dot-separates names', () => {
      expect(deriveDefaultNamespace('My-Cool App')).toBe('com.my.cool.app');
    });
    test('preserves an explicit com. prefix', () => {
      expect(deriveDefaultNamespace('com.example.foo')).toBe('com.example.foo');
    });
    test('falls back when sanitization yields empty string', () => {
      expect(deriveDefaultNamespace('!!!')).toBe('com.sparkling.app');
    });
    test('collapses consecutive separators', () => {
      expect(deriveDefaultNamespace('foo!!!bar')).toBe('com.foo.bar');
    });
  });

  describe('isValidNamespace', () => {
    test('accepts well-formed dotted identifiers', () => {
      expect(isValidNamespace('com.foo.bar')).toBe(true);
      expect(isValidNamespace('a.b1.c_d')).toBe(true);
    });
    test('rejects single-segment or invalid characters', () => {
      expect(isValidNamespace('flat')).toBe(false);
      expect(isValidNamespace('foo..bar')).toBe(false);
      expect(isValidNamespace('1leading.foo')).toBe(false);
      expect(isValidNamespace('foo.bar!')).toBe(false);
    });
  });

  describe('applyPackageNamespace', () => {
    let work: string;

    beforeEach(() => {
      work = fs.mkdtempSync(path.join(os.tmpdir(), 'csa-ns-'));
    });

    afterEach(() => {
      fs.rmSync(work, { recursive: true, force: true });
    });

    test('is a no-op when neither android nor ios trees exist', () => {
      expect(() => applyPackageNamespace(work, 'com.demo.app')).not.toThrow();
    });

    test('rewrites android namespace, package files, and moves source dirs (main + test + androidTest)', () => {
      const appDir = path.join(work, 'android', 'app');
      const mainSrcDir = path.join(appDir, 'src', 'main', 'java', 'com', 'example', 'sparkling', 'go');
      const testSrcDir = path.join(appDir, 'src', 'test', 'java', 'com', 'example', 'sparkling', 'go');
      const androidTestSrcDir = path.join(
        appDir,
        'src',
        'androidTest',
        'java',
        'com',
        'example',
        'sparkling',
        'go',
      );
      fs.mkdirSync(mainSrcDir, { recursive: true });
      fs.mkdirSync(testSrcDir, { recursive: true });
      fs.mkdirSync(androidTestSrcDir, { recursive: true });
      fs.writeFileSync(
        path.join(mainSrcDir, 'MainActivity.kt'),
        `package ${DEFAULT_ANDROID_NAMESPACE}\nclass A`,
      );
      fs.writeFileSync(
        path.join(testSrcDir, 'AppUnitTest.kt'),
        `package ${DEFAULT_ANDROID_NAMESPACE}\nclass T`,
      );
      fs.writeFileSync(
        path.join(androidTestSrcDir, 'InstrumentedTest.kt'),
        `package ${DEFAULT_ANDROID_NAMESPACE}\nclass I`,
      );
      const buildGradle = path.join(appDir, 'build.gradle.kts');
      fs.writeFileSync(
        buildGradle,
        `namespace = "${DEFAULT_ANDROID_NAMESPACE}"\napplicationId = "${DEFAULT_ANDROID_NAMESPACE}"`,
      );

      applyPackageNamespace(work, 'com.demo.app');

      const buildGradleContent = fs.readFileSync(buildGradle, 'utf8');
      expect(buildGradleContent).toContain('namespace = "com.demo.app"');
      expect(buildGradleContent).toContain('applicationId = "com.demo.app"');

      // Each source root is moved AND its kt files have their `package` rewritten.
      for (const root of ['main', 'test', 'androidTest']) {
        const newSrc = path.join(appDir, 'src', root, 'java', 'com', 'demo', 'app');
        expect(fs.existsSync(newSrc)).toBe(true);
        const files = fs.readdirSync(newSrc);
        expect(files.length).toBe(1);
        expect(fs.readFileSync(path.join(newSrc, files[0]!), 'utf8')).toContain(
          'package com.demo.app',
        );
      }

      // Old paths and their now-empty parent dirs are cleaned up.
      expect(fs.existsSync(mainSrcDir)).toBe(false);
      expect(fs.existsSync(testSrcDir)).toBe(false);
      expect(fs.existsSync(androidTestSrcDir)).toBe(false);
      expect(
        fs.existsSync(path.join(appDir, 'src', 'main', 'java', 'com', 'example')),
      ).toBe(false);
      expect(
        fs.existsSync(path.join(appDir, 'src', 'test', 'java', 'com', 'example')),
      ).toBe(false);
    });

    test('rewrites the main and test ios bundle identifiers in pbxproj when present', () => {
      const projDir = path.join(work, 'ios', 'SparklingGo.xcodeproj');
      fs.mkdirSync(projDir, { recursive: true });
      const pbxproj = path.join(projDir, 'project.pbxproj');
      fs.writeFileSync(
        pbxproj,
        [
          `PRODUCT_BUNDLE_IDENTIFIER = ${DEFAULT_IOS_BUNDLE_ID};`,
          `PRODUCT_BUNDLE_IDENTIFIER = ${DEFAULT_IOS_TEST_BUNDLE_ID};`,
          `PRODUCT_BUNDLE_IDENTIFIER = ${DEFAULT_IOS_UITEST_BUNDLE_ID};`,
          `PRODUCT_BUNDLE_IDENTIFIER = ${DEFAULT_IOS_METHOD_TEST_BUNDLE_ID};`,
        ].join('\n'),
      );

      applyPackageNamespace(work, 'com.demo.app');

      const written = fs.readFileSync(pbxproj, 'utf8');
      expect(written).toContain('PRODUCT_BUNDLE_IDENTIFIER = com.demo.app;');
      expect(written).toContain('PRODUCT_BUNDLE_IDENTIFIER = com.demo.app.tests;');
      expect(written).toContain('PRODUCT_BUNDLE_IDENTIFIER = com.demo.app.uitests;');
      expect(written).toContain('PRODUCT_BUNDLE_IDENTIFIER = com.demo.app.methodtests;');
      // No mangled stragglers from the prefix-collision bug.
      expect(written).not.toContain('com.demo.appTests');
      expect(written).not.toContain('com.demo.appUITests');
      // Old bundle ids fully replaced.
      expect(written).not.toContain(DEFAULT_IOS_BUNDLE_ID);
      expect(written).not.toContain(DEFAULT_IOS_TEST_BUNDLE_ID);
      expect(written).not.toContain(DEFAULT_IOS_UITEST_BUNDLE_ID);
      expect(written).not.toContain(DEFAULT_IOS_METHOD_TEST_BUNDLE_ID);
    });
  });
});
