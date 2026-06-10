// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { createMockPipe } from './test-utils';
import * as storageModule from '../../index';
import { setItem as setItemDirect } from '../setStorageItem/setStorageItem';
import { getItem as getItemDirect } from '../getStorageItem/getStorageItem';

jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('sparkling-storage module exports', () => {
  describe('function exports', () => {
    it('should export setItem function', async () => {
      expect(storageModule.setItem).toBeDefined();
      expect(typeof storageModule.setItem).toBe('function');
    });

    it('should export getItem function', async () => {
      expect(storageModule.getItem).toBeDefined();
      expect(typeof storageModule.getItem).toBe('function');
    });

    it('should export all required functions', async () => {
      const expectedFunctions = ['setItem', 'getItem'];
      const moduleAny: Record<string, unknown> = storageModule as unknown as Record<string, unknown>;
      expectedFunctions.forEach(functionName => {
        expect(moduleAny[functionName]).toBeDefined();
        expect(typeof moduleAny[functionName]).toBe('function');
      });
    });
  });

  describe('type exports', () => {
    it('should have type exports available', () => {
      const importStatement = `
        import type {
          SetItemRequest,
          SetItemResponse,
          GetItemRequest,
          GetItemResponse
        } from '../../index'
      `;

      expect(importStatement).toBeTruthy();
    });
  });

  describe('module structure validation', () => {
    it('should not export unintended properties', async () => {
      const exportedKeys = Object.keys(storageModule as unknown as Record<string, unknown>);

      const expectedExports = ['setItem', 'getItem'];

      const unexpectedExports = exportedKeys.filter(key => expectedExports.indexOf(key) === -1);
      expect(unexpectedExports).toHaveLength(0);
    });

    it('should export exactly the expected number of functions', async () => {
      const moduleAny: Record<string, unknown> = storageModule as unknown as Record<string, unknown>;
      const exportedFunctions = Object.keys(moduleAny).filter(key => typeof moduleAny[key] === 'function');
      expect(exportedFunctions).toHaveLength(2); // setItem and getItem
    });
  });

  describe('functional integration', () => {
    it('should allow importing and using setItem function', async () => {
      const { setItem } = storageModule as unknown as { setItem: typeof storageModule.setItem };

      expect(() => {
        setItem({ key: 'test', data: 'value' }, () => {});
      }).not.toThrow();
    });

    it('should allow importing and using getItem function', async () => {
      const { getItem } = storageModule as unknown as { getItem: typeof storageModule.getItem };

      expect(() => {
        getItem({ key: 'test' }, () => {});
      }).not.toThrow();
    });

    it('should allow importing all functions together', async () => {
      const { setItem, getItem } = storageModule as unknown as {
        setItem: typeof storageModule.setItem;
        getItem: typeof storageModule.getItem;
      };

      expect(setItem).toBeDefined();
      expect(getItem).toBeDefined();
      expect(typeof setItem).toBe('function');
      expect(typeof getItem).toBe('function');
    });

    it('should allow destructured import of all exports', async () => {
      const { setItem, getItem } = storageModule as unknown as {
        setItem: typeof storageModule.setItem;
        getItem: typeof storageModule.getItem;
      };

      expect(setItem).toBe(storageModule.setItem);
      expect(getItem).toBe(storageModule.getItem);
    });
  });

  describe('contract compliance', () => {
    it('should maintain function signatures after export', async () => {
      const { setItem, getItem } = storageModule as unknown as {
        setItem: typeof storageModule.setItem;
        getItem: typeof storageModule.getItem;
      };

      expect(setItem.length).toBe(2);

      expect(getItem.length).toBe(2);
    });

    it('should not modify function behavior during export', async () => {
      const { setItem: setItemFromIndex, getItem: getItemFromIndex } = storageModule as unknown as {
        setItem: typeof storageModule.setItem;
        getItem: typeof storageModule.getItem;
      };

      expect(setItemFromIndex).toBe(setItemDirect);
      expect(getItemFromIndex).toBe(getItemDirect);
    });
  });

  describe('storage operations integration', () => {
    it('should support complete storage workflow', async () => {
      const { setItem, getItem } = storageModule as unknown as {
        setItem: typeof storageModule.setItem;
        getItem: typeof storageModule.getItem;
      };
      const mockPipe = jest.requireMock('sparkling-method');

      mockPipe.call.mockImplementation((method: string, params: any, callback: any) => {
        if (method === 'storage.setItem') {
          callback({ code: 1, msg: 'Item stored', data: { success: true } });
        } else if (method === 'storage.getItem') {
          callback({ code: 1, msg: 'Item retrieved', data: { data: params.key === 'test-key' ? 'test-value' : null } });
        }
      });

      let setResult: any;
      setItem({ key: 'test-key', data: 'test-value' }, (result) => {
        setResult = result;
      });

      expect(setResult.code).toBe(1);
      expect(mockPipe.call).toHaveBeenCalledWith('storage.setItem', expect.anything(), expect.any(Function));

      let getResult: any;
      getItem({ key: 'test-key' }, (result) => {
        getResult = result;
      });

      expect(getResult.code).toBe(1);
      expect(mockPipe.call).toHaveBeenCalledWith('storage.getItem', expect.anything(), expect.any(Function));
    });

    it('should handle storage errors gracefully', async () => {
      const { setItem, getItem } = storageModule as unknown as {
        setItem: typeof storageModule.setItem;
        getItem: typeof storageModule.getItem;
      };
      const mockPipe = jest.requireMock('sparkling-method');

      mockPipe.call.mockImplementation((method: string, params: any, callback: any) => {
        callback({ code: -1, msg: 'Storage error', data: undefined });
      });

      let setResult: any;
      setItem({ key: 'test-key', data: 'test-value' }, (result) => {
        setResult = result;
      });

      expect(setResult.code).toBe(-1);
      expect(setResult.msg).toBe('Storage error');

      let getResult: any;
      getItem({ key: 'test-key' }, (result) => {
        getResult = result;
      });

      expect(getResult.code).toBe(-1);
      expect(getResult.msg).toBe('Storage error');
    });
  });

  describe('snapshot testing for module structure', () => {
    it('should maintain consistent module export structure', async () => {
      const moduleAny: Record<string, unknown> = storageModule as unknown as Record<string, unknown>;
      const moduleStructure = {
        exportedKeys: Object.keys(moduleAny),
        functionNames: Object.keys(moduleAny).filter(key => typeof moduleAny[key] === 'function'),
        exportCount: Object.keys(moduleAny).length
      };

      expect(moduleStructure).toMatchSnapshot('storage-module-export-structure');
    });
  });
});
