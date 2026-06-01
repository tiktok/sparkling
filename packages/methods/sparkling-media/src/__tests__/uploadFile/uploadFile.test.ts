/// <reference types="jest" />
// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { uploadFile } from '../../uploadFile/uploadFile';
import type { UploadFileRequest, UploadFileResponse } from '../../uploadFile/uploadFile.d';
import { createMockPipe, TEST_CONSTANTS, MockPipe } from '../test-utils';

// Mock the pipe module
jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('uploadFile', () => {
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
      const callback = jest.fn((result: UploadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      uploadFile(null as any, callback);
    });

    it('should handle undefined params', (done: jest.DoneCallback) => {
      const callback = jest.fn((result: UploadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      uploadFile(undefined as any, callback);
    });

    it('should handle missing url', (done: jest.DoneCallback) => {
      const params: UploadFileRequest = { url: '' };
      const callback = jest.fn((result: UploadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: url must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      uploadFile(params, callback);
    });

    it('should handle whitespace-only url', (done: jest.DoneCallback) => {
      const params: UploadFileRequest = { url: TEST_CONSTANTS.INVALID_URL_WHITESPACE };
      const callback = jest.fn((result: UploadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: url must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      uploadFile(params, callback);
    });

    it('should handle non-function callback', () => {
      const params: UploadFileRequest = { url: TEST_CONSTANTS.VALID_URL };

      uploadFile(params, null as any);

      expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-media] uploadFile: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('successful operations', () => {
    it('should call pipe with correct parameters', () => {
      const params: UploadFileRequest = { 
        url: TEST_CONSTANTS.VALID_URL,
        filePath: TEST_CONSTANTS.VALID_FILE_PATH,
      };
      const callback = jest.fn();

      uploadFile(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.uploadFile',
        expect.objectContaining({
          url: TEST_CONSTANTS.VALID_URL,
          filePath: TEST_CONSTANTS.VALID_FILE_PATH,
        }),
        expect.any(Function)
      );
    });

    it('should trim url whitespace', () => {
      const urlWithWhitespace = `  ${TEST_CONSTANTS.VALID_URL}  `;
      const params: UploadFileRequest = { url: urlWithWhitespace };
      const callback = jest.fn();

      uploadFile(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.uploadFile',
        expect.objectContaining({
          url: TEST_CONSTANTS.VALID_URL,
        }),
        expect.any(Function)
      );
    });

    it('should pass optional parameters', () => {
      const params: UploadFileRequest = {
        url: TEST_CONSTANTS.VALID_URL,
        filePath: TEST_CONSTANTS.VALID_FILE_PATH,
        params: { key: 'value' },
        header: { 'Content-Type': 'multipart/form-data' },
        paramsOption: 1,
      };
      const callback = jest.fn();

      uploadFile(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.uploadFile',
        expect.objectContaining({
          params: { key: 'value' },
          header: { 'Content-Type': 'multipart/form-data' },
          paramsOption: 1,
        }),
        expect.any(Function)
      );
    });
  });

  describe('response handling', () => {
    it('should normalize successful response', () => {
      const params: UploadFileRequest = { url: TEST_CONSTANTS.VALID_URL };
      const callback = jest.fn();

      uploadFile(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      const mockResponse = {
        code: 1, // Pipe uses code 1 for success
        msg: 'Success',
        data: {
          url: 'https://cdn.example.com/file.jpg',
          uri: '/files/file.jpg',
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
      const params: UploadFileRequest = { url: TEST_CONSTANTS.VALID_URL };
      const callback = jest.fn();

      uploadFile(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)(null);
      
      expect(callback).toHaveBeenCalledWith({
        code: -1,
        msg: 'Unknown error',
        data: undefined,
      });
    });

    it('should handle pipe failure response (code 0)', () => {
      const params: UploadFileRequest = { url: TEST_CONSTANTS.VALID_URL };
      const callback = jest.fn();

      uploadFile(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)({ code: 0, msg: 'Server error' });

      expect(callback).toHaveBeenCalledWith({
        code: -1,
        msg: 'Server error',
        data: undefined,
      });
    });
  });

  describe('formDataBody parameter', () => {
    it('should pass formDataBody to pipe', () => {
      const formDataBody = [{ key: 'file', value: '/local/file.pdf' }];
      const params: UploadFileRequest = {
        url: TEST_CONSTANTS.VALID_URL,
        formDataBody,
      };
      const callback = jest.fn();

      uploadFile(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.uploadFile',
        expect.objectContaining({ formDataBody }),
        expect.any(Function)
      );
    });

    it('should pass undefined formDataBody when not provided', () => {
      const params: UploadFileRequest = { url: TEST_CONSTANTS.VALID_URL };
      const callback = jest.fn();

      uploadFile(params, callback);

      const [, pipeParams] = mockPipe.call.mock.calls[0];
      expect((pipeParams as any).formDataBody).toBeUndefined();
    });
  });
});
