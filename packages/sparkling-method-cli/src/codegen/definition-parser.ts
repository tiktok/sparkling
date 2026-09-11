// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import ts from 'typescript';

import {
  DefaultValue,
  FieldSummary,
  MethodDefinition,
  ObjectDefinition,
  PrimitiveKind,
  TypeSummary
} from './types';

export function parseDefinitionFile(filePath: string, sourceText: string): MethodDefinition[] {
  const sourceFile = ts.createSourceFile(filePath, sourceText, ts.ScriptTarget.Latest, true);
  const nodes = collectTypeNodes(sourceFile);
  const cache = new Map<string, ObjectDefinition>();
  const methods: MethodDefinition[] = [];

  sourceFile.forEachChild((node) => {
    if (ts.isFunctionDeclaration(node) && node.name) {
      const method = buildMethodDefinition(node, sourceFile, nodes, cache);
      methods.push(method);
    }
  });

  return methods;
}

function collectTypeNodes(sourceFile: ts.SourceFile): Map<string, ts.InterfaceDeclaration | ts.TypeAliasDeclaration> {
  const nodes = new Map<string, ts.InterfaceDeclaration | ts.TypeAliasDeclaration>();
  sourceFile.forEachChild((node) => {
    if ((ts.isInterfaceDeclaration(node) || ts.isTypeAliasDeclaration(node)) && node.name) {
      nodes.set(node.name.text, node);
    }
  });
  return nodes;
}

function buildMethodDefinition(
  declaration: ts.FunctionDeclaration,
  sourceFile: ts.SourceFile,
  typeNodes: Map<string, ts.InterfaceDeclaration | ts.TypeAliasDeclaration>,
  cache: Map<string, ObjectDefinition>
): MethodDefinition {
  const name = declaration.name?.text ?? 'method';
  const description = extractJsDoc(declaration);
  const callbackParam = declaration.parameters.find((param) => param.type && ts.isFunctionTypeNode(param.type));
  const inputParams = declaration.parameters.filter((param) => param !== callbackParam);

  const request = buildRequestType(inputParams, sourceFile, typeNodes, cache);
  const response = buildResponseType(callbackParam, declaration, sourceFile, typeNodes, cache);
  const interfaces = collectInterfacesForMethod([request, response], typeNodes, cache);

  return {
    name,
    description,
    request,
    response,
    source: sourceFile.fileName,
    interfaces
  };
}

function buildRequestType(
  parameters: readonly ts.ParameterDeclaration[],
  sourceFile: ts.SourceFile,
  typeNodes: Map<string, ts.InterfaceDeclaration | ts.TypeAliasDeclaration>,
  cache: Map<string, ObjectDefinition>
): TypeSummary | undefined {
  if (parameters.length === 0) {
    return undefined;
  }

  if (parameters.length === 1) {
    return convertTypeNode(parameters[0].type, sourceFile, typeNodes, cache, parameters[0]);
  }

  return {
    kind: 'object',
    fields: parameters.map((param) => ({
      name: param.name.getText(sourceFile),
      optional: Boolean(param.questionToken),
      description: extractJsDoc(param),
      defaultValue: extractDefaultValue(param),
      type: convertTypeNode(param.type, sourceFile, typeNodes, cache, param)
    }))
  };
}

function buildResponseType(
  callbackParam: ts.ParameterDeclaration | undefined,
  declaration: ts.FunctionDeclaration,
  sourceFile: ts.SourceFile,
  typeNodes: Map<string, ts.InterfaceDeclaration | ts.TypeAliasDeclaration>,
  cache: Map<string, ObjectDefinition>
): TypeSummary | undefined {
  if (callbackParam && callbackParam.type && ts.isFunctionTypeNode(callbackParam.type)) {
    const cbParam = callbackParam.type.parameters[0];
    return convertTypeNode(cbParam?.type, sourceFile, typeNodes, cache, cbParam);
  }

  if (declaration.type) {
    return convertTypeNode(declaration.type, sourceFile, typeNodes, cache, declaration);
  }
  return undefined;
}

