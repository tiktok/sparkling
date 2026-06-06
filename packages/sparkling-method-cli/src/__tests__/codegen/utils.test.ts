import {
  toCamelCase,
  toKebabCase,
  toPascalCase,
  mapPrimitiveToTypeScript,
  mapPrimitiveToJSType
} from '../../codegen/utils';

describe('Codegen Utilities', () => {
  describe('toCamelCase', () => {
    it('should convert snake_case to camelCase', () => {
      expect(toCamelCase('snake_case_string')).toBe('snakeCaseString');
      expect(toCamelCase('simple_test')).toBe('simpleTest');
      expect(toCamelCase('single')).toBe('single');
    });

    it('should convert kebab-case to camelCase', () => {
      expect(toCamelCase('kebab-case-string')).toBe('kebabCaseString');
      expect(toCamelCase('multi-word-test')).toBe('multiWordTest');
    });

    it('should convert PascalCase to camelCase', () => {
      expect(toCamelCase('PascalCaseString')).toBe('pascalCaseString');
      expect(toCamelCase('TestMethod')).toBe('testMethod');
    });

    it('should handle mixed separators', () => {
      expect(toCamelCase('mixed_case-string')).toBe('mixedCaseString');
      expect(toCamelCase('test_method-name')).toBe('testMethodName');
    });

    it('should handle edge cases', () => {
      expect(toCamelCase('')).toBe('');
      expect(toCamelCase('a')).toBe('a');
      expect(toCamelCase('A')).toBe('a');
      expect(toCamelCase('_')).toBe('');
      expect(toCamelCase('-')).toBe('');
      expect(toCamelCase('__test__')).toBe('test');
    });

    it('should handle numbers in strings', () => {
      expect(toCamelCase('test_method_1')).toBe('testMethod1');
      expect(toCamelCase('api_v2_endpoint')).toBe('apiV2Endpoint');
      expect(toCamelCase('2test_method')).toBe('2testMethod');
    });

    it('should handle consecutive separators', () => {
      expect(toCamelCase('test__double__underscore')).toBe('testDoubleUnderscore');
      expect(toCamelCase('test--double--dash')).toBe('testDoubleDash');
      expect(toCamelCase('test_-mixed-_separators')).toBe('testMixedSeparators');
    });
  });

  describe('toKebabCase', () => {
    it('should convert camelCase to kebab-case', () => {
      expect(toKebabCase('camelCaseString')).toBe('camel-case-string');
      expect(toKebabCase('simpleTest')).toBe('simple-test');
      expect(toKebabCase('single')).toBe('single');
    });

    it('should convert PascalCase to kebab-case', () => {
      expect(toKebabCase('PascalCaseString')).toBe('pascal-case-string');
      expect(toKebabCase('TestMethod')).toBe('test-method');
    });

    it('should convert snake_case to kebab-case', () => {
      expect(toKebabCase('snake_case_string')).toBe('snake-case-string');
      expect(toKebabCase('test_method_name')).toBe('test-method-name');
    });

    it('should handle mixed cases', () => {
      expect(toKebabCase('mixedCase_string')).toBe('mixed-case-string');
      expect(toKebabCase('TestMethod_Name')).toBe('test-method-name');
    });

    it('should handle edge cases', () => {
      expect(toKebabCase('')).toBe('');
      expect(toKebabCase('a')).toBe('a');
      expect(toKebabCase('A')).toBe('a');
      expect(toKebabCase('_')).toBe('');
      expect(toKebabCase('__test__')).toBe('test');
    });

    it('should handle numbers in strings', () => {
      expect(toKebabCase('testMethod1')).toBe('test-method1');
      expect(toKebabCase('apiV2Endpoint')).toBe('api-v2-endpoint');
      expect(toKebabCase('2TestMethod')).toBe('2test-method');
    });

    it('should handle consecutive uppercase letters', () => {
      expect(toKebabCase('XMLParser')).toBe('xml-parser');
      expect(toKebabCase('HTTPSConnection')).toBe('https-connection');
      expect(toKebabCase('URLPath')).toBe('url-path');
    });

    it('should handle already kebab-case strings', () => {
      expect(toKebabCase('already-kebab-case')).toBe('already-kebab-case');
      expect(toKebabCase('test-method')).toBe('test-method');
    });
  });

  describe('toPascalCase', () => {
    it('should convert camelCase to PascalCase', () => {
      expect(toPascalCase('camelCaseString')).toBe('CamelCaseString');
      expect(toPascalCase('simpleTest')).toBe('SimpleTest');
      expect(toPascalCase('single')).toBe('Single');
    });

    it('should convert snake_case to PascalCase', () => {
      expect(toPascalCase('snake_case_string')).toBe('SnakeCaseString');
      expect(toPascalCase('test_method')).toBe('TestMethod');
    });

    it('should convert kebab-case to PascalCase', () => {
      expect(toPascalCase('kebab-case-string')).toBe('KebabCaseString');
      expect(toPascalCase('test-method')).toBe('TestMethod');
    });

    it('should handle mixed separators', () => {
      expect(toPascalCase('mixed_case-string')).toBe('MixedCaseString');
      expect(toPascalCase('test_method-name')).toBe('TestMethodName');
    });

    it('should handle edge cases', () => {
      expect(toPascalCase('')).toBe('');
      expect(toPascalCase('a')).toBe('A');
      expect(toPascalCase('_')).toBe('');
      expect(toPascalCase('-')).toBe('');
      expect(toPascalCase('__test__')).toBe('Test');
    });

    it('should handle numbers in strings', () => {
      expect(toPascalCase('test_method_1')).toBe('TestMethod1');
      expect(toPascalCase('api_v2_endpoint')).toBe('ApiV2Endpoint');
      expect(toPascalCase('2test_method')).toBe('2testMethod');
    });

    it('should handle already PascalCase strings', () => {
      expect(toPascalCase('AlreadyPascalCase')).toBe('AlreadyPascalCase');
      expect(toPascalCase('TestMethod')).toBe('TestMethod');
    });

    it('should handle consecutive separators', () => {
      expect(toPascalCase('test__double__underscore')).toBe('TestDoubleUnderscore');
      expect(toPascalCase('test--double--dash')).toBe('TestDoubleDash');
    });
  });

  describe('mapPrimitiveToTypeScript', () => {
    it('should map primitive types correctly', () => {
      expect(mapPrimitiveToTypeScript('string')).toBe('string');
      expect(mapPrimitiveToTypeScript('number')).toBe('number');
      expect(mapPrimitiveToTypeScript('boolean')).toBe('boolean');
      expect(mapPrimitiveToTypeScript('void')).toBe('void');
      expect(mapPrimitiveToTypeScript('object')).toBe('object');
      expect(mapPrimitiveToTypeScript('any')).toBe('any');
    });

    it('should handle unknown types', () => {
      expect(mapPrimitiveToTypeScript('unknown' as any)).toBe('any');
      expect(mapPrimitiveToTypeScript('custom' as any)).toBe('any');
      expect(mapPrimitiveToTypeScript('' as any)).toBe('any');
    });

    it('should be case sensitive', () => {
      expect(mapPrimitiveToTypeScript('String' as any)).toBe('any');
      expect(mapPrimitiveToTypeScript('NUMBER' as any)).toBe('any');
      expect(mapPrimitiveToTypeScript('Boolean' as any)).toBe('any');
    });
  });

  describe('mapPrimitiveToJSType', () => {
    it('should map primitive types to JavaScript types', () => {
      expect(mapPrimitiveToJSType('string')).toBe('string');
      expect(mapPrimitiveToJSType('number')).toBe('number');
      expect(mapPrimitiveToJSType('boolean')).toBe('boolean');
      expect(mapPrimitiveToJSType('object')).toBe('object');
    });

    it('should map void and any to undefined', () => {
      expect(mapPrimitiveToJSType('void')).toBe('undefined');
      expect(mapPrimitiveToJSType('any')).toBe('undefined');
    });

    it('should handle unknown types', () => {
      expect(mapPrimitiveToJSType('unknown' as any)).toBe('undefined');
      expect(mapPrimitiveToJSType('custom' as any)).toBe('undefined');
      expect(mapPrimitiveToJSType('' as any)).toBe('undefined');
    });

    it('should be case sensitive', () => {
      expect(mapPrimitiveToJSType('String' as any)).toBe('undefined');
      expect(mapPrimitiveToJSType('NUMBER' as any)).toBe('undefined');
      expect(mapPrimitiveToJSType('Boolean' as any)).toBe('undefined');
    });
  });

  describe('integration tests', () => {
    it('should work together for method name transformations', () => {
      const methodName = 'show_toast_message';

      expect(toCamelCase(methodName)).toBe('showToastMessage');
      expect(toPascalCase(methodName)).toBe('ShowToastMessage');
      expect(toKebabCase(methodName)).toBe('show-toast-message');
    });

    it('should handle round-trip conversions', () => {
      const original = 'testMethodName';

      const kebab = toKebabCase(original);
      const backToCamel = toCamelCase(kebab);
      const backToPascal = toPascalCase(kebab);

      expect(backToCamel).toBe('testMethodName');
      expect(backToPascal).toBe('TestMethodName');
    });

    it('should handle complex method names consistently', () => {
      const complexName = 'get_user_profile_by_id';

      expect(toCamelCase(complexName)).toBe('getUserProfileById');
      expect(toPascalCase(complexName)).toBe('GetUserProfileById');
      expect(toKebabCase(complexName)).toBe('get-user-profile-by-id');
    });
  });

  describe('edge cases and error handling', () => {
    it('should handle null and undefined inputs gracefully', () => {
      expect(toCamelCase(null as any)).toBe('');
      expect(toCamelCase(undefined as any)).toBe('');
      expect(toKebabCase(null as any)).toBe('');
      expect(toKebabCase(undefined as any)).toBe('');
      expect(toPascalCase(null as any)).toBe('');
      expect(toPascalCase(undefined as any)).toBe('');
    });

    it('should handle non-string inputs', () => {
      expect(toCamelCase(123 as any)).toBe('');
      expect(toKebabCase(true as any)).toBe('');
      expect(toPascalCase({} as any)).toBe('');
    });

    it('should handle strings with only separators', () => {
      expect(toCamelCase('___')).toBe('');
      expect(toCamelCase('---')).toBe('');
      expect(toCamelCase('_-_-_')).toBe('');
      expect(toKebabCase('___')).toBe('');
      expect(toPascalCase('---')).toBe('');
    });

    it('should handle very long strings', () => {
      const longString = 'very_long_method_name_with_many_words_that_should_be_converted_properly';
      const camelResult = toCamelCase(longString);
      const kebabResult = toKebabCase(longString);
      const pascalResult = toPascalCase(longString);

      expect(camelResult).toMatch(/^veryLongMethodName/);
      expect(kebabResult).toMatch(/^very-long-method-name/);
      expect(pascalResult).toMatch(/^VeryLongMethodName/);
    });

    it('should handle unicode characters', () => {
      expect(toCamelCase('test_méthod')).toBe('testMéthod');
      expect(toKebabCase('testMéthod')).toBe('test-méthod');
      expect(toPascalCase('test_méthod')).toBe('TestMéthod');
    });
  });

  describe('performance considerations', () => {
    it('should handle repeated conversions efficiently', () => {
      const testString = 'performance_test_method';
      const start = performance.now();

      for (let i = 0; i < 1000; i++) {
        toCamelCase(testString);
        toKebabCase(testString);
        toPascalCase(testString);
      }

      const end = performance.now();
      expect(end - start).toBeLessThan(200); // Should complete in less than 200ms (CI machines may be slower)
    });

    it('should not create memory leaks with large inputs', () => {
      const largeString = 'test_'.repeat(1000) + 'method';

      expect(() => {
        toCamelCase(largeString);
        toKebabCase(largeString);
        toPascalCase(largeString);
      }).not.toThrow();
    });
  });

  describe('snapshot testing', () => {
    it('should maintain consistent transformation results', () => {
      const testCases = [
        'simple',
        'camelCase',
        'PascalCase',
        'snake_case',
        'kebab-case',
        'mixed_case-string',
        'XMLHttpRequest',
        'getUserById',
        'API_ENDPOINT_URL'
      ];

      const transformations = testCases.map(str => ({
        original: str,
        camel: toCamelCase(str),
        kebab: toKebabCase(str),
        pascal: toPascalCase(str),
        tsType: mapPrimitiveToTypeScript('string'),
        jsType: mapPrimitiveToJSType('string')
      }));

      expect(transformations).toMatchSnapshot('string-transformations');
    });

    it('should maintain consistent type mappings', () => {
      const primitiveTypes = ['string', 'number', 'boolean', 'void', 'object', 'any'];

      const mappings = primitiveTypes.map(type => ({
        primitive: type,
        typescript: mapPrimitiveToTypeScript(type as any),
        javascript: mapPrimitiveToJSType(type as any)
      }));

      expect(mappings).toMatchSnapshot('type-mappings');
    });
  });
});