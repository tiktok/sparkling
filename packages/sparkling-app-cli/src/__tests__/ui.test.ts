// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { ui } from '../utils/ui';

/**
 * The ui helpers wrap chalk; in CI we may run with NO_COLOR or a non-TTY
 * stdout where chalk strips ANSI sequences. We therefore assert on substring
 * containment (the message text) rather than exact ANSI output.
 */
describe('utils/ui', () => {
  test('headline/success/warn/error/info/muted return strings containing the message', () => {
    expect(ui.headline('hi')).toEqual(expect.stringContaining('hi'));
    expect(ui.success('ok')).toEqual(expect.stringContaining('ok'));
    expect(ui.warn('warn')).toEqual(expect.stringContaining('warn'));
    expect(ui.error('boom')).toEqual(expect.stringContaining('boom'));
    expect(ui.info('info')).toEqual(expect.stringContaining('info'));
    expect(ui.muted('muted')).toEqual(expect.stringContaining('muted'));
  });

  test('tipLabel and tip include the configured TIP label', () => {
    expect(ui.tipLabel).toEqual(expect.stringContaining('Tip'));
    expect(ui.tip('try this')).toEqual(expect.stringContaining('Tip'));
    expect(ui.tip('try this')).toEqual(expect.stringContaining('try this'));
  });

  test('prompt returns string containing the message', () => {
    expect(ui.prompt('enter')).toEqual(expect.stringContaining('enter'));
  });
});
