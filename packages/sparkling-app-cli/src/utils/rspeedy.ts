// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';

const packageRequire = createRequire(__filename);

export interface RspeedyCommand {
  command: string;
  args: string[];
}

export function resolveRspeedyCommand(
  manifestPath = packageRequire.resolve('@lynx-js/rspeedy/package.json'),
): RspeedyCommand {
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8')) as {
    bin?: Record<string, unknown>;
  };
  const binPath = manifest.bin?.rspeedy;

  if (typeof binPath !== 'string' || binPath.trim() === '') {
    throw new Error(`Invalid @lynx-js/rspeedy package metadata at ${manifestPath}: missing bin.rspeedy`);
  }

  return {
    command: process.execPath,
    args: [path.resolve(path.dirname(manifestPath), binPath)],
  };
}
