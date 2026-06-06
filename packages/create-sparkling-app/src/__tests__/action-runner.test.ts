// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { ActionRunner, executeAndLogAction } from '../core/actions/action-runner';
import type { ActionContext } from '../core/actions/action';

describe('ActionRunner', () => {
  function context(): ActionContext {
    return {
      devMode: false,
      environment: 'development',
      logger: {
        clear: jest.fn(),
        error: jest.fn(),
        info: jest.fn(),
        logFile: null,
        message: jest.fn(),
        warn: jest.fn(),
      },
      projectRoot: '/tmp/project',
    };
  }

  it('runs actions in order and passes previous result', async () => {
    const calls: string[] = [];
    const runner = new ActionRunner(context());

    runner.addAction({
      name: 'first',
      async execute() {
        calls.push('first');
        return { result: 'one', outputPaths: ['/tmp/one'] };
      },
    });
    runner.addAction({
      name: 'second',
      async execute(_context, previous) {
        calls.push(`second:${previous?.result}`);
        return { crucialOutputPaths: ['/tmp/two'], result: 'two' };
      },
    });

    await runner.run();

    expect(calls).toEqual(['first', 'second:one']);
  });

  it('stops and rethrows when an action fails', async () => {
    const runner = new ActionRunner({
      ...context(),
      logger: {
        ...context().logger,
        logFile: '/tmp/create-app.log',
      },
    });
    runner.addAction({
      name: 'broken',
      async execute() {
        throw new Error('boom');
      },
    });

    await expect(runner.run()).rejects.toThrow('boom');
  });

  it('executes and logs a single action', async () => {
    const ctx = context();
    const result = await executeAndLogAction(
      {
        name: 'copy',
        async execute(_context, previous) {
          return {
            outputPaths: [`${previous?.result ?? 'none'}/out`],
            result: 'done',
          };
        },
      },
      ctx,
      { result: 'previous' },
    );

    expect(result.result).toBe('done');
    expect(ctx.logger.info).toHaveBeenCalledWith(expect.stringContaining('Starting action'));
    expect(ctx.logger.info).toHaveBeenCalledWith('  - previous/out');
  });
});
