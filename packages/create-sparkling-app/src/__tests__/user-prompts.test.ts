// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import * as p from '@clack/prompts';
import { askProjectName } from '../create-app/user-prompts';

jest.mock('@clack/prompts', () => ({
  text: jest.fn(),
  select: jest.fn(),
  multiselect: jest.fn(),
  confirm: jest.fn(),
  isCancel: jest.fn(() => false),
}));

describe('user prompts', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns user-provided project name from text prompt', async () => {
    (p.text as jest.Mock).mockResolvedValueOnce('custom-app');

    const result = await askProjectName({}, 'sparkling-app');

    expect(result).toBe('custom-app');
    expect(p.text).toHaveBeenCalledWith(
      expect.objectContaining({
        message: 'Project name',
        defaultValue: 'sparkling-app',
      }),
    );
  });
});
