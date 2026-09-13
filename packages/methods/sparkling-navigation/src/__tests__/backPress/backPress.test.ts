/// <reference types="jest" />
// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { BACK_PRESS_EVENT, onBackPress, setBackPressIntercept } from '../../backPress/backPress';
import { createMockPipe, MockPipe } from '../test-utils';

jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

interface FakeEmitter {
  addListener: jest.Mock;
  removeListener: jest.Mock;
  listeners: Map<string, ((...args: unknown[]) => void)[]>;
  emit(name: string): void;
}

function installEmitter(): FakeEmitter {
  const listeners = new Map<string, ((...args: unknown[]) => void)[]>();
  const emitter: FakeEmitter = {
    listeners,
    addListener: jest.fn((name: string, handler: (...args: unknown[]) => void) => {
      listeners.set(name, [...(listeners.get(name) ?? []), handler]);
    }),
    removeListener: jest.fn((name: string, handler: (...args: unknown[]) => void) => {
      listeners.set(name, (listeners.get(name) ?? []).filter((entry) => entry !== handler));
    }),
    emit(name: string) {
      for (const handler of listeners.get(name) ?? []) {
        handler();
      }
    },
  };
  (globalThis as Record<string, unknown>).lynx = { getJSModule: () => emitter };
  return emitter;
}

describe('back press', () => {
  let mockPipe: ReturnType<typeof createMockPipe>;
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    mockPipe = jest.requireMock('sparkling-method') as unknown as MockPipe;
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    delete (globalThis as Record<string, unknown>).lynx;
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
    delete (globalThis as Record<string, unknown>).lynx;
  });

  describe('setBackPressIntercept', () => {
    it('calls the method with the flag', () => {
      setBackPressIntercept({ intercept: true });
      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.setBackPressIntercept',
        { intercept: true },
        expect.any(Function),
      );
    });

    it('passes `supported` back, so a page can tell iOS from a host that declined', () => {
      const callback = jest.fn();
      setBackPressIntercept({ intercept: true }, callback);
      const handler = mockPipe.call.mock.calls[0][2] as (v: unknown) => void;
      handler({ code: 1, msg: 'ok', supported: false });

      expect(callback).toHaveBeenCalledWith({ code: 1, msg: 'ok', supported: false });
    });

    it('rejects a callback that is not a function', () => {
      setBackPressIntercept({ intercept: true }, 'nope' as never);
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        '[sparkling-navigation] setBackPressIntercept: callback must be a function',
      );
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('onBackPress', () => {
    it('subscribes and turns interception on in one step', () => {
      const emitter = installEmitter();
      onBackPress(() => undefined);

      expect(emitter.addListener).toHaveBeenCalledWith(BACK_PRESS_EVENT, expect.any(Function));
      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.setBackPressIntercept',
        { intercept: true },
        expect.any(Function),
      );
    });

    it('runs the handler when the container dispatches', () => {
      const emitter = installEmitter();
      const handler = jest.fn();
      onBackPress(handler);

      emitter.emit(BACK_PRESS_EVENT);
      expect(handler).toHaveBeenCalledTimes(1);
    });

    it('unsubscribes and turns interception off', () => {
      const emitter = installEmitter();
      const handler = jest.fn();
      const release = onBackPress(handler);

      release();
      emitter.emit(BACK_PRESS_EVENT);

      expect(handler).not.toHaveBeenCalled();
      expect(mockPipe.call).toHaveBeenLastCalledWith(
        'router.setBackPressIntercept',
        { intercept: false },
        expect.any(Function),
      );
    });

    it('releases only once, however many times it is called', () => {
      installEmitter();
      const release = onBackPress(() => undefined);
      release();
      const afterFirst = mockPipe.call.mock.calls.length;
      release();
      expect(mockPipe.call.mock.calls.length).toBe(afterFirst);
    });

    it('intercepts nothing when there is no event emitter to listen on', () => {
      // Leaving the container intercepting with no listener is a page the back
      // button cannot leave.
      const release = onBackPress(() => undefined);
      expect(mockPipe.call).not.toHaveBeenCalled();
      expect(() => release()).not.toThrow();
    });

    it('rejects a handler that is not a function', () => {
      installEmitter();
      const release = onBackPress('nope' as never);
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        '[sparkling-navigation] onBackPress: handler must be a function',
      );
      expect(mockPipe.call).not.toHaveBeenCalled();
      expect(() => release()).not.toThrow();
    });
  });
});
