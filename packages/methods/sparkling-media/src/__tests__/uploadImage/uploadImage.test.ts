/// <reference types="jest" />
// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { uploadImage } from '../../uploadImage/uploadImage';
import type { UploadImageRequest, UploadImageResponse } from '../../uploadImage/uploadImage.d';
import { createMockPipe, TEST_CONSTANTS, MockPipe } from '../test-utils';

// Mock the pipe module
jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('uploadImage', () => {
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
      const callback = jest.fn((result: UploadImageResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      uploadImage(null as any, callback);
    });

    it('should handle undefined params', (done: jest.DoneCallback) => {
      const callback = jest.fn((result: UploadImageResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      uploadImage(undefined as any, callback);
    });

    it('should handle missing url', (done: jest.DoneCallback) => {
      const params: UploadImageRequest = { url: '' };
      const callback = jest.fn((result: UploadImageResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: url must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      uploadImage(params, callback);
    });

    it('should handle whitespace-only url', (done: jest.DoneCallback) => {
      const params: UploadImageRequest = { url: TEST_CONSTANTS.INVALID_URL_WHITESPACE };
      const callback = jest.fn((result: UploadImageResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: url must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      uploadImage(params, callback);
    });

    it('should handle non-function callback', () => {
      const params: UploadImageRequest = { url: TEST_CONSTANTS.VALID_URL };

      uploadImage(params, null as any);

      expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-media] uploadImage: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('successful operations', () => {
    it('should call pipe with correct parameters', () => {
      const params: UploadImageRequest = { 
        url: TEST_CONSTANTS.VALID_URL,
        filePath: '/path/to/image.jpg',
      };
      const callback = jest.fn();

      uploadImage(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.uploadImage',
        expect.objectContaining({
          url: TEST_CONSTANTS.VALID_URL,
          filePath: '/path/to/image.jpg',
        }),
        expect.any(Function)
      );
    });

    it('should trim url whitespace', () => {
      const urlWithWhitespace = `  ${TEST_CONSTANTS.VALID_URL}  `;
      const params: UploadImageRequest = { url: urlWithWhitespace };
      const callback = jest.fn();

      uploadImage(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.uploadImage',
        expect.objectContaining({
          url: TEST_CONSTANTS.VALID_URL,
        }),
        expect.any(Function)
      );
    });

    it('should pass optional parameters', () => {
      const params: UploadImageRequest = {
        url: TEST_CONSTANTS.VALID_URL,
        filePath: '/path/to/image.png',
        params: { description: 'test image' },
        header: { 'Authorization': 'Bearer token' },
        paramsOption: 2,
      };
      const callback = jest.fn();

      uploadImage(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.uploadImage',
        expect.objectContaining({
          params: { description: 'test image' },
          header: { 'Authorization': 'Bearer token' },
          paramsOption: 2,
        }),
        expect.any(Function)
      );
    });
  });

  describe('response handling', () => {
    it('should normalize successful response', () => {
      const params: UploadImageRequest = { url: TEST_CONSTANTS.VALID_URL };
      const callback = jest.fn();

      uploadImage(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      const mockResponse = {
        code: 1, // Pipe uses code 1 for success
        msg: 'Success',
        data: {
          url: 'https://cdn.example.com/image.jpg',
          uri: '/images/image.jpg',
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
      const params: UploadImageRequest = { url: TEST_CONSTANTS.VALID_URL };
      const callback = jest.fn();

      uploadImage(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)(null);
      
      expect(callback).toHaveBeenCalledWith({
        code: -1,
        msg: 'Unknown error',
        data: undefined,
      });
    });

    it('should handle pipe failure response (code 0)', () => {
      const params: UploadImageRequest = { url: TEST_CONSTANTS.VALID_URL };
      const callback = jest.fn();

      uploadImage(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)({ code: 0, msg: 'Upload failed' });

      expect(callback).toHaveBeenCalledWith({
        code: -1,
        msg: 'Upload failed',
        data: undefined,
      });
    });
  });

  describe('formDataBody parameter', () => {
    it('should pass formDataBody to pipe', () => {
      const formDataBody = [{ key: 'image', value: '/local/photo.jpg' }];
      const params: UploadImageRequest = {
        url: TEST_CONSTANTS.VALID_URL,
        formDataBody,
      };
      const callback = jest.fn();

      uploadImage(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.uploadImage',
        expect.objectContaining({ formDataBody }),
        expect.any(Function)
      );
    });

    it('should pass undefined formDataBody when not provided', () => {
      const params: UploadImageRequest = { url: TEST_CONSTANTS.VALID_URL };
      const callback = jest.fn();

      uploadImage(params, callback);

      const [, pipeParams] = mockPipe.call.mock.calls[0];
      expect((pipeParams as any).formDataBody).toBeUndefined();
    });
  });
});
