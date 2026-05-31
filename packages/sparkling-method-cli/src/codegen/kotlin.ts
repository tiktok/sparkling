// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'path';
import fs from 'fs-extra';
import Mustache from 'mustache';

import {
  DefaultValue,
  FieldSummary,
  KotlinEnumEntry,
  KotlinFieldView,
  KotlinInnerClass,
  KotlinModelView,
  KotlinObjectView,
  KotlinTemplateView,
  MethodDefinition,
  ModuleConfig,
  ObjectDefinition,
  PrimitiveKind,
  TypeSummary
} from './types';
import { ensureObjectDefinition } from './definition-parser';
import { buildPackageSegments, splitIntoWords, toPascalCase } from './utils';

export function buildKotlinView(method: MethodDefinition, config: ModuleConfig): KotlinTemplateView {
  const methodPascal = toPascalCase(method.name);
  const packageSegments = buildPackageSegments(config, method.name);
  const className = `Abs${methodPascal}MethodIDL`;
  const requestModel = buildKotlinModel(method.request, `${methodPascal}Request`, method, 'request');
  const responseModel = buildKotlinModel(method.response, `${methodPascal}Response`, method, 'response');

  const paramsList = requestModel.topItem?.items.map((item) => item.title).filter(Boolean) ?? [];
  const resultsList = responseModel.topItem?.items.map((item) => item.title).filter(Boolean) ?? [];

  return {
    packageName: packageSegments.join('.'),
    moduleName: config.moduleName,
    methodName: method.name,
    className,
    fullName: methodPascal,
    params: paramsList.length > 0 ? { value: paramsList, str: paramsList.map((item) => `"${item}"`).join(', ') } : undefined,
    results: resultsList.length > 0 ? { value: resultsList, str: resultsList.map((item) => `"${item}"`).join(', ') } : undefined,
    request: requestModel,
    response: responseModel
  };
}

interface KotlinBuildContext {
  methodPascal: string;
  interfaces: Record<string, ObjectDefinition>;
  classMap: Map<string, KotlinInnerClass>;
  visiting: Set<string>;
  scope: 'request' | 'response';
}

function buildKotlinModel(
  summary: TypeSummary | undefined,
  fallbackName: string,
  method: MethodDefinition,
  scope: 'request' | 'response'
): KotlinModelView {
  if (!summary) {
    return { clazz: [] };
  }
  const definition = ensureObjectDefinition(summary, fallbackName, method.interfaces);
  const classMap = new Map<string, KotlinInnerClass>();
  const context: KotlinBuildContext = {
    methodPascal: toPascalCase(method.name),
    interfaces: method.interfaces,
    classMap,
    visiting: new Set<string>(),
    scope
  };
  const topItem = convertObjectToKotlinView(definition, context, true, []);
  return {
    topItem,
    clazz: Array.from(classMap.values())
  };
}

function convertObjectToKotlinView(
  definition: ObjectDefinition,
  context: KotlinBuildContext,
  isRoot: boolean,
  path: string[]
): KotlinObjectView {
  const className = definition.name ?? 'AnonymousType';
  if (!isRoot) {
    const cached = context.classMap.get(className);
    if (cached) {
      return cached;
    }
    if (context.visiting.has(className)) {
      return { needCompanion: false, items: [] };
    }
  }

  context.visiting.add(className);
  const items = definition.fields.map((field, index) => ({
    ...convertFieldToKotlinView(field, context, [...path, field.name]),
    isLast: index === definition.fields.length - 1
  }));
  const needCompanion = items.some((item) => item.enum && item.enum.length > 0);
  context.visiting.delete(className);

  const result: KotlinObjectView = {
    needCompanion,
    items
  };

  if (!isRoot) {
    context.classMap.set(className, { name: className, ...result });
  }

  return result;
}

function convertFieldToKotlinView(field: FieldSummary, context: KotlinBuildContext, path: string[]): KotlinFieldView {
  const required = !field.optional;
  const defaultValue = formatDefaultValue(field.defaultValue);
  const nullable = !required && !defaultValue;
  const typeInfo = resolveKotlinType(field.type, context, path);
  const enumDetails = buildEnumDetails(field.type, context.methodPascal, path);

  return {
    title: field.name,
    field: toFieldName(field.name),
    className: typeInfo.typeName,
    required,
    nullable,
    annotation: typeInfo.annotation,
    default: defaultValue,
    enumType: enumDetails?.enumType,
    enum: enumDetails?.entries,
    explanation: context.scope === 'response' && typeInfo.typeName === 'Number'
      ? ' // you can\'t set a Number type to this param, only support Int/Double/Long/Float'
      : undefined,
    isLast: false
  };
}

interface KotlinTypeInfo {
  typeName: string;
  annotation?: string;
}

function resolveKotlinType(summary: TypeSummary, context: KotlinBuildContext, path: string[]): KotlinTypeInfo {
  switch (summary.kind) {
    case 'primitive':
      return { typeName: mapPrimitiveToKotlin(summary.name) };
    case 'reference':
      return resolveReferenceType(summary.name, context, path);
    case 'object':
      return resolveInlineObject(summary, context, path);
    case 'array':
      return resolveArrayType(summary.elementType, context, path);
    default:
      return { typeName: 'Any' };
  }
}

function resolveReferenceType(name: string, context: KotlinBuildContext, path: string[]): KotlinTypeInfo {
  const definition = context.interfaces[name];
  if (definition) {
    convertObjectToKotlinView(definition, context, false, path);
  }
  return {
    typeName: name,
    annotation: `nestedClassType = ${name}::class`
  };
}

