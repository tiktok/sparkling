// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { setItem } from '../../setStorageItem/setStorageItem';
import type { SetItemRequest, SetItemResponse } from '../../setStorageItem/setStorageItem.d';
import { createMockPipe, createSuccessResponse, createErrorResponse, TEST_CONSTANTS } from '../test-utils';

jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('setItem', () => {
  let mockPipe: ReturnType<typeof createMockPipe>;
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    mockPipe = jest.requireMock('sparkling-method') as ReturnType<typeof createMockPipe>;
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  describe('parameter validation', () => {
    it('should handle null params', (done) => {
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(null as any, callback);
    });

    it('should handle undefined params', (done) => {
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(undefined as any, callback);
    });

    it('should handle missing key', (done) => {
      const params: SetItemRequest = { key: '', data: TEST_CONSTANTS.VALID_STRING_DATA };
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: key must be a non-empty string');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(params, callback);
    });

    it('should handle whitespace-only key', (done) => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.INVALID_KEY_WHITESPACE, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: key must be a non-empty string');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(params, callback);
    });

    it('should handle null key', (done) => {
      const params = { key: null, data: TEST_CONSTANTS.VALID_STRING_DATA } as any;
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: key must be a non-empty string');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(params, callback);
    });

    it('should handle non-string key', (done) => {
      const params = { key: 123, data: TEST_CONSTANTS.VALID_STRING_DATA } as any;
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: key must be a non-empty string');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(params, callback);
    });

    it('should handle undefined data', (done) => {
      const params = { key: TEST_CONSTANTS.VALID_KEY, data: undefined } as any;
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: data cannot be undefined');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(params, callback);
    });

    it('should allow null data', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: null };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: null,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle negative validDuration', (done) => {
      const params: SetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY,
        data: TEST_CONSTANTS.VALID_STRING_DATA,
        validDuration: TEST_CONSTANTS.INVALID_DURATION_NEGATIVE
      };
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: validDuration must be a non-negative number');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(params, callback);
    });

    it('should handle non-number validDuration', (done) => {
      const params: SetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY,
        data: TEST_CONSTANTS.VALID_STRING_DATA,
        validDuration: TEST_CONSTANTS.INVALID_DURATION_STRING
      };
      const callback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: validDuration must be a non-negative number');
        expect(result.data).toBeUndefined();
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      setItem(params, callback);
    });

    it('should allow zero validDuration', () => {
      const params: SetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY,
        data: TEST_CONSTANTS.VALID_STRING_DATA,
        validDuration: 0
      };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_STRING_DATA,
          biz: undefined,
          validDuration: 0
        },
        expect.any(Function)
      );
    });

    it('should handle non-function callback', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };

      setItem(params, null as any);

expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-storage] setItem: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });

    it('should handle undefined callback', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };

      setItem(params, undefined as any);

expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-storage] setItem: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('successful operations', () => {
    it('should call pipe with correct parameters for basic operation', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_STRING_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should trim whitespace from key', () => {
      const keyWithWhitespace = `  ${TEST_CONSTANTS.VALID_KEY}  `;
      const params: SetItemRequest = { key: keyWithWhitespace, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_STRING_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should pass through all optional parameters', () => {
      const params: SetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY,
        data: TEST_CONSTANTS.VALID_OBJECT_DATA,
        biz: TEST_CONSTANTS.MOCK_BIZ,
        validDuration: TEST_CONSTANTS.VALID_DURATION
      };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_OBJECT_DATA,
          biz: TEST_CONSTANTS.MOCK_BIZ,
          validDuration: TEST_CONSTANTS.VALID_DURATION
        },
        expect.any(Function)
      );
    });

    it('should handle string data', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_STRING_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle number data', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_NUMBER_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_NUMBER_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle boolean data', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_BOOLEAN_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_BOOLEAN_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle object data', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_OBJECT_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_OBJECT_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle array data', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_ARRAY_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_ARRAY_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle null data', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.NULL_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.NULL_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });
  });

  describe('pipe response processing', () => {
    it('should process successful pipe response', (done) => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const mockResponse = createSuccessResponse({ success: true });
      const userCallback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(1);
        expect(result.msg).toBe('Success');
        expect(result.data).toEqual({ success: true });
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      setItem(params, userCallback);
    });

    it('should handle pipe response with missing fields', (done) => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const incompleteResponse = {}; // Missing code, msg, and data
      const userCallback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Unknown error');
        expect(result.data).toBeUndefined();
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(incompleteResponse);
      });

      setItem(params, userCallback);
    });

    it('should handle pipe response with null code', (done) => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const responseWithNullCode = { code: null, msg: 'Set successfully', data: { success: true } };
      const userCallback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Set successfully');
        expect(result.data).toEqual({ success: true });
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(responseWithNullCode);
      });

      setItem(params, userCallback);
    });

    it('should handle pipe response with null msg', (done) => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const responseWithNullMsg = { code: 200, msg: null, data: { success: true } };
      const userCallback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(200);
        expect(result.msg).toBe('Unknown error');
        expect(result.data).toEqual({ success: true });
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(responseWithNullMsg);
      });

      setItem(params, userCallback);
    });

    it('should handle error responses from pipe', (done) => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const errorResponse = createErrorResponse(-500, 'Storage operation failed');
      const userCallback = jest.fn((result: SetItemResponse) => {
        expect(result.code).toBe(-500);
        expect(result.msg).toBe('Storage operation failed');
        expect(result.data).toBeUndefined();
        done();
      });

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(errorResponse);
      });

      setItem(params, userCallback);
    });
  });

  describe('edge cases', () => {
    it('should handle very long keys', () => {
      const longKey = 'a'.repeat(1000);
      const params: SetItemRequest = { key: longKey, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: longKey,
          data: TEST_CONSTANTS.VALID_STRING_DATA,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle Unicode characters in key and data', () => {
      const unicodeKey = '测试键值_🔑_🗄️_中文키';
      const unicodeData = '测试数据_📊_💾_中文데이터';
      const params: SetItemRequest = { key: unicodeKey, data: unicodeData };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: unicodeKey,
          data: unicodeData,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle very large data objects', () => {
      const largeData = {
        bigArray: Array(1000).fill(0).map((_, i) => ({ id: i, value: `item_${i}` })),
        bigString: 'a'.repeat(10000),
        nestedStructure: {
          level1: {
            level2: {
              level3: {
                data: Array(100).fill('nested')
              }
            }
          }
        }
      };
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: largeData };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: largeData,
          biz: undefined,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });

    it('should handle maximum validDuration value', () => {
      const maxDuration = Number.MAX_SAFE_INTEGER;
      const params: SetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY,
        data: TEST_CONSTANTS.VALID_STRING_DATA,
        validDuration: maxDuration
      };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_STRING_DATA,
          biz: undefined,
          validDuration: maxDuration
        },
        expect.any(Function)
      );
    });

    it('should handle special characters in biz parameter', () => {
      const specialBiz = 'biz-name!@#$%^&*()_+{}|:<>?[]\\;\'",./';
      const params: SetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY,
        data: TEST_CONSTANTS.VALID_STRING_DATA,
        biz: specialBiz
      };
      const callback = jest.fn();

      setItem(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'storage.setItem',
        {
          key: TEST_CONSTANTS.VALID_KEY,
          data: TEST_CONSTANTS.VALID_STRING_DATA,
          biz: specialBiz,
          validDuration: undefined
        },
        expect.any(Function)
      );
    });
  });

  describe('contract testing', () => {
    it('should validate SetItemResponse contract requirements', () => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };

      const callback = (result: SetItemResponse) => {
        // These properties must exist per the interface
        expect(typeof result.code).toBe('number');
        expect(typeof result.msg).toBe('string');
      };

      setItem(params, callback);
    });

    it('should handle valid SetItemRequest with all optional fields', () => {
      const fullRequest: SetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY,
        data: TEST_CONSTANTS.VALID_OBJECT_DATA,
        biz: TEST_CONSTANTS.MOCK_BIZ,
        validDuration: TEST_CONSTANTS.VALID_DURATION
      };
      const callback = jest.fn();

      expect(() => setItem(fullRequest, callback)).not.toThrow();
      expect(mockPipe.call).toHaveBeenCalledTimes(1);
    });

    it('should work with minimal SetItemRequest', () => {
      const minimalRequest: SetItemRequest = {
        key: TEST_CONSTANTS.VALID_KEY,
        data: TEST_CONSTANTS.VALID_STRING_DATA
      };
      const callback = jest.fn();

      expect(() => setItem(minimalRequest, callback)).not.toThrow();
      expect(mockPipe.call).toHaveBeenCalledTimes(1);
    });
  });

  describe('snapshot testing for responses', () => {
    it('should return consistent error response for null params', (done) => {
      const callback = (result: SetItemResponse) => {
        expect(result).toMatchSnapshot('null-params-error');
        done();
      };

      setItem(null as any, callback);
    });

    it('should return consistent error response for invalid key', (done) => {
      const callback = (result: SetItemResponse) => {
        expect(result).toMatchSnapshot('invalid-key-error');
        done();
      };

      setItem({ key: '', data: 'test' }, callback);
    });

    it('should return consistent error response for undefined data', (done) => {
      const callback = (result: SetItemResponse) => {
        expect(result).toMatchSnapshot('undefined-data-error');
        done();
      };

      setItem({ key: TEST_CONSTANTS.VALID_KEY, data: undefined } as any, callback);
    });

    it('should return consistent processed response for valid pipe response', (done) => {
      const params: SetItemRequest = { key: TEST_CONSTANTS.VALID_KEY, data: TEST_CONSTANTS.VALID_STRING_DATA };
      const mockResponse = { code: 1, msg: 'Item stored successfully', data: { success: true, timestamp: 1234567890 } };
      const userCallback = (result: SetItemResponse) => {
        expect(result).toMatchSnapshot('valid-set-response');
        done();
      };

      mockPipe.call.mockImplementation((method, params, callback) => {
        callback(mockResponse);
      });

      setItem(params, userCallback);
    });
  });
});
