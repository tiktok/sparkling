/// <reference types="jest" />
// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { open } from '../../open/open';
import type { OpenRequest, OpenResponse, OpenOptions } from '../../open/open.d';
import { createMockPipe, createSuccessResponse, createErrorResponse, TEST_CONSTANTS, MockPipe } from '../test-utils';

// Mock the pipe modul
jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('open', () => {
  let mockPipe: ReturnType<typeof createMockPipe>;
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    mockPipe = jest.requireMock('sparkling-method') as unknown as MockPipe;
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  describe('parameter validation', () => {
    it('should handle null params', (done: jest.DoneCallback) => {
      const callback = jest.fn((result: OpenResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      open(null as any, callback);
    });

    it('should handle undefined params', (done: jest.DoneCallback) => {
      const callback = jest.fn((result: OpenResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      open(undefined as any, callback);
    });

    it('should handle missing scheme', (done: jest.DoneCallback) => {
      const params: OpenRequest = { scheme: '' };
      const callback = jest.fn((result: OpenResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: scheme must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      open(params, callback);
    });

    it('should handle whitespace-only scheme', (done: jest.DoneCallback) => {
      const params: OpenRequest = { scheme: TEST_CONSTANTS.INVALID_SCHEME_WHITESPACE };
      const callback = jest.fn((result: OpenResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: scheme must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      open(params, callback);
    });

    it('should handle null scheme', (done: jest.DoneCallback) => {
      const params = { scheme: null } as any;
      const callback = jest.fn((result: OpenResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: scheme must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      open(params, callback);
    });

    it('should handle non-string scheme', (done: jest.DoneCallback) => {
      const params = { scheme: 123 } as any;
      const callback = jest.fn((result: OpenResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: scheme must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      open(params, callback);
    });

    it('should handle non-function callback', () => {
      const params: OpenRequest = { scheme: TEST_CONSTANTS.VALID_SCHEME };

      open(params, null as any);

expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-navigation] open: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });

    it('should handle undefined callback', () => {
      const params: OpenRequest = { scheme: TEST_CONSTANTS.VALID_SCHEME };

      open(params, undefined as any);

expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-navigation] open: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('successful operations', () => {
    it('should call pipe with correct parameters for basic scheme', () => {
      const params: OpenRequest = { scheme: TEST_CONSTANTS.VALID_SCHEME };
      const callback = jest.fn();

      open(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.open',
        { scheme: TEST_CONSTANTS.VALID_SCHEME },
        expect.any(Function)
      );
    });

    it('should trim whitespace from scheme', () => {
      const schemeWithWhitespace = `  ${TEST_CONSTANTS.VALID_SCHEME}  `;
      const params: OpenRequest = { scheme: schemeWithWhitespace };
      const callback = jest.fn();

      open(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.open',
        { scheme: TEST_CONSTANTS.VALID_SCHEME },
        expect.any(Function)
      );
    });

    it('should pass through all options', () => {
      const options: OpenOptions = {
        replace: true,
        replaceType: 'alwaysCloseAfterOpen',
        useSysBrowser: true,
        animated: false,
        interceptor: 'custom-interceptor',
        extra: { customData: 'test' }
      };
      const params: OpenRequest = {
        scheme: TEST_CONSTANTS.VALID_SCHEME,
        options
      };
      const callback = jest.fn();

      open(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.open',
        {
          scheme: TEST_CONSTANTS.VALID_SCHEME,
          replace: true,
          replaceType: 'alwaysCloseAfterOpen',
          useSysBrowser: true,
          animated: false,
          interceptor: 'custom-interceptor',
          extra: { customData: 'test' }
        },
        expect.any(Function)
      );
    });

    it('should handle empty options object', () => {
      const params: OpenRequest = {
        scheme: TEST_CONSTANTS.VALID_SCHEME,
        options: {}
      };
      const callback = jest.fn();

      open(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.open',
        { scheme: TEST_CONSTANTS.VALID_SCHEME },
        expect.any(Function)
      );
    });

    it('should handle missing options', () => {
      const params: OpenRequest = { scheme: TEST_CONSTANTS.VALID_SCHEME };
      const callback = jest.fn();

      open(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.open',
        { scheme: TEST_CONSTANTS.VALID_SCHEME },
        expect.any(Function)
      );
    });
  });

  describe('edge cases and error scenarios', () => {
    it('should handle schemes with special characters', () => {
      const specialScheme = 'custom://app/page?param=value&other=123#section';
      const params: OpenRequest = { scheme: specialScheme };
      const callback = jest.fn();

      open(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.open',
        { scheme: specialScheme },
        expect.any(Function)
      );
    });

    it('should handle complex options with nested objects', () => {
      const complexOptions: OpenOptions = {
        extra: {
          nested: {
            data: 'value',
            array: [1, 2, 3],
            boolean: true
          }
        }
      };
      const params: OpenRequest = {
        scheme: TEST_CONSTANTS.VALID_SCHEME,
        options: complexOptions
      };
      const callback = jest.fn();

      open(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.open',
        {
          scheme: TEST_CONSTANTS.VALID_SCHEME,
          extra: complexOptions.extra
        },
        expect.any(Function)
      );
    });

    it('should wrap callback for response normalization', () => {
      const params: OpenRequest = { scheme: TEST_CONSTANTS.VALID_SCHEME };
      const originalCallback = jest.fn();

      open(params, originalCallback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.open',
        { scheme: TEST_CONSTANTS.VALID_SCHEME },
        expect.any(Function)
      );

      // Implementation wraps the callback to normalize response shape.
      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      expect(passedCallback).not.toBe(originalCallback);

      (passedCallback as Function)(createSuccessResponse());
      expect(originalCallback).toHaveBeenCalledWith({ code: 1, msg: 'Success' });
    });
  });

  describe('contract testing', () => {
    it('should validate OpenResponse contract requirements', () => {
      const params: OpenRequest = { scheme: TEST_CONSTANTS.VALID_SCHEME };

      const callback = (result: OpenResponse) => {
        expect(typeof result.code).toBe('number');
        expect(typeof result.msg).toBe('string');
      };

      open(params, callback);
    });

    it('should handle valid OpenRequest with all optional fields', () => {
      const fullRequest: OpenRequest = {
        scheme: TEST_CONSTANTS.VALID_SCHEME,
        options: {
          replace: false,
          replaceType: 'onlyCloseAfterOpenSucceed',
          useSysBrowser: false,
          animated: true,
          interceptor: 'test',
          extra: { test: 'data' }
        }
      };
      const callback = jest.fn();

      expect(() => open(fullRequest, callback)).not.toThrow();
      expect(mockPipe.call).toHaveBeenCalledTimes(1);
    });
  });

  describe('snapshot testing for error responses', () => {
    it('should return consistent error response for null params', (done: jest.DoneCallback) => {
      const callback = (result: OpenResponse) => {
        expect(result).toMatchSnapshot('null-params-error');
        done();
      };

      open(null as any, callback);
    });

    it('should return consistent error response for invalid scheme', (done: jest.DoneCallback) => {
      const callback = (result: OpenResponse) => {
        expect(result).toMatchSnapshot('invalid-scheme-error');
        done();
      };

      open({ scheme: '' }, callback);
    });
  });
});
