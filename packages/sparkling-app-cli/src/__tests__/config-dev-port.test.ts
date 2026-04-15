// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
/// <reference types="jest" />
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { getConfiguredDevServerPorts, resolveDevServerPort, updateDevServerPortInAppConfig } from '../config';

describe('dev server port config helpers', () => {
  it('uses default port when app config has no port', () => {
    const port = resolveDevServerPort({ lynxConfig: {} } as never);
    expect(port).toBe(5969);
  });

  it('uses configured dev.server.port when present', () => {
    const port = resolveDevServerPort({ lynxConfig: {}, dev: { server: { port: 7001 } } } as never);
    expect(port).toBe(7001);
  });

  it('falls back to lynxConfig.server.port when dev.server.port is missing', () => {
    const port = resolveDevServerPort({ lynxConfig: { server: { port: 7003 } } } as never);
    expect(port).toBe(7003);
  });

  it('prefers dev.server.port over lynxConfig.server.port when both exist', () => {
    const port = resolveDevServerPort({ lynxConfig: { server: { port: 7003 } }, dev: { server: { port: 7004 } } } as never);
    expect(port).toBe(7004);
  });

  it('extracts both configured ports for conflict checks', () => {
    const ports = getConfiguredDevServerPorts({ lynxConfig: { server: { port: 7010 } }, dev: { server: { port: 7020 } } } as never);
    expect(ports).toEqual({ devPort: 7020, lynxPort: 7010 });
  });

  it('updates existing dev.server.port in app.config.ts', () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'spk-port-'));
    const file = path.join(dir, 'app.config.ts');
    fs.writeFileSync(file, 'const config = {\n  lynxConfig: {},\n  dev: {\n    server: {\n      port: 5969,\n    },\n  },\n}\n');

    const updated = updateDevServerPortInAppConfig(file, 7001);
    const content = fs.readFileSync(file, 'utf8');

    expect(updated).toBe(true);
    expect(content).toContain('port: 7001');
  });

  it('inserts dev.server.port when missing in app.config.ts', () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'spk-port-'));
    const file = path.join(dir, 'app.config.ts');
    fs.writeFileSync(file, 'const config = {\n  lynxConfig: {},\n  appName: "demo",\n}\n');

    const updated = updateDevServerPortInAppConfig(file, 7002);
    const content = fs.readFileSync(file, 'utf8');

    expect(updated).toBe(true);
    expect(content).toContain('dev: {');
    expect(content).toContain('server: {');
    expect(content).toContain('port: 7002');
  });
});
