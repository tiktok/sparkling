// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import pipe from 'sparkling-method';
import type {
  SetBackPressInterceptRequest,
  SetBackPressInterceptResponse,
} from './backPress.d';

/** The event the container sends while a page is intercepting. */
export const BACK_PRESS_EVENT = 'onBackPress';

/**
 * Take over the hardware back button, or give it back.
 *
 * While a page is intercepting, a back press arrives as the `onBackPress` event
 * and the container does nothing else - so the page has to close itself with
 * `close()` when it decides to go back. Forgetting that is a page the user
 * cannot leave, which is why `onBackPress` below turns interception off as soon
 * as the listener goes away.
 */
export function setBackPressIntercept(
  params: SetBackPressInterceptRequest,
  callback?: (result: SetBackPressInterceptResponse) => void,
): void {
  if (callback !== undefined && typeof callback !== 'function') {
    console.error('[sparkling-navigation] setBackPressIntercept: callback must be a function');
    return;
  }

  pipe.call('router.setBackPressIntercept', { ...params }, (v: unknown) => {
    if (typeof callback === 'function') {
      const response = v as SetBackPressInterceptResponse;
      callback({
        code: response?.code ?? -1,
        msg: response?.msg ?? (response?.code === 1 ? 'ok' : 'Unknown error'),
        supported: response?.supported,
      });
    }
  });
}

type Emitter = {
  addListener: (name: string, handler: (...args: unknown[]) => void) => void;
  removeListener: (name: string, handler: (...args: unknown[]) => void) => void;
};

function globalEventEmitter(): Emitter | null {
  const runtime = globalThis as unknown as { lynx?: { getJSModule?: (name: string) => Emitter } };
  const emitter = runtime.lynx?.getJSModule?.('GlobalEventEmitter');
  return emitter && typeof emitter.addListener === 'function' ? emitter : null;
}

/**
 * Handle the hardware back button for as long as the returned function is not
 * called.
 *
 * Turning interception on and subscribing are one step on purpose: they are
 * useless apart, and doing them separately is how a page ends up intercepting
 * with nothing listening - which is a page the back button cannot leave.
 *
 * ```ts
 * useEffect(() => onBackPress(() => {
 *   if (sheetOpen) { closeSheet(); return; }
 *   close();
 * }), [sheetOpen]);
 * ```
 */
export function onBackPress(handler: () => void): () => void {
  if (typeof handler !== 'function') {
    console.error('[sparkling-navigation] onBackPress: handler must be a function');
    return () => undefined;
  }

  const emitter = globalEventEmitter();
  if (!emitter) {
    // A runtime with no event emitter - a unit test, a platform without one -
    // must not be left intercepting a button nothing is listening for.
    return () => undefined;
  }

  const listener = (): void => handler();
  emitter.addListener(BACK_PRESS_EVENT, listener);
  setBackPressIntercept({ intercept: true });

  let released = false;
  return () => {
    if (released) {
      return;
    }
    released = true;
    emitter.removeListener(BACK_PRESS_EVENT, listener);
    setBackPressIntercept({ intercept: false });
  };
}
