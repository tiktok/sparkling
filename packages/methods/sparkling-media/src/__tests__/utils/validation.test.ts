// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import {
    BaseResponse,
    ValidationRule,
    createErrorResponse,
    isValidCallback,
    validateParams,
    validationRules,
    logInvalidCallback,
} from '../../utils/validation';

interface TestRequest {
    url?: string;
    items?: string[];
    sourceType?: string;
    cameraType?: string;
}

interface TestResponse extends BaseResponse {
    data?: string;
}

describe('validation utilities', () => {
    describe('createErrorResponse', () => {
        it('should create error response with code -1', () => {
            const response = createErrorResponse<TestResponse>('Test error');
            expect(response.code).toBe(-1);
            expect(response.msg).toBe('Test error');
        });

        it('should work with different response types', () => {
            interface CustomResponse extends BaseResponse {
                customField?: number;
            }
            const response = createErrorResponse<CustomResponse>('Custom error');
            expect(response.code).toBe(-1);
            expect(response.msg).toBe('Custom error');
        });
    });

    describe('isValidCallback', () => {
        it('should return true for function', () => {
            expect(isValidCallback(() => {})).toBe(true);
            expect(isValidCallback(function() {})).toBe(true);
        });

        it('should return false for non-function values', () => {
            expect(isValidCallback(null)).toBe(false);
            expect(isValidCallback(undefined)).toBe(false);
            expect(isValidCallback('string')).toBe(false);
            expect(isValidCallback(123)).toBe(false);
            expect(isValidCallback({})).toBe(false);
            expect(isValidCallback([])).toBe(false);
        });
    });

    describe('validateParams', () => {
        it('should return null when all rules pass', () => {
            const params: TestRequest = { url: 'https://example.com' };
            const callback = jest.fn();
            const rules: ValidationRule<TestRequest>[] = [
                validationRules.paramsRequired<TestRequest>(),
                validationRules.requiredString<TestRequest>('url'),
            ];

            const result = validateParams<TestRequest, TestResponse>(params, callback, rules);
            
            expect(result).toBeNull();
            expect(callback).not.toHaveBeenCalled();
        });

        it('should return error and call callback when rule fails', () => {
            const params = null as unknown as TestRequest;
            const callback = jest.fn();
            const rules: ValidationRule<TestRequest>[] = [
                validationRules.paramsRequired<TestRequest>(),
            ];

            const result = validateParams<TestRequest, TestResponse>(params, callback, rules);
            
            expect(result).not.toBeNull();
            expect(result?.code).toBe(-1);
            expect(result?.msg).toBe('Invalid params: params cannot be null or undefined');
            expect(callback).toHaveBeenCalledWith(result);
        });

        it('should not call callback if it is not a function', () => {
            const params = null as unknown as TestRequest;
            const callback = 'not a function';
            const rules: ValidationRule<TestRequest>[] = [
                validationRules.paramsRequired<TestRequest>(),
            ];

            const result = validateParams<TestRequest, TestResponse>(params, callback, rules);
            
            expect(result).not.toBeNull();
            expect(result?.code).toBe(-1);
        });

        it('should stop at first failing rule', () => {
            const params: TestRequest = { url: '' };
            const callback = jest.fn();
            const rule1Check = jest.fn().mockReturnValue(true);
            const rule2Check = jest.fn().mockReturnValue(false);
            const rule3Check = jest.fn().mockReturnValue(true);

            const rules: ValidationRule<TestRequest>[] = [
                { check: rule1Check, message: 'Rule 1 failed' },
                { check: rule2Check, message: 'Rule 2 failed' },
                { check: rule3Check, message: 'Rule 3 failed' },
            ];

            const result = validateParams<TestRequest, TestResponse>(params, callback, rules);
            
            expect(result?.msg).toBe('Rule 2 failed');
            expect(rule1Check).toHaveBeenCalled();
            expect(rule2Check).toHaveBeenCalled();
            expect(rule3Check).not.toHaveBeenCalled();
        });
    });

    describe('validationRules', () => {
        describe('paramsRequired', () => {
            const rule = validationRules.paramsRequired<TestRequest>();

            it('should pass for valid object', () => {
                expect(rule.check({ url: 'test' })).toBe(true);
                expect(rule.check({})).toBe(true);
            });

            it('should fail for null or undefined', () => {
                expect(rule.check(null as unknown as TestRequest)).toBe(false);
                expect(rule.check(undefined as unknown as TestRequest)).toBe(false);
            });

            it('should have correct error message', () => {
                expect(rule.message).toBe('Invalid params: params cannot be null or undefined');
            });
        });

        describe('requiredString', () => {
            const rule = validationRules.requiredString<TestRequest>('url');

            it('should pass for non-empty string', () => {
                expect(rule.check({ url: 'https://example.com' })).toBe(true);
                expect(rule.check({ url: 'a' })).toBe(true);
            });

            it('should fail for empty string', () => {
                expect(rule.check({ url: '' })).toBe(false);
                expect(rule.check({ url: '   ' })).toBe(false);
            });

            it('should fail for non-string values', () => {
                expect(rule.check({ url: undefined })).toBe(false);
                expect(rule.check({ url: 123 as unknown as string })).toBe(false);
                expect(rule.check({})).toBe(false);
            });

            it('should fail for null params', () => {
                expect(rule.check(null as unknown as TestRequest)).toBe(false);
            });

            it('should use custom field name in error message', () => {
                const customRule = validationRules.requiredString<TestRequest>('url', 'URL address');
                expect(customRule.message).toBe('Invalid params: URL address must be a non-empty string');
            });

            it('should use field name as default in error message', () => {
                expect(rule.message).toBe('Invalid params: url must be a non-empty string');
            });
        });

        describe('requiredArray', () => {
            const rule = validationRules.requiredArray<TestRequest>('items');

            it('should pass for non-empty array', () => {
                expect(rule.check({ items: ['a'] })).toBe(true);
                expect(rule.check({ items: ['a', 'b', 'c'] })).toBe(true);
            });

            it('should fail for empty array', () => {
                expect(rule.check({ items: [] })).toBe(false);
            });

            it('should fail for non-array values', () => {
                expect(rule.check({ items: undefined })).toBe(false);
                expect(rule.check({ items: 'string' as unknown as string[] })).toBe(false);
                expect(rule.check({})).toBe(false);
            });

            it('should fail for null params', () => {
                expect(rule.check(null as unknown as TestRequest)).toBe(false);
            });

            it('should use custom field name in error message', () => {
                const customRule = validationRules.requiredArray<TestRequest>('items', 'media items');
                expect(customRule.message).toBe('Invalid params: media items must be a non-empty array');
            });

            it('should use field name as default in error message', () => {
                expect(rule.message).toBe('Invalid params: items must be a non-empty array');
            });
        });

        describe('enumValue', () => {
            const rule = validationRules.enumValue<TestRequest>('sourceType', ['album', 'camera']);

            it('should pass for allowed values', () => {
                expect(rule.check({ sourceType: 'album' })).toBe(true);
                expect(rule.check({ sourceType: 'camera' })).toBe(true);
            });

            it('should fail for non-allowed values', () => {
                expect(rule.check({ sourceType: 'invalid' })).toBe(false);
                expect(rule.check({ sourceType: '' })).toBe(false);
            });

            it('should fail for non-string values', () => {
                expect(rule.check({ sourceType: undefined })).toBe(false);
                expect(rule.check({ sourceType: 123 as unknown as string })).toBe(false);
                expect(rule.check({})).toBe(false);
            });

            it('should fail for null params', () => {
                expect(rule.check(null as unknown as TestRequest)).toBe(false);
            });

            it('should format error message correctly', () => {
                expect(rule.message).toBe('Invalid params: sourceType must be "album" or "camera"');
            });

            it('should use custom field name in error message', () => {
                const customRule = validationRules.enumValue<TestRequest>('sourceType', ['album', 'camera'], 'source type');
                expect(customRule.message).toBe('Invalid params: source type must be "album" or "camera"');
            });
        });

        describe('conditional', () => {
            it('should apply rule when condition is true', () => {
                const rule = validationRules.conditional<TestRequest>(
                    (params) => params.sourceType === 'camera',
                    validationRules.requiredString<TestRequest>('cameraType')
                );

                // Condition true, cameraType missing - should fail
                expect(rule.check({ sourceType: 'camera' })).toBe(false);
                
                // Condition true, cameraType present - should pass
                expect(rule.check({ sourceType: 'camera', cameraType: 'front' })).toBe(true);
            });

            it('should skip rule when condition is false', () => {
                const rule = validationRules.conditional<TestRequest>(
                    (params) => params.sourceType === 'camera',
                    validationRules.requiredString<TestRequest>('cameraType')
                );

                // Condition false, cameraType missing - should still pass
                expect(rule.check({ sourceType: 'album' })).toBe(true);
            });

            it('should return true for null params (let paramsRequired handle it)', () => {
                const rule = validationRules.conditional<TestRequest>(
                    (params) => params.sourceType === 'camera',
                    validationRules.requiredString<TestRequest>('cameraType')
                );

                expect(rule.check(null as unknown as TestRequest)).toBe(true);
            });

            it('should use inner rule error message', () => {
                const innerRule = validationRules.requiredString<TestRequest>('cameraType', 'camera type');
                const rule = validationRules.conditional<TestRequest>(
                    (params) => params.sourceType === 'camera',
                    innerRule
                );

                expect(rule.message).toBe('Invalid params: camera type must be a non-empty string');
            });
        });

        describe('custom', () => {
            it('should allow custom validation logic', () => {
                const rule = validationRules.custom<TestRequest>(
                    (params) => params.url?.startsWith('https://') ?? false,
                    'Invalid params: url must start with https://'
                );

                expect(rule.check({ url: 'https://example.com' })).toBe(true);
                expect(rule.check({ url: 'http://example.com' })).toBe(false);
                expect(rule.check({ url: undefined })).toBe(false);
            });

            it('should use provided error message', () => {
                const rule = validationRules.custom<TestRequest>(
                    () => false,
                    'Custom error message'
                );

                expect(rule.message).toBe('Custom error message');
            });
        });
    });

    describe('logInvalidCallback', () => {
        it('should log error with method name', () => {
            const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
            
            logInvalidCallback('testMethod');
            
            expect(consoleSpy).toHaveBeenCalledWith('[sparkling-media] testMethod: callback must be a function');
            consoleSpy.mockRestore();
        });
    });
});

