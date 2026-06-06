/// <reference types="jest" />
// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { saveDataURL } from '../../saveDataURL/saveDataURL';
import type { SaveDataURLRequest, SaveDataURLResponse } from '../../saveDataURL/saveDataURL.d';
import { createMockPipe, TEST_CONSTANTS, MockPipe } from '../test-utils';

// Mock the pipe module
jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('saveDataURL', () => {
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
      const callback = jest.fn((result: SaveDataURLResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      saveDataURL(null as any, callback);
    });

    it('should handle undefined params', (done: jest.DoneCallback) => {
      const callback = jest.fn((result: SaveDataURLResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      saveDataURL(undefined as any, callback);
    });

    it('should handle missing dataURL', (done: jest.DoneCallback) => {
      const params = { 
        dataURL: '', 
        filename: TEST_CONSTANTS.VALID_FILENAME, 
        extension: TEST_CONSTANTS.VALID_EXTENSION 
      } as SaveDataURLRequest;
      const callback = jest.fn((result: SaveDataURLResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: dataURL must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      saveDataURL(params, callback);
    });

    it('should handle missing filename', (done: jest.DoneCallback) => {
      const params = { 
        dataURL: TEST_CONSTANTS.VALID_DATA_URL, 
        filename: '', 
        extension: TEST_CONSTANTS.VALID_EXTENSION 
      } as SaveDataURLRequest;
      const callback = jest.fn((result: SaveDataURLResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: filename must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      saveDataURL(params, callback);
    });

    it('should handle missing extension', (done: jest.DoneCallback) => {
      const params = { 
        dataURL: TEST_CONSTANTS.VALID_DATA_URL, 
        filename: TEST_CONSTANTS.VALID_FILENAME, 
        extension: '' 
      } as SaveDataURLRequest;
      const callback = jest.fn((result: SaveDataURLResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: extension must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      saveDataURL(params, callback);
    });

    it('should handle non-function callback', () => {
      const params: SaveDataURLRequest = { 
        dataURL: TEST_CONSTANTS.VALID_DATA_URL, 
        filename: TEST_CONSTANTS.VALID_FILENAME, 
        extension: TEST_CONSTANTS.VALID_EXTENSION 
      };

      saveDataURL(params, null as any);

      expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-media] saveDataURL: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('successful operations', () => {
    it('should call pipe with correct parameters', () => {
      const params: SaveDataURLRequest = { 
        dataURL: TEST_CONSTANTS.VALID_DATA_URL,
        filename: TEST_CONSTANTS.VALID_FILENAME,
        extension: TEST_CONSTANTS.VALID_EXTENSION,
      };
      const callback = jest.fn();

      saveDataURL(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.saveDataURL',
        expect.objectContaining({
          dataURL: TEST_CONSTANTS.VALID_DATA_URL,
          filename: TEST_CONSTANTS.VALID_FILENAME,
          extension: TEST_CONSTANTS.VALID_EXTENSION,
        }),
        expect.any(Function)
      );
    });

    it('should pass saveToAlbum parameter', () => {
      const params: SaveDataURLRequest = {
        dataURL: TEST_CONSTANTS.VALID_DATA_URL,
        filename: TEST_CONSTANTS.VALID_FILENAME,
        extension: TEST_CONSTANTS.VALID_EXTENSION,
        saveToAlbum: 'image',
      };
      const callback = jest.fn();

      saveDataURL(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.saveDataURL',
        expect.objectContaining({
          saveToAlbum: 'image',
        }),
        expect.any(Function)
      );
    });
  });

  describe('response handling', () => {
    it('should normalize successful response', () => {
      const params: SaveDataURLRequest = { 
        dataURL: TEST_CONSTANTS.VALID_DATA_URL,
        filename: TEST_CONSTANTS.VALID_FILENAME,
        extension: TEST_CONSTANTS.VALID_EXTENSION,
      };
      const callback = jest.fn();

      saveDataURL(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      const mockResponse = {
        code: 1, // Pipe uses code 1 for success
        msg: 'Success',
        data: {
          filePath: '/tmp/test_file.png',
        }
      };
      
      (passedCallback as Function)(mockResponse);
      
      expect(callback).toHaveBeenCalledWith({
        code: 0, // mapPipeResponse converts code 1 to 0 for business layer
        msg: 'Success',
        data: mockResponse.data,
      });
    });

    it('should handle null response', () => {
      const params: SaveDataURLRequest = { 
        dataURL: TEST_CONSTANTS.VALID_DATA_URL,
        filename: TEST_CONSTANTS.VALID_FILENAME,
        extension: TEST_CONSTANTS.VALID_EXTENSION,
      };
      const callback = jest.fn();

      saveDataURL(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)(null);
      
      expect(callback).toHaveBeenCalledWith({
        code: -1,
        msg: 'Unknown error',
        data: undefined,
      });
    });

    it('should handle pipe failure response (code 0)', () => {
      const params: SaveDataURLRequest = {
        dataURL: TEST_CONSTANTS.VALID_DATA_URL,
        filename: TEST_CONSTANTS.VALID_FILENAME,
        extension: TEST_CONSTANTS.VALID_EXTENSION,
      };
      const callback = jest.fn();

      saveDataURL(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)({ code: 0, msg: 'Write failed' });

      expect(callback).toHaveBeenCalledWith({
        code: -1,
        msg: 'Write failed',
        data: undefined,
      });
    });
  });

  describe('field trimming', () => {
    it('should trim whitespace from dataURL', () => {
      const params: SaveDataURLRequest = {
        dataURL: `  ${TEST_CONSTANTS.VALID_DATA_URL}  `,
        filename: TEST_CONSTANTS.VALID_FILENAME,
        extension: TEST_CONSTANTS.VALID_EXTENSION,
      };
      const callback = jest.fn();

      saveDataURL(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.saveDataURL',
        expect.objectContaining({ dataURL: TEST_CONSTANTS.VALID_DATA_URL }),
        expect.any(Function)
      );
    });

    it('should trim whitespace from filename', () => {
      const params: SaveDataURLRequest = {
        dataURL: TEST_CONSTANTS.VALID_DATA_URL,
        filename: `  ${TEST_CONSTANTS.VALID_FILENAME}  `,
        extension: TEST_CONSTANTS.VALID_EXTENSION,
      };
      const callback = jest.fn();

      saveDataURL(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.saveDataURL',
        expect.objectContaining({ filename: TEST_CONSTANTS.VALID_FILENAME }),
        expect.any(Function)
      );
    });

    it('should trim whitespace from extension', () => {
      const params: SaveDataURLRequest = {
        dataURL: TEST_CONSTANTS.VALID_DATA_URL,
        filename: TEST_CONSTANTS.VALID_FILENAME,
        extension: `  ${TEST_CONSTANTS.VALID_EXTENSION}  `,
      };
      const callback = jest.fn();

      saveDataURL(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.saveDataURL',
        expect.objectContaining({ extension: TEST_CONSTANTS.VALID_EXTENSION }),
        expect.any(Function)
      );
    });

    it('should reject whitespace-only dataURL', (done: jest.DoneCallback) => {
      const params: SaveDataURLRequest = {
        dataURL: '   ',
        filename: TEST_CONSTANTS.VALID_FILENAME,
        extension: TEST_CONSTANTS.VALID_EXTENSION,
      };
      const callback = jest.fn((result: SaveDataURLResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toContain('dataURL');
        done();
      });

      saveDataURL(params, callback);
    });
  });
});
