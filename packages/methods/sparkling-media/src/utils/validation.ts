// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * Base response interface that all method responses extend
 */
export interface BaseResponse {
    code: number;
    msg: string;
}

/**
 * Validation rule definition
 */
export interface ValidationRule<T> {
    /** Returns true if validation passes, false if it fails */
    check: (params: T) => boolean;
    /** Error message to return when validation fails */
    message: string;
}

/**
 * Create an error response with the given message
 * @param msg - Error message
 * @returns Error response object with code -1
 */
export function createErrorResponse<R extends BaseResponse>(msg: string): R {
    return {
        code: -1,
        msg,
    } as R;
}

/**
 * Check if callback is a valid function
 * @param callback - The callback to validate
 * @returns True if callback is a function
 */
export function isValidCallback<T>(callback: unknown): callback is (result: T) => void {
    return typeof callback === 'function';
}

/**
 * Validate parameters against a set of rules and invoke callback with error if validation fails
 * @param params - Parameters to validate
 * @param callback - Callback function to invoke with error response
 * @param rules - Array of validation rules to check
 * @returns Error response if validation fails, null if all validations pass
 */
export function validateParams<P, R extends BaseResponse>(
    params: P,
    callback: ((result: R) => void) | unknown,
    rules: ValidationRule<P>[]
): R | null {
    for (const rule of rules) {
        if (!rule.check(params)) {
            const errorResponse = createErrorResponse<R>(rule.message);
            if (isValidCallback<R>(callback)) {
                callback(errorResponse);
            }
            return errorResponse;
        }
    }
    return null;
}

/**
 * Pre-built validation rule factories
 */
export const validationRules = {
    /**
     * Rule that checks if params is not null or undefined
     */
    paramsRequired: <T>(): ValidationRule<T> => ({
        check: (params: T) => params != null,
        message: 'Invalid params: params cannot be null or undefined',
    }),

    /**
     * Rule that checks if a string field is non-empty
     * @param field - The field name to check
     * @param fieldName - Optional display name for error message (defaults to field)
     */
    requiredString: <T>(field: keyof T, fieldName?: string): ValidationRule<T> => ({
        check: (params: T) => {
            if (params == null) return false;
            const value = params[field];
            return typeof value === 'string' && value.trim().length > 0;
        },
        message: `Invalid params: ${fieldName ?? String(field)} must be a non-empty string`,
    }),

    /**
     * Rule that checks if an array field is non-empty
     * @param field - The field name to check
     * @param fieldName - Optional display name for error message (defaults to field)
     */
    requiredArray: <T>(field: keyof T, fieldName?: string): ValidationRule<T> => ({
        check: (params: T) => {
            if (params == null) return false;
            const value = params[field];
            return Array.isArray(value) && value.length > 0;
        },
        message: `Invalid params: ${fieldName ?? String(field)} must be a non-empty array`,
    }),

    /**
     * Rule that checks if a field value is one of the allowed values
     * @param field - The field name to check
     * @param allowed - Array of allowed values
     * @param fieldName - Optional display name for error message (defaults to field)
     */
    enumValue: <T>(field: keyof T, allowed: string[], fieldName?: string): ValidationRule<T> => ({
        check: (params: T) => {
            if (params == null) return false;
            const value = params[field];
            return typeof value === 'string' && allowed.includes(value);
        },
        message: `Invalid params: ${fieldName ?? String(field)} must be ${allowed.map(v => `"${v}"`).join(' or ')}`,
    }),

    /**
     * Rule that checks a conditional requirement (e.g., field B required when field A has value X)
     * @param condition - Function that returns true when the conditional check should be applied
     * @param rule - The validation rule to apply when condition is true
     */
    conditional: <T>(
        condition: (params: T) => boolean,
        rule: ValidationRule<T>
    ): ValidationRule<T> => ({
        check: (params: T) => {
            if (params == null) return true; // Let paramsRequired handle null
            if (!condition(params)) return true; // Condition not met, skip this rule
            return rule.check(params);
        },
        message: rule.message,
    }),

    /**
     * Custom validation rule
     * @param check - Custom check function
     * @param message - Error message when check fails
     */
    custom: <T>(check: (params: T) => boolean, message: string): ValidationRule<T> => ({
        check,
        message,
    }),
};

/**
 * Log an error for invalid callback (use when callback validation fails)
 * @param methodName - Name of the method for logging
 */
export function logInvalidCallback(methodName: string): void {
    console.error(`[sparkling-media] ${methodName}: callback must be a function`);
}

/**
 * Map a raw pipe response to a method response.
 *
 * The native pipe layer uses `code: 1` for success and `code: 0` for failure,
 * with negative values for bridge and method errors. Sparkling Method packages
 * expose those native codes unchanged.
 *
 * @param raw - The raw value returned from `pipe.call`
 * @returns A normalised response with `code`, `msg`, and optional `data`
 */
export function mapPipeResponse<R extends BaseResponse>(raw: unknown): R {
    const obj = raw as Record<string, any> | null | undefined;
    const pipeCode = obj?.code;
    // Pipe status codes: 1 = succeeded, 0 = failed, negative = various errors
    const isSuccess = pipeCode === 1;
    const code = pipeCode ?? -1;
    const msg = obj?.msg ?? obj?.data?.__status_message__ ?? (isSuccess ? 'ok' : 'Unknown error');
    const data = obj?.data;
    // Remove internal status message key from data if present
    if (data && typeof data === 'object' && '__status_message__' in data) {
        delete data.__status_message__;
    }
    return { code, msg, data } as unknown as R;
}