import { mapPipeResponse } from '../../utils/validation';

interface SampleResponse extends BaseResponse {
    data?: {
        filePath?: string;
        url?: string;
        __status_message__?: string;
    };
}

describe('mapPipeResponse', () => {
    it('maps pipe code 1 to business code 0 (success)', () => {
        const raw = { code: 1, msg: 'ok', data: { filePath: '/tmp/file.jpg' } };
        const result = mapPipeResponse<SampleResponse>(raw);
        expect(result.code).toBe(0);
        expect(result.msg).toBe('ok');
        expect(result.data).toEqual({ filePath: '/tmp/file.jpg' });
    });

    it('maps pipe code 0 to business code -1 (failure)', () => {
        const raw = { code: 0, msg: 'failed' };
        const result = mapPipeResponse<SampleResponse>(raw);
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('failed');
    });

    it('preserves negative pipe codes as-is', () => {
        const raw = { code: -2, msg: 'permission denied' };
        const result = mapPipeResponse<SampleResponse>(raw);
        expect(result.code).toBe(-2);
        expect(result.msg).toBe('permission denied');
    });

    it('returns code -1 and "Unknown error" for null raw value', () => {
        const result = mapPipeResponse<SampleResponse>(null);
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Unknown error');
        expect(result.data).toBeUndefined();
    });

    it('returns code -1 for undefined raw value', () => {
        const result = mapPipeResponse<SampleResponse>(undefined);
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('Unknown error');
    });

    it('falls back to "ok" message for success when msg is missing', () => {
        const raw = { code: 1, data: { url: 'https://cdn.example.com/img.png' } };
        const result = mapPipeResponse<SampleResponse>(raw);
        expect(result.code).toBe(0);
        expect(result.msg).toBe('ok');
    });

    it('extracts msg from data.__status_message__ when top-level msg is absent', () => {
        const raw = { code: 0, data: { __status_message__: 'quota exceeded' } };
        const result = mapPipeResponse<SampleResponse>(raw);
        expect(result.msg).toBe('quota exceeded');
    });

    it('removes __status_message__ from data after extraction', () => {
        const raw = {
            code: 1,
            msg: 'Success',
            data: { filePath: '/tmp/file.jpg', __status_message__: 'internal note' },
        };
        const result = mapPipeResponse<SampleResponse>(raw);
        expect(result.data).not.toHaveProperty('__status_message__');
        expect(result.data).toHaveProperty('filePath', '/tmp/file.jpg');
    });

    it('does not fail when data is absent on success', () => {
        const raw = { code: 1, msg: 'done' };
        const result = mapPipeResponse<SampleResponse>(raw);
        expect(result.code).toBe(0);
        expect(result.data).toBeUndefined();
    });

    it('handles non-object raw value gracefully', () => {
        const result = mapPipeResponse<SampleResponse>('unexpected string' as unknown);
        expect(result.code).toBe(-1);
    });

    it('handles raw value with missing code field', () => {
        const raw = { msg: 'no code here' };
        const result = mapPipeResponse<SampleResponse>(raw);
        expect(result.code).toBe(-1);
        expect(result.msg).toBe('no code here');
    });
});