function resolveInlineObject(summary: Extract<TypeSummary, { kind: 'object' }>, context: KotlinBuildContext, path: string[]): KotlinTypeInfo {
  const className = createInlineClassName(context.methodPascal, path);
  const definition: ObjectDefinition = { name: className, fields: summary.fields };
  convertObjectToKotlinView(definition, context, false, path);
  return {
    typeName: className,
    annotation: `nestedClassType = ${className}::class`
  };
}

function resolveArrayType(summary: TypeSummary, context: KotlinBuildContext, path: string[]): KotlinTypeInfo {
  const elementInfo = resolveKotlinType(summary, context, path);
  let annotation: string | undefined;
  if (summary.kind === 'primitive') {
    annotation = `primitiveClassType = ${elementInfo.typeName}::class`;
  } else if (summary.kind === 'reference' || summary.kind === 'object') {
    annotation = `nestedClassType = ${elementInfo.typeName}::class`;
  } else if (summary.kind === 'array') {
    annotation = 'primitiveClassType = List::class';
  }
  return {
    typeName: `List<${elementInfo.typeName}>`,
    annotation
  };
}

function buildEnumDetails(type: TypeSummary, methodPascal: string, path: string[]): { enumType: string; entries: KotlinEnumEntry[] } | undefined {
  const enumSource = extractEnumSource(type);
  if (!enumSource) {
    return undefined;
  }
  const enumType = enumSource.kind === 'string' ? 'IDLMethodStringEnum' : enumSource.kind === 'number' ? 'IDLMethodIntEnum' : undefined;
  if (!enumType) {
    return undefined;
  }
  return {
    enumType,
    entries: buildEnumConstants(enumSource.values, methodPascal, path)
  };
}

function extractEnumSource(type: TypeSummary): { kind: 'string' | 'number'; values: Array<string | number> } | undefined {
  if (type.kind === 'primitive' && type.enumValues && type.enumValues.length > 0) {
    if (type.name === 'string' || type.name === 'number') {
      return { kind: type.name, values: type.enumValues as Array<string | number> };
    }
  }
  if (type.kind === 'array') {
    return extractEnumSource(type.elementType);
  }
  return undefined;
}

function buildEnumConstants(values: Array<string | number>, methodPascal: string, path: string[]): KotlinEnumEntry[] {
  const base = `${methodPascal}${path.map((segment) => toPascalCase(segment)).join('')}` || methodPascal;
  const used = new Set<string>();
  return values.map((value, index) => {
    const suffix = enumSuffix(value);
    const nameCandidate = suffix ? `${base}${suffix}` : base;
    let candidate = nameCandidate;
    let counter = 1;
    while (used.has(candidate)) {
      candidate = `${nameCandidate}${counter}`;
      counter += 1;
    }
    used.add(candidate);
    const wrapperValue = typeof value === 'string' ? JSON.stringify(value) : String(value);
    return {
      wrapperName: candidate,
      wrapperValue,
      comma: index < values.length - 1
    };
  });
}

function enumSuffix(value: string | number): string {
  if (typeof value === 'number') {
    if (value === 0) {
      return '';
    }
    return value.toString().replace(/[^0-9]/g, '');
  }
  const cleaned = value.replace(/[^a-zA-Z0-9]+/g, ' ').trim();
  if (!cleaned) {
    return '';
  }
  return cleaned
    .split(/\s+/)
    .map((chunk) => chunk.charAt(0).toUpperCase() + chunk.slice(1).toLowerCase())
    .join('');
}

function mapPrimitiveToKotlin(kind: PrimitiveKind): string {
  switch (kind) {
    case 'string':
      return 'String';
    case 'number':
      return 'Number';
    case 'boolean':
      return 'Boolean';
    case 'void':
      return 'Unit';
    case 'object':
      return 'Map<String, Any>';
    default:
      return 'Any';
  }
}

function formatDefaultValue(value: DefaultValue | undefined): string | undefined {
  if (!value) {
    return undefined;
  }
  switch (value.kind) {
    case 'boolean':
      return `MethodParamDefaultValue(type = DefaultType.BOOL, boolValue = ${value.value})`;
    case 'number': {
      const num = value.value as number;
      if (Number.isInteger(num)) {
        if (num > 2147483647 || num < -2147483648) {
          return `MethodParamDefaultValue(type = DefaultType.LONG, longValue = ${num})`;
        }
        return `MethodParamDefaultValue(type = DefaultType.INT, intValue = ${num})`;
      }
      return `MethodParamDefaultValue(type = DefaultType.DOUBLE, doubleValue = ${num})`;
    }
    case 'string':
      return `MethodParamDefaultValue(type = DefaultType.STRING, stringValue = ${JSON.stringify(value.value)})`;
    default:
      return undefined;
  }
}

function createInlineClassName(methodPascal: string, path: string[]): string {
  const suffix = path.map((segment) => toPascalCase(segment)).join('');
  return `PipeBean${methodPascal}${suffix}`;
}

function toFieldName(name: string): string {
  if (/^[a-z][a-zA-Z0-9]*$/.test(name)) {
    return name;
  }
  const parts = splitIntoWords(name);
  if (parts.length === 0) {
    return name || 'field';
  }
  return parts[0].toLowerCase() + parts.slice(1).map((chunk) => chunk.charAt(0).toUpperCase() + chunk.slice(1).toLowerCase()).join('');
}

export async function writeKotlinFile(
  root: string,
  config: ModuleConfig,
  method: MethodDefinition,
  view: KotlinTemplateView,
  template: string
): Promise<void> {
  const packageSegments = buildPackageSegments(config, method.name);
  const kotlinDir = path.join(root, 'android', 'src', 'main', 'java', ...packageSegments);
  await fs.ensureDir(kotlinDir);
  const filePath = path.join(kotlinDir, `${view.className}.kt`);
  const rendered = Mustache.render(template, view);
  await fs.writeFile(filePath, rendered, 'utf8');
}