function collectInterfacesForMethod(
  summaries: Array<TypeSummary | undefined>,
  typeNodes: Map<string, ts.InterfaceDeclaration | ts.TypeAliasDeclaration>,
  cache: Map<string, ObjectDefinition>
): Record<string, ObjectDefinition> {
  const pending: string[] = [];
  const names = new Set<string>();
  summaries.forEach((summary) => gatherInterfaceNames(summary, names));
  names.forEach((name) => pending.push(name));

  // An interface can reference further interfaces through its own fields, so
  // the walk continues until the set stops growing. Collecting only the names
  // the request and response mention directly left anything one level deeper
  // undeclared: the generated code still named the type and annotated it with
  // `nestedClassType = X::class`, but no `interface X` was ever emitted, so it
  // did not compile.
  const interfaces: Record<string, ObjectDefinition> = {};
  const visited = new Set<string>();
  while (pending.length > 0) {
    const name = pending.shift() as string;
    if (visited.has(name)) {
      continue;
    }
    visited.add(name);
    const definition = resolveInterface(name, typeNodes, cache);
    if (!definition) {
      continue;
    }
    interfaces[name] = definition;
    const nested = new Set<string>();
    definition.fields.forEach((field) => gatherInterfaceNames(field.type, nested));
    nested.forEach((nestedName) => {
      if (!visited.has(nestedName)) {
        pending.push(nestedName);
      }
    });
  }
  return interfaces;
}

function gatherInterfaceNames(summary: TypeSummary | undefined, target: Set<string>): void {
  if (!summary) {
    return;
  }
  if (summary.kind === 'reference') {
    target.add(summary.name);
    return;
  }
  if (summary.kind === 'array') {
    gatherInterfaceNames(summary.elementType, target);
    return;
  }
  if (summary.kind === 'object') {
    summary.fields.forEach((field) => gatherInterfaceNames(field.type, target));
  }
}

function convertTypeNode(
  node: ts.TypeNode | undefined,
  sourceFile: ts.SourceFile,
  typeNodes: Map<string, ts.InterfaceDeclaration | ts.TypeAliasDeclaration>,
  cache: Map<string, ObjectDefinition>,
  docSource?: ts.Node
): TypeSummary {
  if (!node) {
    return { kind: 'primitive', name: 'any' };
  }

  if (ts.isArrayTypeNode(node)) {
    return { kind: 'array', elementType: convertTypeNode(node.elementType, sourceFile, typeNodes, cache, docSource) };
  }

  if (ts.isTypeReferenceNode(node)) {
    const typeName = node.typeName.getText(sourceFile);
    if ((typeName === 'Array' || typeName === 'ReadonlyArray') && node.typeArguments?.length) {
      return { kind: 'array', elementType: convertTypeNode(node.typeArguments[0], sourceFile, typeNodes, cache, docSource) };
    }
    const referenced = typeNodes.get(typeName);
    if (referenced && ts.isTypeAliasDeclaration(referenced)) {
      const resolved = convertTypeNode(referenced.type, referenced.getSourceFile(), typeNodes, cache, referenced);
      if (resolved.kind !== 'object') {
        return resolved;
      }
    }
    resolveInterface(typeName, typeNodes, cache);
    return { kind: 'reference', name: typeName };
  }

  if (ts.isUnionTypeNode(node)) {
    const literalUnion = extractLiteralUnion(node, sourceFile);
    if (literalUnion) {
      return { kind: 'primitive', name: literalUnion.kind, enumValues: literalUnion.values };
    }
    return { kind: 'primitive', name: 'any', text: node.getText(sourceFile) };
  }

  if (ts.isLiteralTypeNode(node)) {
    const literal = extractLiteralValue(node.literal);
    if (literal !== undefined) {
      const kind = typeof literal === 'string' ? 'string' : typeof literal === 'number' ? 'number' : 'boolean';
      return { kind: 'primitive', name: kind, enumValues: [literal] };
    }
  }

  if (ts.isTypeLiteralNode(node)) {
    return {
      kind: 'object',
      fields: node.members
        .filter(ts.isPropertySignature)
        .map((member) => ({
          name: propertyNameToString(member.name, sourceFile),
          optional: Boolean(member.questionToken),
          description: extractJsDoc(member),
          defaultValue: extractDefaultValue(member),
          type: convertTypeNode(member.type, sourceFile, typeNodes, cache, member)
        }))
    };
  }

  switch (node.kind) {
    case ts.SyntaxKind.StringKeyword:
      return { kind: 'primitive', name: 'string' };
    case ts.SyntaxKind.NumberKeyword:
      return { kind: 'primitive', name: 'number' };
    case ts.SyntaxKind.BooleanKeyword:
      return { kind: 'primitive', name: 'boolean' };
    case ts.SyntaxKind.VoidKeyword:
    case ts.SyntaxKind.UndefinedKeyword:
      return { kind: 'primitive', name: 'void' };
    case ts.SyntaxKind.ObjectKeyword:
      return { kind: 'primitive', name: 'object' };
    case ts.SyntaxKind.AnyKeyword:
    case ts.SyntaxKind.UnknownKeyword:
    default:
      return { kind: 'primitive', name: 'any', text: docSource?.getText() };
  }
}

