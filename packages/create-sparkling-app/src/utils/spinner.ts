// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import * as p from '@clack/prompts';

export interface SimpleSpinner {
  message(message?: string): void;
  start(message?: string): void;
  stop(message?: string, code?: number): void;
}

export function createSpinner(): SimpleSpinner {
  // p.spinner() uses animated cursor output that requires a TTY.
  // Fall back to simple console.log in non-TTY environments (CI, piped output).
  if (!process.stdout.isTTY) {
    let activeMessage: string | null = null;
    return {
      message(message?: string) {
        if (message) console.log(message);
      },
      start(message?: string) {
        activeMessage = message ?? null;
        if (message) console.log(message);
      },
      stop(message?: string, _code?: number) {
        const finalMessage = message ?? activeMessage;
        if (finalMessage) console.log(finalMessage);
        activeMessage = null;
      },
    };
  }

  const spin = p.spinner();

  return {
    message(message?: string) {
      if (message) {
        spin.message(message);
      }
    },
    start(message?: string) {
      spin.start(message ?? '');
    },
    stop(message?: string, _code?: number) {
      spin.stop(message ?? '');
    },
  };
}
