// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
/// <reference types="jest" />
import fs from 'fs-extra';
import os from 'node:os';
import path from 'node:path';
import { prebuild } from '../prebuild';
import { withEntitlements, withInfoPlist } from '../prebuild/mods';

let appConfig: Record<string, unknown> = {};

jest.mock('../config', () => ({
  loadAppConfig: jest.fn(async () => ({
    config: appConfig,
    configPath: '/mock/app.config.ts',
  })),
}));

const MANIFEST = [
  '<?xml version="1.0" encoding="utf-8"?>',
  '<manifest xmlns:android="http://schemas.android.com/apk/res/android">',
  '',
  '    <application android:label="@string/app_name">',
  '        <activity',
  '            android:name="com.example.app.SplashActivity"',
  '            android:exported="true">',
  '            <intent-filter>',
  '                <action android:name="android.intent.action.MAIN" />',
  '                <category android:name="android.intent.category.LAUNCHER" />',
  '            </intent-filter>',
  '        </activity>',
  '    </application>',
  '',
  '</manifest>',
  '',
].join('\n');

const INFO_PLIST = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">',
  '<plist version="1.0">',
  '<dict>',
  '\t<key>CFBundleName</key>',
  '\t<string>Demo</string>',
  '</dict>',
  '</plist>',
  '',
].join('\n');

function project(): string {
  const cwd = fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling-prebuild-'));
  fs.mkdirpSync(path.join(cwd, 'android/app/src/main'));
  fs.writeFileSync(path.join(cwd, 'android/app/src/main/AndroidManifest.xml'), MANIFEST);
  fs.mkdirpSync(path.join(cwd, 'ios'));
  fs.writeFileSync(path.join(cwd, 'ios/Info.plist'), INFO_PLIST);
  return cwd;
}

const manifestOf = (cwd: string) =>
  fs.readFileSync(path.join(cwd, 'android/app/src/main/AndroidManifest.xml'), 'utf8');
const plistOf = (cwd: string) => fs.readFileSync(path.join(cwd, 'ios/Info.plist'), 'utf8');