function extractLiteralUnion(node: ts.UnionTypeNode, sourceFile: ts.SourceFile): { kind: PrimitiveKind; values: Array<string | number> } | undefined {
  const values: Array<string | number> = [];
  let primitive: PrimitiveKind | undefined;
  for (const type of node.types) {
    const literal = extractLiteralFromNode(type, sourceFile);
    if (!literal) {
      return undefined;
    }
    if (!primitive) {
      primitive = literal.kind;
    } else if (primitive !== literal.kind) {
      return undefined;
    }
    values.push(literal.value);
  }
  if (!primitive || (primitive !== 'string' && primitive !== 'number')) {
    return undefined;
  }
  return { kind: primitive, values };
}

function extractLiteralFromNode(node: ts.TypeNode, sourceFile: ts.SourceFile): { kind: PrimitiveKind; value: string | number } | undefined {
  if (ts.isLiteralTypeNode(node)) {
    const literal = extractLiteralValue(node.literal);
    if (typeof literal === 'string') {
      return { kind: 'string', value: literal };
    }
    if (typeof literal === 'number') {
      return { kind: 'number', value: literal };
    }
  }
  if (ts.isParenthesizedTypeNode(node)) {
    return extractLiteralFromNode(node.type, sourceFile);
  }
  return undefined;
}

function extractLiteralValue(node: ts.LiteralTypeNode['literal']): string | number | boolean | undefined {
  if (ts.isStringLiteral(node)) {
    return node.text;
  }
  if (ts.isNumericLiteral(node)) {
    return Number(node.text);
  }
  if (node.kind === ts.SyntaxKind.TrueKeyword) {
    return true;
  }
  if (node.kind === ts.SyntaxKind.FalseKeyword) {
    return false;
  }
  return undefined;
}

function resolveInterface(
  name: string,
  typeNodes: Map<string, ts.InterfaceDeclaration | ts.TypeAliasDeclaration>,
  cache: Map<string, ObjectDefinition>,
  seen: Set<string> = new Set()
): ObjectDefinition | undefined {
  if (cache.has(name)) {
    return cache.get(name);
  }
  const node = typeNodes.get(name);
  if (!node || seen.has(name)) {
    return undefined;
  }
  seen.add(name);

  let fields: FieldSummary[] = [];
  if (ts.isInterfaceDeclaration(node)) {
    fields = node.members
      .filter(ts.isPropertySignature)
      .map((member) => ({
        name: propertyNameToString(member.name, node.getSourceFile()),
        optional: Boolean(member.questionToken),
        description: extractJsDoc(member),
        defaultValue: extractDefaultValue(member),
        type: convertTypeNode(member.type, node.getSourceFile(), typeNodes, cache, member)
      }));
  } else if (ts.isTypeAliasDeclaration(node)) {
    const aliasType = convertTypeNode(node.type, node.getSourceFile(), typeNodes, cache, node);
    if (aliasType.kind === 'object') {
      fields = aliasType.fields;
    }
  }

  const definition: ObjectDefinition = {
    name,
    description: extractJsDoc(node),
    fields
  };
  cache.set(name, definition);
  seen.delete(name);
  return definition;
}

