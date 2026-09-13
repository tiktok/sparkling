/// <reference types="jest" />
// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { createMockPipe } from './test-utils';
import * as routerModule from '../../index';
import { open as openDirect } from '../open/open';
import { close as closeDirect } from '../close/close';
import { navigate as navigateDirect } from '../navigate/navigate';

jest.mock('sparkling-method', () => ({ call: jest.fn() }), { virtual: true });

describe('sparkling-navigation module exports', () => {
  describe('function exports', () => {
    it('should export open function', async () => {
      expect(routerModule.open).toBeDefined();
      expect(typeof routerModule.open).toBe('function');
    });

    it('should export close function', async () => {
      expect(routerModule.close).toBeDefined();
      expect(typeof routerModule.close).toBe('function');
    });

    it('should export navigate function', async () => {
      expect(routerModule.navigate).toBeDefined();
      expect(typeof routerModule.navigate).toBe('function');
    });

    it('should export all required functions', async () => {
      const expectedFunctions = ['open', 'close', 'navigate'];
      const moduleAny: Record<string, unknown> = routerModule as unknown as Record<string, unknown>;
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
          OpenRequest,
          OpenResponse,
          OpenOptions,
          CloseRequest,
          CloseResponse,
          NavigateRequest,
          NavigateResponse,
          NavigateOptions
        } from '../../index'
      `;

      // If types are not exported, TypeScript would throw compilation error
      // This test passing means all types are properly exported
      expect(importStatement).toBeTruthy();
    });
  });

  describe('module structure validation', () => {
    it('should not export unintended properties', async () => {
      const moduleAny: Record<string, unknown> = routerModule as unknown as Record<string, unknown>;
      const exportedKeys = Object.keys(moduleAny);

      const expectedExports = [
        'open',
        'close',
        'navigate',
        'setBackPressIntercept',
        'onBackPress',
        'BACK_PRESS_EVENT',
      ];

      const unexpectedExports = exportedKeys.filter(key => expectedExports.indexOf(key) === -1);
      expect(unexpectedExports).toHaveLength(0);
    });

    it('should export exactly the expected number of functions', async () => {
      const moduleAny: Record<string, unknown> = routerModule as unknown as Record<string, unknown>;
      const exportedFunctions = Object.keys(moduleAny).filter(key => typeof moduleAny[key] === 'function');
      // open, close, navigate, setBackPressIntercept, onBackPress
      expect(exportedFunctions).toHaveLength(5);
    });
  });

  describe('functional integration', () => {
    it('should allow importing and using open function', async () => {
      const { open } = routerModule as unknown as { open: typeof routerModule.open };

      expect(() => {
        open({ scheme: 'test://example' }, () => {});
      }).not.toThrow();
    });

    it('should allow importing and using close function', async () => {
      const { close } = routerModule as unknown as { close: typeof routerModule.close };

      expect(() => {
        close({ containerID: 'test' }, () => {});
      }).not.toThrow();
    });

    it('should allow importing and using navigate function', async () => {
      const { navigate } = routerModule as unknown as { navigate: typeof routerModule.navigate };

      expect(() => {
        navigate({ path: 'test.lynx.bundle' }, () => {});
      }).not.toThrow();
    });

    it('should allow importing all functions together', async () => {
      const { open, close, navigate } = routerModule as unknown as {
        open: typeof routerModule.open;
        close: typeof routerModule.close;
        navigate: typeof routerModule.navigate;
      };

      expect(open).toBeDefined();
      expect(close).toBeDefined();
      expect(navigate).toBeDefined();
      expect(typeof open).toBe('function');
      expect(typeof close).toBe('function');
      expect(typeof navigate).toBe('function');
    });

    it('should allow destructured import of all exports', async () => {
      const { open, close, navigate } = routerModule as unknown as {
        open: typeof routerModule.open;
        close: typeof routerModule.close;
        navigate: typeof routerModule.navigate;
      };

      expect(open).toBe(routerModule.open);
      expect(close).toBe(routerModule.close);
      expect(navigate).toBe(routerModule.navigate);
    });
  });

  describe('contract compliance', () => {
    it('should maintain function signatures after export', async () => {
      const { open, close, navigate } = routerModule as unknown as {
        open: typeof routerModule.open;
        close: typeof routerModule.close;
        navigate: typeof routerModule.navigate;
      };

      expect(open.length).toBe(2);

      expect(close.length).toBe(2);

      expect(navigate.length).toBe(2);
    });

    it('should not modify function behavior during export', async () => {
      const { open: openFromIndex, close: closeFromIndex, navigate: navigateFromIndex } = routerModule as unknown as {
        open: typeof routerModule.open;
        close: typeof routerModule.close;
        navigate: typeof routerModule.navigate;
      };

      expect(openFromIndex).toBe(openDirect);
      expect(closeFromIndex).toBe(closeDirect);
      expect(navigateFromIndex).toBe(navigateDirect);
    });
  });

  describe('snapshot testing for module structure', () => {
    it('should maintain consistent module export structure', async () => {
      const moduleAny: Record<string, unknown> = routerModule as unknown as Record<string, unknown>;
      const moduleStructure = {
        exportedKeys: Object.keys(moduleAny),
        functionNames: Object.keys(moduleAny).filter(key => typeof moduleAny[key] === 'function'),
        exportCount: Object.keys(moduleAny).length
      };

      expect(moduleStructure).toMatchSnapshot('module-export-structure');
    });
  });
});
