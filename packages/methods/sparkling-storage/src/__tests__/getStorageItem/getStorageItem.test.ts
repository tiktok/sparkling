// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { getItem } from '../../getStorageItem/getStorageItem';
import type { GetItemRequest, GetItemResponse } from '../../getStorageItem/getStorageItem.d';
import { createMockPipe, createSuccessResponse, createErrorResponse, TEST_CONSTANTS, MOCK_STORAGE_RESPONSES, MockPipe } from '../test-utils';

// Mock the pipe module
jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('getItem', () => {
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
    it('should handle null params', (done) => {
      const callback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      getItem(null as any, callback);
    });

    it('should handle undefined params', (done) => {
      const callback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      getItem(undefined as any, callback);
    });

    it('should handle missing key', (done) => {
      const params: GetItemRequest = { key: '' };
      const callback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: key must be a non-empty string');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      getItem(params, callback);
    });

    it('should handle whitespace-only key', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.INVALID_KEY_WHITESPACE };
      const callback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: key must be a non-empty string');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      getItem(params, callback);
    });

    it('should handle null key', (done) => {
      const params = { key: null } as any;
      const callback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: key must be a non-empty string');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      getItem(params, callback);
    });

    it('should handle non-string key', (done) => {
      const params = { key: 123 } as any;
      const callback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: key must be a non-empty string');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      getItem(params, callback);
    });

    it('should handle non-function callback', () => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };

      getItem(params, null as any);

    expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-storage] getItem: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });

    it('should handle undefined callback', () => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };

      getItem(params, undefined as any);

    expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-storage] getItem: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('successful operations', () => {
    it('should call pipe with correct parameters for basic key', () => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const callback = jest.fn();

      getItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.getItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          biz: undefined
        },
        expect.any(Function)
      );
    });

    it('should trim whitespace from key', () => {
      const keyWithWhitespace = `  ${TEST_CONSTANTS.VALID_KEY}  `;
      const params: GetItemRequest = { key: keyWithWhitespace };
      const callback = jest.fn();

      getItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.getItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          biz: undefined
        },
        expect.any(Function)
      );
    });

    it('should pass through biz parameter when provided', () => {
      // Note: This test acknowledges that biz is used in implementation but not in type definition
      const params = {
        key: TEST_CONSTANTS.VALID_KEY,
        biz: TEST_CONSTANTS.MOCK_BIZ
      } as any;
      const callback = jest.fn();

      getItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.getItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          biz: TEST_CONSTANTS.MOCK_BIZ
        },
        expect.any(Function)
      );
    });

    it('should handle keys with special characters', () => {
      const specialKey = 'special-key_123!@#$%^&*()_+{}|:<>?[]\\;\'",./';
      const params: GetItemRequest = { key: specialKey };
      const callback = jest.fn();

      getItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.getItem',
        {
          key: specialKey,
          biz: undefined
        },
        expect.any(Function)
      );
    });
  });

  describe('pipe response processing', () => {
    it('should process successful pipe response with string data', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const mockResponse = createSuccessResponse(MOCK_STORAGE_RESPONSES.getString);
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(1);
        expect(result.msg).toBe('Success');
        expect(result.data).toEqual(MOCK_STORAGE_RESPONSES.getString);
        done();
      });

      // Mock pipe to simulate successful response
      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      getItem(params, userCallback);
    });

    it('should process successful pipe response with number data', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const mockResponse = createSuccessResponse(MOCK_STORAGE_RESPONSES.getNumber);
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(1);
        expect(result.msg).toBe('Success');
        expect(result.data).toEqual(MOCK_STORAGE_RESPONSES.getNumber);
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      getItem(params, userCallback);
    });

    it('should process successful pipe response with object data', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const mockResponse = createSuccessResponse(MOCK_STORAGE_RESPONSES.getObject);
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(1);
        expect(result.msg).toBe('Success');
        expect(result.data).toEqual(MOCK_STORAGE_RESPONSES.getObject);
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      getItem(params, userCallback);
    });

    it('should process successful pipe response with array data', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const mockResponse = createSuccessResponse(MOCK_STORAGE_RESPONSES.getArray);
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(1);
        expect(result.msg).toBe('Success');
        expect(result.data).toEqual(MOCK_STORAGE_RESPONSES.getArray);
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      getItem(params, userCallback);
    });

    it('should process successful pipe response with null data', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const mockResponse = createSuccessResponse(MOCK_STORAGE_RESPONSES.getNull);
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(1);
        expect(result.msg).toBe('Success');
        expect(result.data).toEqual(MOCK_STORAGE_RESPONSES.getNull);
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      getItem(params, userCallback);
    });

    it('should handle pipe response with missing fields', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const incompleteResponse = {}; // Missing code, msg, and data
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Unknown error');
        expect(result.data).toBeUndefined();
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(incompleteResponse);
      });

      getItem(params, userCallback);
    });

    it('should handle pipe response with null code', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const responseWithNullCode = { code: null, msg: 'Test message', data: { data: 'test' } };
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Test message');
        expect(result.data).toEqual({ data: 'test' });
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(responseWithNullCode);
      });

      getItem(params, userCallback);
    });

    it('should handle pipe response with null msg', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const responseWithNullMsg = { code: 200, msg: null, data: { data: 'test' } };
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(200);
        expect(result.msg).toBe('Unknown error');
        expect(result.data).toEqual({ data: 'test' });
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(responseWithNullMsg);
      });

      getItem(params, userCallback);
    });

    it('should handle error responses from pipe', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const errorResponse = createErrorResponse(-404, 'Item not found');
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.code).toBe(-404);
        expect(result.msg).toBe('Item not found');
        expect(result.data).toBeUndefined();
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(errorResponse);
      });

      getItem(params, userCallback);
    });
  });

  describe('edge cases', () => {
    it('should handle very long keys', () => {
      const longKey = 'a'.repeat(1000);
      const params: GetItemRequest = { key: longKey };
      const callback = jest.fn();

      getItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.getItem',
        {
          key: longKey,
          biz: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle Unicode characters in key', () => {
      const unicodeKey = '测试键值_🔑_🗄️_中文키';
      const params: GetItemRequest = { key: unicodeKey };
      const callback = jest.fn();

      getItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.getItem',
        {
          key: unicodeKey,
          biz: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle complex data structures in response', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const complexData = {
        data: {
          nested: {
            deeply: {
              nested: {
                value: [1, 2, { inner: 'value' }],
                boolean: true,
                nullValue: null,
                undefinedValue: undefined
              }
            }
          },
          array: [
            { id: 1, name: 'item1' },
            { id: 2, name: 'item2', meta: { tags: ['a', 'b'] } }
          ]
        }
      };
      const mockResponse = createSuccessResponse(complexData);
      const userCallback = jest.fn((result: GetItemResponse) => {
        expect(result.data).toEqual(complexData);
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      getItem(params, userCallback);
    });
  });

  describe('contract testing', () => {
    it('should validate GetItemResponse contract requirements', () => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };

      // Type checking at compile time ensures this callback matches GetItemResponse
      const callback = (result: GetItemResponse) => {
        // These properties must exist per the interface
        expect(typeof result.code).toBe('number');
        expect(typeof result.msg).toBe('string');
        expect(result.data).toBeDefined(); // data can be any type including undefined
      };

      // Should not throw any TypeScript compilation errors
      getItem(params, callback);
    });

    it('should handle valid GetItemRequest with required fields only', () => {
      const minimalRequest: GetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY
      };
      const callback = jest.fn();

      expect(() => getItem(minimalRequest, callback)).not.toThrow();
      expect(mockPipe.call).toHaveBeenCalledTimes(1);
    });
  });

  describe('snapshot testing for error responses', () => {
    it('should return consistent error response for null params', (done) => {
      const callback = (result: GetItemResponse) => {
        expect(result).toMatchSnapshot('null-params-error');
        done();
      };

      getItem(null as any, callback);
    });

    it('should return consistent error response for invalid key', (done) => {
      const callback = (result: GetItemResponse) => {
        expect(result).toMatchSnapshot('invalid-key-error');
        done();
      };

      getItem({ key: '' }, callback);
    });

    it('should return consistent processed response for valid pipe response', (done) => {
      const params: GetItemRequest = { key: TEST_CONSTANTS.VALID_KEY };
      const mockResponse = { code: 1, msg: 'Retrieved successfully', data: { data: 'test-value' } };
      const userCallback = (result: GetItemResponse) => {
        expect(result).toMatchSnapshot('valid-get-response');
        done();
      };

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      getItem(params, userCallback);
    });
  });
});