function propertyNameToString(name: ts.PropertyName, sourceFile: ts.SourceFile): string {
  if (ts.isIdentifier(name) || ts.isStringLiteral(name) || ts.isNumericLiteral(name)) {
    return name.text.toString();
  }
  return name.getText(sourceFile);
}

function extractJsDoc(node: ts.Node): string | undefined {
  const jsDocNodes = (node as ts.Node & { jsDoc?: readonly ts.JSDoc[] }).jsDoc;
  if (!jsDocNodes || jsDocNodes.length === 0) {
    return undefined;
  }
  const comment = jsDocNodes
    .map((entry: ts.JSDoc) => normalizeJsDocComment(entry.comment))
    .join('\n')
    .trim();
  return comment || undefined;
}

function normalizeJsDocComment(
  comment: string | readonly (ts.JSDocText | ts.JSDocLink | ts.JSDocLinkCode | ts.JSDocLinkPlain)[] | undefined
): string {
  if (!comment) {
    return '';
  }
  if (typeof comment === 'string') {
    return comment;
  }
  return comment.map((part) => (typeof part === 'string' ? part : part.text)).join('');
}

function extractDefaultValue(node: ts.Node): DefaultValue | undefined {
  const tags = ts.getJSDocTags(node);
  const tag = tags.find((entry) => entry.tagName.getText() === 'default');
  if (!tag) {
    return undefined;
  }
  let comment: string | undefined;
  if (typeof tag.comment === 'string') {
    comment = tag.comment;
  } else if (tag.comment?.length) {
    comment = tag.comment.map((part) => part.text).join('');
  }
  if (!comment) {
    return undefined;
  }
  return parseDefaultLiteral(comment.trim());
}

function parseDefaultLiteral(value: string): DefaultValue | undefined {
  if (!value) {
    return undefined;
  }
  const trimmed = value.trim();
  if (trimmed === 'true' || trimmed === 'false') {
    return { kind: 'boolean', value: trimmed === 'true' };
  }
  if (!Number.isNaN(Number(trimmed))) {
    return { kind: 'number', value: Number(trimmed) };
  }
  if ((trimmed.startsWith('"') && trimmed.endsWith('"')) || (trimmed.startsWith('\'') && trimmed.endsWith('\''))) {
    return { kind: 'string', value: trimmed.slice(1, -1) };
  }
  return { kind: 'string', value: trimmed };
}

export function ensureObjectDefinition(
  summary: TypeSummary | undefined,
  fallbackName: string,
  interfaces: Record<string, ObjectDefinition>
): ObjectDefinition {
  if (!summary) {
    return { name: fallbackName, fields: [] };
  }
  if (summary.kind === 'object') {
    return { name: fallbackName, fields: summary.fields };
  }
  if (summary.kind === 'reference') {
    return interfaces[summary.name] ?? { name: summary.name, fields: [] };
  }
  return {
    name: fallbackName,
    fields: [
      {
        name: 'value',
        optional: false,
        type: summary,
        description: undefined
      }
    ]
  };
}

export function collectReferencedObjects(
  method: MethodDefinition,
  interfaces: Record<string, ObjectDefinition>,
  exclude: Set<string>
): ObjectDefinition[] {
  const seen = new Set<string>(exclude);
  const results: ObjectDefinition[] = [];

  const walk = (summary: TypeSummary | undefined): void => {
    if (!summary) {
      return;
    }
    if (summary.kind === 'array') {
      walk(summary.elementType);
      return;
    }
    if (summary.kind === 'reference') {
      if (seen.has(summary.name)) {
        return;
      }
      seen.add(summary.name);
      const def = interfaces[summary.name];
      if (def) {
        results.push(def);
        def.fields.forEach((field) => walk(field.type));
      }
      return;
    }
    if (summary.kind === 'object') {
      summary.fields.forEach((field) => walk(field.type));
    }
  };

  walk(method.request);
  walk(method.response);
  return results;
}
