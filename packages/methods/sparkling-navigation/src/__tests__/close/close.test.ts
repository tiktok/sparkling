/// <reference types="jest" />
// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { close } from '../../close/close';
import type { CloseRequest, CloseResponse } from '../../close/close.d';
import { createMockPipe, createSuccessResponse, createErrorResponse, TEST_CONSTANTS, MockPipe } from '../test-utils';

jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('close', () => {
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
    it('should handle non-function callback', () => {
      close(undefined, 'not-a-function' as any);

expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-navigation] close: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });

    it('should handle null callback', () => {
      close(undefined, null as any);

expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-navigation] close: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });

    it('should allow undefined callback', () => {
      close();

      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        {},
        expect.any(Function)
      );
    });

    it('should allow undefined params', () => {
      const callback = jest.fn();

      close(undefined, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        {},
        expect.any(Function)
      );
    });
  });

  describe('successful operations', () => {
    it('should call pipe with empty params when no params provided', () => {
      const callback = jest.fn();

      close(undefined, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        {},
        expect.any(Function)
      );
    });

    it('should call pipe with provided params', () => {
      const params: CloseRequest = {
        containerID: TEST_CONSTANTS.MOCK_CONTAINER_ID,
        animated: true
      };
      const callback = jest.fn();

      close(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        params,
        expect.any(Function)
      );
    });

    it('should handle empty params object', () => {
      const params: CloseRequest = {};
      const callback = jest.fn();

      close(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        {},
        expect.any(Function)
      );
    });

    it('should handle params with containerID only', () => {
      const params: CloseRequest = { containerID: TEST_CONSTANTS.MOCK_CONTAINER_ID };
      const callback = jest.fn();

      close(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        { containerID: TEST_CONSTANTS.MOCK_CONTAINER_ID },
        expect.any(Function)
      );
    });

    it('should handle params with animated only', () => {
      const params: CloseRequest = { animated: false };
      const callback = jest.fn();

      close(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        { animated: false },
        expect.any(Function)
      );
    });

    it('should handle complete params', () => {
      const params: CloseRequest = {
        containerID: TEST_CONSTANTS.MOCK_CONTAINER_ID,
        animated: true
      };
      const callback = jest.fn();

      close(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        params,
        expect.any(Function)
      );
    });
  });

  describe('callback handling and response processing', () => {
    it('should process pipe response and call user callback', (done) => {
      const params: CloseRequest = { containerID: 'test' };
      const mockResponse = createSuccessResponse();
      const userCallback = jest.fn((result: CloseResponse) => {
        expect(result.code).toBe(1);
        expect(result.msg).toBe('Success');
        done();
      });
      // Mock pipe to simulate successful response
      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      close(params, userCallback);
    });

    it('should handle pipe response with missing fields', (done) => {
      const params: CloseRequest = { containerID: 'test' };
      const incompleteResponse = {}; // Missing code and msg
      const userCallback = jest.fn((result: CloseResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Unknown error');
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(incompleteResponse);
      });

      close(params, userCallback);
    });

    it('should handle pipe response with null code', (done) => {
      const params: CloseRequest = { containerID: 'test' };
      const responseWithNullCode = { code: null, msg: 'Test message' };
      const userCallback = jest.fn((result: CloseResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Test message');
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(responseWithNullCode);
      });

      close(params, userCallback);
    });

    it('should handle pipe response with null msg', (done) => {
      const params: CloseRequest = { containerID: 'test' };
      const responseWithNullMsg = { code: 200, msg: null };
      const userCallback = jest.fn((result: CloseResponse) => {
        expect(result.code).toBe(200);
        expect(result.msg).toBe('Unknown error');
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(responseWithNullMsg);
      });

      close(params, userCallback);
    });

    it('should not call user callback when callback is undefined', () => {
      const params: CloseRequest = { containerID: 'test' };
      const mockResponse = createSuccessResponse();

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      expect(() => close(params)).not.toThrow();
    });

    it('should handle error responses from pipe', (done) => {
      const params: CloseRequest = { containerID: 'test' };
      const errorResponse = createErrorResponse(-1, 'Pipe error');
      const userCallback = jest.fn((result: CloseResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Pipe error');
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(errorResponse);
      });

      close(params, userCallback);
    });
  });

  describe('edge cases', () => {
    it('should handle very long containerID', () => {
      const longContainerID = 'a'.repeat(1000);
      const params: CloseRequest = { containerID: longContainerID };
      const callback = jest.fn();

      close(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        { containerID: longContainerID },
        expect.any(Function)
      );
    });

    it('should handle special characters in containerID', () => {
      const specialContainerID = 'test-container!@#$%^&*()_+{}|:<>?[]\\;\'",./';
      const params: CloseRequest = { containerID: specialContainerID };
      const callback = jest.fn();

      close(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'router.close',
        { containerID: specialContainerID },
        expect.any(Function)
      );
    });

    it('should handle boolean edge cases for animated', () => {
      const paramsTrue: CloseRequest = { animated: true };
      const paramsFalse: CloseRequest = { animated: false };
      const callback = jest.fn();

      close(paramsTrue, callback);
      close(paramsFalse, callback);

      expect(mockPipe.call).toHaveBeenNthCalledWith(
        1,
        'router.close',
        { animated: true },
        expect.any(Function)
      );
      expect(mockPipe.call).toHaveBeenNthCalledWith(
        2,
        'router.close',
        { animated: false },
        expect.any(Function)
      );
    });
  });

  describe('contract testing', () => {
    it('should validate CloseResponse contract requirements', () => {
      const params: CloseRequest = { containerID: 'test' };

      // Type checking at compile time ensures this callback matches CloseResponse
      const callback = (result: CloseResponse) => {
        expect(typeof result.code).toBe('number');
        expect(typeof result.msg).toBe('string');
      };

      close(params, callback);
    });

    it('should handle valid CloseRequest with all optional fields', () => {
      const fullRequest: CloseRequest = {
        containerID: TEST_CONSTANTS.MOCK_CONTAINER_ID,
        animated: true
      };
      const callback = jest.fn();

      expect(() => close(fullRequest, callback)).not.toThrow();
      expect(mockPipe.call).toHaveBeenCalledTimes(1);
    });

    it('should work with minimal CloseRequest', () => {
      const minimalRequest: CloseRequest = {};
      const callback = jest.fn();

      expect(() => close(minimalRequest, callback)).not.toThrow();
      expect(mockPipe.call).toHaveBeenCalledTimes(1);
    });
  });

  describe('snapshot testing for response processing', () => {
    it('should return consistent processed response for valid pipe response', (done) => {
      const params: CloseRequest = { containerID: 'test' };
      const mockResponse = { code: 1, msg: 'Operation completed' };
      const userCallback = (result: CloseResponse) => {
        expect(result).toMatchSnapshot('valid-close-response');
        done();
      };

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      close(params, userCallback);
    });

    it('should return consistent processed response for incomplete pipe response', (done) => {
      const params: CloseRequest = { containerID: 'test' };
      const incompleteResponse = { code: undefined, msg: undefined };
      const userCallback = (result: CloseResponse) => {
        expect(result).toMatchSnapshot('incomplete-pipe-response');
        done();
      };

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(incompleteResponse);
      });

      close(params, userCallback);
    });
  });
});
