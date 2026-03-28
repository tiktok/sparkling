/// <reference types="jest" />
// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { chooseMedia } from '../../chooseMedia/chooseMedia';
import type { ChooseMediaRequest, ChooseMediaResponse } from '../../chooseMedia/chooseMedia.d';
import { createMockPipe, createSuccessResponse, createErrorResponse, MockPipe } from '../test-utils';

// Mock the pipe module
jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('chooseMedia', () => {
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
      const callback = jest.fn((result: ChooseMediaResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      chooseMedia(null as any, callback);
    });

    it('should handle undefined params', (done: jest.DoneCallback) => {
      const callback = jest.fn((result: ChooseMediaResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: params cannot be null or undefined');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      chooseMedia(undefined as any, callback);
    });

    it('should handle empty mediaTypes', (done: jest.DoneCallback) => {
      const params: ChooseMediaRequest = { mediaTypes: [], sourceType: 'album' };
      const callback = jest.fn((result: ChooseMediaResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: mediaTypes must be a non-empty array');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      chooseMedia(params, callback);
    });

    it('should handle missing sourceType', (done: jest.DoneCallback) => {
      const params = { mediaTypes: ['image'] } as any;
      const callback = jest.fn((result: ChooseMediaResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: sourceType must be "album" or "camera"');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      chooseMedia(params, callback);
    });

    it('should handle invalid sourceType', (done: jest.DoneCallback) => {
      const params = { mediaTypes: ['image'], sourceType: 'invalid' } as any;
      const callback = jest.fn((result: ChooseMediaResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: sourceType must be "album" or "camera"');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      chooseMedia(params, callback);
    });

    it('should require cameraType when sourceType is camera', (done: jest.DoneCallback) => {
      const params: ChooseMediaRequest = { mediaTypes: ['image'], sourceType: 'camera' };
      const callback = jest.fn((result: ChooseMediaResponse) => {
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Invalid params: cameraType is required when sourceType is "camera"');
        expect(mockPipe.call).not.toHaveBeenCalled();
        done();
      });

      chooseMedia(params, callback);
    });

    it('should handle non-function callback', () => {
      const params: ChooseMediaRequest = { mediaTypes: ['image'], sourceType: 'album' };

      chooseMedia(params, null as any);

      expect(consoleErrorSpy).toHaveBeenCalledWith('[sparkling-media] chooseMedia: callback must be a function');
      expect(mockPipe.call).not.toHaveBeenCalled();
    });
  });

  describe('successful operations', () => {
    it('should call pipe with correct parameters for album selection', () => {
      const params: ChooseMediaRequest = { 
        mediaTypes: ['image'], 
        sourceType: 'album' 
      };
      const callback = jest.fn();

      chooseMedia(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.chooseMedia',
        expect.objectContaining({
          mediaTypes: [1],
          sourceType: 1,
          maxCount: 1,
        }),
        expect.any(Function)
      );
    });

    it('should call pipe with correct parameters for camera selection', () => {
      const params: ChooseMediaRequest = {
        mediaTypes: ['image', 'video'],
        sourceType: 'camera',
        cameraType: 'back'
      };
      const callback = jest.fn();

      chooseMedia(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.chooseMedia',
        expect.objectContaining({
          mediaTypes: [1, 2],
          sourceType: 2,
          cameraType: 2,
        }),
        expect.any(Function)
      );
    });

    it('should pass all optional parameters', () => {
      const params: ChooseMediaRequest = {
        mediaTypes: ['image'],
        sourceType: 'album',
        maxCount: 5,
        compressImage: true,
        saveToPhotoAlbum: true,
        needBase64Data: true,
        compressOption: 1,
        compressWidth: 800,
        compressHeight: 600,
        compressQuality: 80,
      };
      const callback = jest.fn();

      chooseMedia(params, callback);

      expect(mockPipe.call).toHaveBeenCalledWith(
        'media.chooseMedia',
        expect.objectContaining({
          maxCount: 5,
          compressImage: true,
          saveToPhotoAlbum: true,
          needBase64Data: true,
          compressOption: 1,
          compressWidth: 800,
          compressHeight: 600,
          compressQuality: 80,
        }),
        expect.any(Function)
      );
    });
  });

  describe('response handling', () => {
    it('should normalize successful response', () => {
      const params: ChooseMediaRequest = { mediaTypes: ['image'], sourceType: 'album' };
      const callback = jest.fn();

      chooseMedia(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      const mockResponse = {
        code: 0,
        msg: 'Success',
        data: {
          tempFiles: [{ tempFilePath: '/path/to/file.jpg', size: 1024, mediaType: 'image' }]
        }
      };
      
      (passedCallback as Function)(mockResponse);
      
      expect(callback).toHaveBeenCalledWith({
        code: 0,
        msg: 'Success',
        data: mockResponse.data,
      });
    });

    it('should handle null response', () => {
      const params: ChooseMediaRequest = { mediaTypes: ['image'], sourceType: 'album' };
      const callback = jest.fn();

      chooseMedia(params, callback);

      const [, , passedCallback] = mockPipe.call.mock.calls[0];
      (passedCallback as Function)(null);
      
      expect(callback).toHaveBeenCalledWith({
        code: -1,
        msg: 'Unknown error',
        data: undefined,
      });
    });
  });
});
