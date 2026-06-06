// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { ui } from './ui';

const WILDCARD_HOSTS = new Set(['0.0.0.0', '::', '[::]']);

export function isWildcardDevServerHost(host?: string): boolean {
  const normalized = host?.trim().toLowerCase();
  return normalized ? WILDCARD_HOSTS.has(normalized) : false;
}

export function warnIfWildcardDevServerHost(host?: string): void {
  if (!isWildcardDevServerHost(host)) {
    return;
  }
  console.warn(ui.warn(
    `Dev server host ${host} listens on every network interface. ` +
    'Use a specific LAN IP such as 192.168.x.x, or 127.0.0.1 with adb reverse, on untrusted networks.',
  ));
}
