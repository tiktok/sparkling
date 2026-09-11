// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'path';
import fs from 'fs-extra';
import Mustache from 'mustache';

import {
  DefaultValue,
  MethodDefinition,
  ModuleConfig,
  ObjectDefinition,
  PrimitiveKind,
  SwiftStructView,
  SwiftTemplateView,
  TypeSummary
} from './types';
import { ensureObjectDefinition } from './definition-parser';
import { toPascalCase } from './utils';

export function buildSwiftView(method: MethodDefinition, config: ModuleConfig): SwiftTemplateView {
  const methodPascal = toPascalCase(method.name);
  const requestObject = ensureObjectDefinition(method.request, `${methodPascal}Request`, method.interfaces);
  const responseObject = ensureObjectDefinition(method.response, `${methodPascal}Response`, method.interfaces);
  const swiftContext: SwiftBuildContext = {
    methodPascal,
    interfaces: method.interfaces,
    extras: new Map()
  };

  const requestView = toSwiftStructView(requestObject, `SPK${methodPascal}MethodParamModel`, swiftContext, ['Param']);
  const responseView = toSwiftStructView(responseObject, `SPK${methodPascal}MethodResultModel`, swiftContext, ['Result']);

  return {
    moduleName: config.moduleName,
    methodName: method.name,
    methodClassName: `SPK${methodPascal}Method`,
    methodObjcName: `SPK${methodPascal}Method`,
    request: requestView,
    response: responseView,
    extraTypes: Array.from(swiftContext.extras.values())
  };
}

interface SwiftBuildContext {
  methodPascal: string;
  interfaces: Record<string, ObjectDefinition>;
  extras: Map<string, SwiftStructView>;
}

function toSwiftStructView(
  definition: ObjectDefinition,
  className: string,
  context: SwiftBuildContext,
  path: string[]
): SwiftStructView {
  const fields = definition.fields.map((field, index, arr) =>
    buildSwiftField(field, context, path, index === arr.length - 1)
  );
  return {
    structName: className,
    objcName: className,
    hasFields: fields.length > 0,
    fields
  };
}

/**
 * One field of a generated model, in a shape Objective-C can represent.
 *
 * Two rules decide the output, and both exist because the models are `@objc`
 * classes bridged to Objective-C:
 *
 * - An optional `number` or `boolean` cannot be `Double?` / `Bool?`: Objective-C
 *   has no optional value types, so `@objc` rejects the property outright. They
 *   bridge through `NSNumber?`, which is what an absent key on the wire actually
 *   looks like.
 * - A non-optional stored property with no initial value leaves the class with
 *   no initializers, which is a compile error rather than a runtime surprise.
 *   Fields the author did not give a default get the empty value for their type.
 */
function buildSwiftField(
  field: ObjectDefinition['fields'][number],
  context: SwiftBuildContext,
  path: string[],
  isLast: boolean
) {
  const optionalValueType =
    field.optional &&
    field.type.kind === 'primitive' &&
    (field.type.name === 'number' || field.type.name === 'boolean');

  const type = optionalValueType
    ? 'NSNumber'
    : resolveSwiftType(field.type, context, [...path, field.name]);

  const declared = formatSwiftDefaultValue(field.defaultValue);
  const defaultValue = field.optional
    ? declared
    : declared ?? emptySwiftValue(type, field.type);

  return {
    name: field.name,
    type,
    optional: field.optional,
    comment: field.description,
    defaultValue,
    isLast
  };
}

/** The value a required field starts at, so the class has an initializer. */
function emptySwiftValue(swiftType: string, type: TypeSummary): string | undefined {
  if (swiftType.startsWith('[') && swiftType.endsWith(']')) {
    return swiftType.includes(':') ? '[:]' : '[]';
  }
  if (type.kind === 'primitive') {
    switch (type.name) {
      case 'string':
        return '""';
      case 'number':
        return '0';
      case 'boolean':
        return 'false';
      case 'void':
        return undefined;
      default:
        // `any` bridges to `id`, which has no empty literal of its own.
        return 'NSNull()';
    }
  }
  // A model type: its own generated class is always default-constructible.
  return `${swiftType}()`;
}

function resolveSwiftType(type: TypeSummary, context: SwiftBuildContext, path: string[]): string {
  switch (type.kind) {
    case 'primitive':
      return mapPrimitiveToSwift(type.name);
    case 'array':
      return `[${resolveSwiftType(type.elementType, context, path)}]`;
    case 'reference': {
      const definition = context.interfaces[type.name];
      if (definition) {
        const helper = ensureSwiftHelper(definition, context, [type.name]);
        return helper.structName;
      }
      return type.name;
    }
    case 'object': {
      const inline: ObjectDefinition = {
        name: path.map((segment) => toPascalCase(segment)).join('') || undefined,
        fields: type.fields
      };
      const helper = ensureSwiftHelper(inline, context, path);
      return helper.structName;
    }
    default:
      return 'Any';
  }
}

function ensureSwiftHelper(definition: ObjectDefinition, context: SwiftBuildContext, path: string[]): SwiftStructView {
  const key = definition.name ?? path.join('.');
  const cached = context.extras.get(key);
  if (cached) {
    return cached;
  }
  const base = definition.name ? toPascalCase(definition.name) : path.map((segment) => toPascalCase(segment)).join('');
  const suffix = base || 'Anonymous';
  const className = `SPK${context.methodPascal}Method${suffix}Model`;
  const fields = definition.fields.map((field, index, arr) =>
    buildSwiftField(field, context, path, index === arr.length - 1)
  );
  const view: SwiftStructView = {
    structName: className,
    objcName: className,
    hasFields: fields.length > 0,
    fields
  };
  context.extras.set(key, view);
  return view;
}

function mapPrimitiveToSwift(kind: PrimitiveKind): string {
  switch (kind) {
    case 'string':
      return 'String';
    case 'number':
      return 'Double';
    case 'boolean':
      return 'Bool';
    case 'void':
      return 'Void';
    case 'object':
      return '[String: Any]';
    default:
      return 'Any';
  }
}

function formatSwiftDefaultValue(value: DefaultValue | undefined): string | undefined {
  if (!value) {
    return undefined;
  }
  switch (value.kind) {
    case 'boolean':
      return value.value ? 'true' : 'false';
    case 'number':
      return `${value.value}`;
    case 'string':
      return JSON.stringify(value.value);
    default:
      return undefined;
  }
}

export async function writeSwiftFile(
  root: string,
  config: ModuleConfig,
  method: MethodDefinition,
  view: SwiftTemplateView,
  template: string
): Promise<void> {
  const moduleDir = toPascalCase(config.moduleName);
  const methodDir = toPascalCase(method.name);
  const swiftDir = path.join(root, 'ios', 'Source', 'Core', moduleDir, methodDir);
  await fs.ensureDir(swiftDir);
  const filePath = path.join(swiftDir, `${methodDir}IDL.swift`);
  const rendered = Mustache.render(template, view);
  await fs.writeFile(filePath, rendered, 'utf8');
}