describe('sparkling prebuild', () => {
  let cwd: string;
  let warn: jest.SpyInstance;
  let log: jest.SpyInstance;
  let error: jest.SpyInstance;

  beforeEach(() => {
    cwd = project();
    appConfig = { lynxConfig: {}, platform: { android: { packageName: 'com.example.app' }, ios: { bundleIdentifier: 'com.example.app' } } };
    warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    log = jest.spyOn(console, 'log').mockImplementation(() => undefined);
    error = jest.spyOn(console, 'error').mockImplementation(() => undefined);
  });

  afterEach(() => {
    fs.removeSync(cwd);
    jest.restoreAllMocks();
  });

  describe('android', () => {
    it('writes permissions into a marked region, qualifying bare names', async () => {
      appConfig.android = { permissions: ['INTERNET', 'android.permission.CAMERA'] };
      await prebuild({ cwd, platform: 'android' });

      const manifest = manifestOf(cwd);
      expect(manifest).toContain('<!-- sparkling:begin(android-permissions) -->');
      expect(manifest).toContain('<uses-permission android:name="android.permission.INTERNET" />');
      expect(manifest).toContain('<uses-permission android:name="android.permission.CAMERA" />');
    });

    it('is idempotent', async () => {
      appConfig.android = { permissions: ['INTERNET'] };
      await prebuild({ cwd, platform: 'android' });
      const once = manifestOf(cwd);
      const second = await prebuild({ cwd, platform: 'android' });
      expect(manifestOf(cwd)).toBe(once);
      expect(second.changed).toEqual([]);
    });

    it('removes what the config no longer declares', async () => {
      appConfig.android = { permissions: ['INTERNET', 'CAMERA'] };
      await prebuild({ cwd, platform: 'android' });
      appConfig.android = { permissions: ['INTERNET'] };
      await prebuild({ cwd, platform: 'android' });

      const manifest = manifestOf(cwd);
      expect(manifest).toContain('android.permission.INTERNET');
      expect(manifest).not.toContain('android.permission.CAMERA');
    });

    it('puts an intent filter inside the launcher activity', async () => {
      appConfig.android = {
        intentFilters: [
          { action: 'VIEW', categories: ['DEFAULT', 'BROWSABLE'], data: [{ scheme: 'myapp' }] },
        ],
      };
      await prebuild({ cwd, platform: 'android' });

      const manifest = manifestOf(cwd);
      const regionStart = manifest.indexOf('sparkling:begin(android-intent-filters)');
      const activityEnd = manifest.indexOf('</activity>');
      expect(regionStart).toBeGreaterThan(0);
      expect(regionStart).toBeLessThan(activityEnd);
      expect(manifest).toContain('<action android:name="android.intent.action.VIEW" />');
      expect(manifest).toContain('<category android:name="android.intent.category.BROWSABLE" />');
      expect(manifest).toContain('<data android:scheme="myapp" />');
    });

    it('warns instead of declaring a permission twice', async () => {
      const manifestPath = path.join(cwd, 'android/app/src/main/AndroidManifest.xml');
      fs.writeFileSync(
        manifestPath,
        MANIFEST.replace(
          '    <application',
          '    <uses-permission android:name="android.permission.INTERNET" />\n\n    <application',
        ),
      );
      appConfig.android = { permissions: ['INTERNET'] };
      await prebuild({ cwd, platform: 'android' });

      expect(warn).toHaveBeenCalledWith(expect.stringContaining('already declared'));
      const occurrences = manifestOf(cwd).match(/android\.permission\.INTERNET/g) ?? [];
      expect(occurrences).toHaveLength(1);
    });

    it('reports a missing activity rather than dropping the filter silently', async () => {
      appConfig.android = {
        intentFilters: [{ action: 'VIEW', activity: 'com.example.app.NoSuchActivity' }],
      };
      await prebuild({ cwd, platform: 'android' });
      expect(warn).toHaveBeenCalledWith(expect.stringContaining('NoSuchActivity'));
    });
  });

  describe('ios', () => {
    it('writes Info.plist keys, including nested values', async () => {
      appConfig.ios = {
        infoPlist: {
          NSLocalNetworkUsageDescription: 'Find printers.',
          NSBonjourServices: ['_printer._tcp'],
          UIRequiresFullScreen: true,
          NSAppTransportSecurity: { NSAllowsLocalNetworking: true },
        },
      };
      await prebuild({ cwd, platform: 'ios' });

      const plist = plistOf(cwd);
      expect(plist).toContain('<key>NSLocalNetworkUsageDescription</key>');
      expect(plist).toContain('<string>Find printers.</string>');
      expect(plist).toContain('<array>');
      expect(plist).toContain('<string>_printer._tcp</string>');
      expect(plist).toContain('<true/>');
      expect(plist).toContain('<key>NSAllowsLocalNetworking</key>');
      // The developer's own key is untouched and outside the region.
      expect(plist.indexOf('<key>CFBundleName</key>')).toBeLessThan(
        plist.indexOf('sparkling:begin(ios-info-plist)'),
      );
    });

    it('turns urlSchemes into CFBundleURLTypes', async () => {
      appConfig.ios = { urlSchemes: ['myapp'] };
      await prebuild({ cwd, platform: 'ios' });

      const plist = plistOf(cwd);
      expect(plist).toContain('<key>CFBundleURLTypes</key>');
      expect(plist).toContain('<string>myapp</string>');
      expect(plist).toContain('<string>com.example.app.myapp</string>');
    });

    it('creates an entitlements file and says what Xcode still needs', async () => {
      appConfig.ios = { entitlements: { 'com.apple.developer.networking.multicast': true } };
      await prebuild({ cwd, platform: 'ios' });

      const file = path.join(cwd, 'ios/SparklingGo/SparklingGo/SparklingGo.entitlements');
      expect(fs.existsSync(file)).toBe(true);
      expect(fs.readFileSync(file, 'utf8')).toContain('com.apple.developer.networking.multicast');
      expect(warn).toHaveBeenCalledWith(expect.stringContaining('CODE_SIGN_ENTITLEMENTS'));
    });

    it('escapes text that would otherwise break the document', async () => {
      appConfig.ios = { infoPlist: { NSCameraUsageDescription: 'Scan <QR> & go' } };
      await prebuild({ cwd, platform: 'ios' });
      expect(plistOf(cwd)).toContain('<string>Scan &lt;QR&gt; &amp; go</string>');
    });
  });

  describe('check and revert', () => {
    it('--check writes nothing and reports what would change', async () => {
      appConfig.android = { permissions: ['INTERNET'] };
      const before = manifestOf(cwd);
      const result = await prebuild({ cwd, platform: 'android', check: true });

      expect(manifestOf(cwd)).toBe(before);
      expect(result.changed).toEqual(['android/app/src/main/AndroidManifest.xml']);
      expect(error).toHaveBeenCalledWith(expect.stringContaining('would change'));
    });

    it('--check passes once prebuild has run', async () => {
      appConfig.android = { permissions: ['INTERNET'] };
      await prebuild({ cwd, platform: 'android' });
      const result = await prebuild({ cwd, platform: 'android', check: true });
      expect(result.changed).toEqual([]);
    });

    it('--revert restores the file the developer wrote', async () => {
      appConfig.android = { permissions: ['INTERNET'] };
      appConfig.ios = { infoPlist: { UIRequiresFullScreen: true } };
      await prebuild({ cwd, platform: 'all' });
      expect(manifestOf(cwd)).not.toBe(MANIFEST);

      await prebuild({ cwd, platform: 'all', revert: true });
      expect(manifestOf(cwd)).toBe(MANIFEST);
      expect(plistOf(cwd)).toBe(INFO_PLIST);
    });
  });

  describe('config plugins', () => {
    it('applies a plugin and composes it with the declarative values', async () => {
      appConfig.ios = { infoPlist: { CFBundleDisplayName: 'Demo' } };
      appConfig.plugins = [
        (config) => withInfoPlist(config, (values) => ({ ...values, UIRequiresFullScreen: true })),
      ];
      await prebuild({ cwd, platform: 'ios' });

      const plist = plistOf(cwd);
      expect(plist).toContain('<key>CFBundleDisplayName</key>');
      expect(plist).toContain('<key>UIRequiresFullScreen</key>');
    });

    it('lets two plugins touch the same file without either losing', async () => {
      appConfig.plugins = [
        (config) => withInfoPlist(config, (values) => ({ ...values, A: 'one' })),
        (config) => withInfoPlist(config, (values) => ({ ...values, B: 'two' })),
      ];
      await prebuild({ cwd, platform: 'ios' });

      const plist = plistOf(cwd);
      expect(plist).toContain('<key>A</key>');
      expect(plist).toContain('<key>B</key>');
    });

    it('runs entitlement plugins even with no declarative entitlements', async () => {
      appConfig.plugins = [
        (config) => withEntitlements(config, (values) => ({ ...values, 'keychain-access-groups': ['$(AppIdentifierPrefix)com.example.app'] })),
      ];
      await prebuild({ cwd, platform: 'ios' });

      const file = path.join(cwd, 'ios/SparklingGo/SparklingGo/SparklingGo.entitlements');
      expect(fs.readFileSync(file, 'utf8')).toContain('keychain-access-groups');
    });

    it('names a plugin it cannot load', async () => {
      appConfig.plugins = ['sparkling-plugin-does-not-exist'];
      await expect(prebuild({ cwd, platform: 'ios' })).rejects.toThrow(
        /sparkling-plugin-does-not-exist/,
      );
    });
  });
});

