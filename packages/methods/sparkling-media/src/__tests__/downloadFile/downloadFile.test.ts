/// <reference types="jest" />
// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { downloadFile } from '../../downloadFile/downloadFile';
import type { DownloadFileRequest, DownloadFileResponse } from '../../downloadFile/downloadFile.d';
import { createMockPipe, TEST_CONSTANTS, MockPipe } from '../test-utils';

// Mock the pipe module
jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('downloadFile', () => {
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
      const callback = jest.fn((result: DownloadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      downloadFile(null as any, callback);
    });

    it('should handle undefined params', (done: jest.DoneCallback) => {
      const callback = jest.fn((result: DownloadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      downloadFile(undefined as any, callback);
    });

    it('should handle missing url', (done: jest.DoneCallback) => {
      const params = { url: '', extension: 'jpg' } as DownloadFileRequest;
      const callback = jest.fn((result: DownloadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: url must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      downloadFile(params, callback);
    });

    it('should handle missing extension', (done: jest.DoneCallback) => {
      const params = { url: TEST_CONSTANTS.VALID_URL, extension: '' } as DownloadFileRequest;
      const callback = jest.fn((result: DownloadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: extension must be a non-empty string');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      downloadFile(params, callback);
    });

    it('should handle non-function callback', () => {
      const params: DownloadFileRequest = { url: TEST_CONSTANTS.VALID_URL, extension: 'jpg' };

      downloadFile(params, null as any);

      expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-media] downloadFile: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('successful operations', () => {
    it('should call pipe with correct parameters', () => {
      const params: DownloadFileRequest = { 
        url: TEST_CONSTANTS.VALID_URL,
        extension: 'jpg',
      };
      const callback = jest.fn();

      downloadFile(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.downloadFile',
        expect.objectContaining({
          url: TEST_CONSTANTS.VALID_URL,
          extension: 'jpg',
          needCommonParams: true,
        }),
        expect.any(Function)
      );
    });

    it('should pass optional parameters', () => {
      const params: DownloadFileRequest = {
        url: TEST_CONSTANTS.VALID_URL,
        extension: 'mp4',
        saveToAlbum: 'video',
        needCommonParams: false,
        timeoutInterval: 60,
        params: { quality: 'high' },
        header: { 'Accept': 'video/mp4' },
      };
      const callback = jest.fn();

      downloadFile(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.downloadFile',
        expect.objectContaining({
          saveToAlbum: 'video',
          needCommonParams: false,
          timeoutInterval: 60,
          params: { quality: 'high' },
          header: { 'Accept': 'video/mp4' },
        }),
        expect.any(Function)
      );
    });
  });

  describe('response handling', () => {
    it('should normalize successful response', () => {
      const params: DownloadFileRequest = { url: TEST_CONSTANTS.VALID_URL, extension: 'jpg' };
      const callback = jest.fn();

      downloadFile(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      const mockResponse = {
        code: 1, // Pipe uses code 1 for success
        msg: 'Success',
        data: {
          httpCode: 200,
          filePath: '/tmp/downloaded.jpg',
        }
      };
      
      (passedCallback as Function)(mockResponse);
      
      expect(callback).toHaveBeenCalledWith({
        code: 1,
        msg: 'Success',
        data: mockResponse.data,
      });
    });

    it('should handle null response', () => {
      const params: DownloadFileRequest = { url: TEST_CONSTANTS.VALID_URL, extension: 'jpg' };
      const callback = jest.fn();

      downloadFile(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)(null);
      
      expect(callback).toHaveBeenCalledWith({
        code: -1,
        msg: 'Unknown error',
        data: undefined,
      });
    });

    it('should handle pipe failure response (code 0)', () => {
      const params: DownloadFileRequest = { url: TEST_CONSTANTS.VALID_URL, extension: 'jpg' };
      const callback = jest.fn();

      downloadFile(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)({ code: 0, msg: 'Network error' });

      expect(callback).toHaveBeenCalledWith({
        code: 0,
        msg: 'Network error',
        data: undefined,
      });
    });
  });

  describe('url trimming', () => {
    it('should trim whitespace from url before passing to pipe', () => {
      const params: DownloadFileRequest = {
        url: `  ${TEST_CONSTANTS.VALID_URL}  `,
        extension: 'mp4',
      };
      const callback = jest.fn();

      downloadFile(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.downloadFile',
        expect.objectContaining({ url: TEST_CONSTANTS.VALID_URL }),
        expect.any(Function)
      );
    });

    it('should reject whitespace-only url', (done: jest.DoneCallback) => {
      const params: DownloadFileRequest = { url: '   ', extension: 'jpg' };
      const callback = jest.fn((result: DownloadFileResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toContain('url');
        done();
      });

      downloadFile(params, callback);
    });
  });
});
