/// <reference types="jest" />
// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { navigate } from '../../navigate/navigate';
import type { NavigateRequest, NavigateResponse } from '../../navigate/navigate.d';
import { open } from '../../open/open';

jest.mock('../../open/open', () => ({
  open: jest.fn(),
}));

describe('navigate', () => {
  let mockOpen: jest.MockedFunction<typeof open>;
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    mockOpen = jest.requireMock('../../open/open').open as jest.MockedFunction<typeof open>;
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  describe('parameter validation', () => {
    it('should handle null params', (done) => {
      const callback = (result: NavigateResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockOpen).not.toHaveBeenCalled();
        done();
      };

      navigate(null as any, callback);
    });

    it('should handle undefined params', (done) => {
      const callback = (result: NavigateResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockOpen).not.toHaveBeenCalled();
        done();
      };

      navigate(undefined as any, callback);
    });

    it('should handle empty path', (done) => {
      const params = { path: '' } as NavigateRequest;
      const callback = (result: NavigateResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: path must be a non-empty string');
        expect(mockOpen).not.toHaveBeenCalled();
        done();
      };

      navigate(params, callback);
    });

    it('should handle whitespace path', (done) => {
      const params = { path: '   ' } as NavigateRequest;
      const callback = (result: NavigateResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: path must be a non-empty string');
        expect(mockOpen).not.toHaveBeenCalled();
        done();
      };

      navigate(params, callback);
    });

    it('should reject absolute schemes', (done) => {
      const params = { path: 'hybrid://lynxview_page?bundle=main.lynx.bundle' } as NavigateRequest;
      const callback = (result: NavigateResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: path must be a relative path, not a full scheme');
        expect(mockOpen).not.toHaveBeenCalled();
        done();
      };

      navigate(params, callback);
    });

    it('should handle non-function callback', () => {
      const params: NavigateRequest = { path: 'pages/second.lynx.bundle' };

      navigate(params, null as any);

expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-navigation] navigate: callback must be a function');
      expect(mockOpen).not.toHaveBeenCalled();
    });
  });

  describe('successful navigation', () => {
    it('should navigate with default scheme when only path is provided', () => {
      const params: NavigateRequest = { path: 'main.lynx.bundle' };
      const callback = jest.fn();

      navigate(params, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        {
          scheme: 'hybrid://lynxview_page?bundle=main.lynx.bundle',
          options: undefined,
        },
        callback
      );
    });

    it('should build scheme from relative path and params', () => {
      const params: NavigateRequest = {
        path: './pages/second.lynx.bundle',
        options: {
          params: {
            title: 'Second Page',
            screen_orientation: 'portrait',
          },
          animated: true
        },
      };
      const callback = jest.fn();

      navigate(params, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        {
          scheme: 'hybrid://lynxview_page?bundle=pages%2Fsecond.lynx.bundle&title=Second%20Page&screen_orientation=portrait',
          options: { animated: true },
        },
        callback
      );
    });

    it('should honor custom base scheme', () => {
      const params: NavigateRequest = {
        path: 'main.lynx.bundle',
        baseScheme: 'hybrid://lynxview',
        options: { params: { title: 'Main' } },
      };
      const callback = jest.fn();

      navigate(params, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        {
          scheme: 'hybrid://lynxview?bundle=main.lynx.bundle&title=Main',
          options: undefined,
        },
        callback
      );
    });

    it('should omit empty params entries and still navigate', () => {
      const params: NavigateRequest = {
        path: '/main.lynx.bundle',
        options: { params: { hide_error: undefined, hide_loading: undefined } },
      };
      const callback = jest.fn();

      navigate(params, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        {
          scheme: 'hybrid://lynxview_page?bundle=main.lynx.bundle',
          options: undefined,
        },
        callback
      );
    });

    it('should not forward extra to open (host extra is open-only)', () => {
      const params = {
        path: 'main.lynx.bundle',
        options: { animated: true, extra: { foo: 'bar' } },
      } as NavigateRequest;
      const callback = jest.fn();

      navigate(params, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        {
          scheme: 'hybrid://lynxview_page?bundle=main.lynx.bundle',
          options: { animated: true },
        },
        callback
      );
    });

    it('should pass custom (non-predefined) params through to the scheme URL', () => {
      const params: NavigateRequest = {
        path: 'pages/detail.lynx.bundle',
        options: {
          params: {
            title: 'Detail',
            customParam: 'hello',
            itemId: 42,
          },
        },
      };
      const callback = jest.fn();

      navigate(params, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        {
          scheme: 'hybrid://lynxview_page?bundle=pages%2Fdetail.lynx.bundle&title=Detail&customParam=hello&itemId=42',
          options: undefined,
        },
        callback
      );
    });
  });

  describe('dev mode with globalProps', () => {
    const savedLynx = (globalThis as any).lynx;
    const savedDev = (globalThis as any).__DEV__;
    const savedPublicPath = (globalThis as any).__webpack_public_path__;

    beforeEach(() => {
      (globalThis as any).__DEV__ = true;
      (globalThis as any).__webpack_public_path__ = 'http://localhost:5969/';
    });

    afterEach(() => {
      (globalThis as any).lynx = savedLynx;
      (globalThis as any).__DEV__ = savedDev;
      (globalThis as any).__webpack_public_path__ = savedPublicPath;
    });

    it('should use base URL from globalProps.queryItems.url over __webpack_public_path__', () => {
      (globalThis as any).lynx = {
        __globalProps: {
          queryItems: { url: 'http://192.168.1.100:5969/main.lynx.bundle' },
        },
      };
      const callback = jest.fn();

      navigate({ path: 'second.lynx.bundle' }, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        expect.objectContaining({
          scheme: expect.stringContaining('url=http%3A%2F%2F192.168.1.100%3A5969%2Fsecond.lynx.bundle'),
        }),
        callback,
      );
    });

    it('should fall back to __webpack_public_path__ when globalProps has no url', () => {
      (globalThis as any).lynx = {
        __globalProps: { queryItems: {} },
      };
      const callback = jest.fn();

      navigate({ path: 'second.lynx.bundle' }, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        expect.objectContaining({
          scheme: expect.stringContaining('url=http%3A%2F%2Flocalhost%3A5969%2Fsecond.lynx.bundle'),
        }),
        callback,
      );
    });

    it('should ignore non-HTTP URLs in globalProps', () => {
      (globalThis as any).lynx = {
        __globalProps: {
          queryItems: { url: 'file:///local/main.lynx.bundle' },
        },
      };
      const callback = jest.fn();

      navigate({ path: 'second.lynx.bundle' }, callback);

      expect(mockOpen).toHaveBeenCalledWith(
        expect.objectContaining({
          scheme: expect.stringContaining('url=http%3A%2F%2Flocalhost%3A5969%2Fsecond.lynx.bundle'),
        }),
        callback,
      );
    });
  });
});