describe('app icon generation', () => {
  const { decodePng, encodePng, resize } = require('../prebuild/png');

  /** A solid square with a transparent corner, so alpha handling is observable. */
  function master(size: number): Buffer {
    const data = Buffer.alloc(size * size * 4);
    for (let y = 0; y < size; y += 1) {
      for (let x = 0; x < size; x += 1) {
        const index = (y * size + x) * 4;
        const transparent = x < size / 4 && y < size / 4;
        data[index] = 0x20;
        data[index + 1] = 0x40;
        data[index + 2] = 0xc0;
        data[index + 3] = transparent ? 0 : 255;
      }
    }
    return encodePng({ width: size, height: size, data });
  }

  let cwd: string;

  beforeEach(() => {
    cwd = project();
    fs.mkdirpSync(path.join(cwd, 'android/app/src/main/res'));
    fs.mkdirpSync(path.join(cwd, 'ios/SparklingGo/SparklingGo/Assets.xcassets'));
    fs.mkdirpSync(path.join(cwd, 'resource'));
    fs.writeFileSync(path.join(cwd, 'resource/icon.png'), master(256));
    appConfig = {
      lynxConfig: {},
      appIcon: './resource/icon.png',
      platform: { android: { packageName: 'com.example.app' }, ios: { bundleIdentifier: 'com.example.app' } },
    };
    jest.spyOn(console, 'warn').mockImplementation(() => undefined);
    jest.spyOn(console, 'log').mockImplementation(() => undefined);
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
  });

  afterEach(() => {
    fs.removeSync(cwd);
    jest.restoreAllMocks();
  });

  it('writes every Android density at the right size', async () => {
    await prebuild({ cwd, platform: 'android' });
    for (const [density, size] of [['mdpi', 48], ['hdpi', 72], ['xhdpi', 96], ['xxhdpi', 144], ['xxxhdpi', 192]] as const) {
      const file = path.join(cwd, `android/app/src/main/res/mipmap-${density}/ic_launcher.png`);
      expect(fs.existsSync(file)).toBe(true);
      const decoded = decodePng(fs.readFileSync(file));
      expect([decoded.width, decoded.height]).toEqual([size, size]);
    }
  });

  it('writes a round variant that is transparent in the corners', async () => {
    await prebuild({ cwd, platform: 'android' });
    const round = decodePng(
      fs.readFileSync(path.join(cwd, 'android/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png')),
    );
    const corner = (round.width * 0 + 0) * 4;
    const centre = (Math.floor(round.height / 2) * round.width + Math.floor(round.width / 2)) * 4;
    expect(round.data[corner + 3]).toBe(0);
    expect(round.data[centre + 3]).toBe(255);
  });

  it('writes an opaque 1024 iOS icon and a Contents.json that names it', async () => {
    await prebuild({ cwd, platform: 'ios' });
    const iconset = path.join(cwd, 'ios/SparklingGo/SparklingGo/Assets.xcassets/AppIcon.appiconset');
    const icon = decodePng(fs.readFileSync(path.join(iconset, 'AppIcon-1024.png')));

    expect([icon.width, icon.height]).toEqual([1024, 1024]);
    // The App Store rejects alpha, so the transparent corner is composited.
    for (let i = 3; i < icon.data.length; i += 4) {
      expect(icon.data[i]).toBe(255);
    }
    const contents = JSON.parse(fs.readFileSync(path.join(iconset, 'Contents.json'), 'utf8'));
    expect(contents.images[0].filename).toBe('AppIcon-1024.png');
  });

  it('removes a webp that would otherwise win over the generated png', async () => {
    const stale = path.join(cwd, 'android/app/src/main/res/mipmap-hdpi/ic_launcher.webp');
    fs.mkdirpSync(path.dirname(stale));
    fs.writeFileSync(stale, 'not really a webp');
    await prebuild({ cwd, platform: 'android' });
    expect(fs.existsSync(stale)).toBe(false);
  });

  it('is idempotent', async () => {
    await prebuild({ cwd, platform: 'all' });
    const second = await prebuild({ cwd, platform: 'all' });
    expect(second.changed).toEqual([]);
  });

  it('--check writes no icon', async () => {
    const result = await prebuild({ cwd, platform: 'android', check: true });
    expect(result.changed.length).toBeGreaterThan(0);
    expect(fs.existsSync(path.join(cwd, 'android/app/src/main/res/mipmap-mdpi/ic_launcher.png'))).toBe(false);
  });

  it('names the file when the icon is not a PNG', async () => {
    fs.writeFileSync(path.join(cwd, 'resource/icon.png'), Buffer.from([0xef, 0xbf, 0xbd, 0x50, 0x4e, 0x47]));
    const result = await prebuild({ cwd, platform: 'android' });
    expect(result.warnings.join(' ')).toContain('./resource/icon.png');
    expect(result.warnings.join(' ')).toContain('Not a PNG');
  });

  it('decodes what it encodes, at every colour type an icon uses', () => {
    const size = 8;
    const data = Buffer.alloc(size * size * 4);
    for (let i = 0; i < size * size; i += 1) {
      data[i * 4] = i * 3;
      data[i * 4 + 1] = 255 - i;
      data[i * 4 + 2] = 128;
      data[i * 4 + 3] = i % 2 ? 255 : 64;
    }
    const round = decodePng(encodePng({ width: size, height: size, data }));
    expect(round.data.equals(data)).toBe(true);
  });

  it('averages when it downscales instead of dropping samples', () => {
    // Two-pixel checkerboard: a box filter halves it to the mean, nearest
    // neighbour would keep one of the two extremes.
    const data = Buffer.from([
      0, 0, 0, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 0, 0, 0, 255,
    ]);
    const scaled = resize({ width: 2, height: 2, data }, 1, 1);
    expect(scaled.data[0]).toBeGreaterThan(100);
    expect(scaled.data[0]).toBeLessThan(155);
  });
});
