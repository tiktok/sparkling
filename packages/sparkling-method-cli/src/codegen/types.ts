// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
export interface CodegenOptions {
  src?: string;
}

export interface ModuleConfig {
  packageName: string;
  moduleName: string;
}

export type PrimitiveKind = 'string' | 'number' | 'boolean' | 'void' | 'object' | 'any';

export interface DefaultValue {
  kind: 'string' | 'number' | 'boolean';
  value: string | number | boolean;
}

export type TypeSummary =
  | { kind: 'primitive'; name: PrimitiveKind; text?: string; enumValues?: Array<string | number | boolean> }
  | { kind: 'array'; elementType: TypeSummary }
  | { kind: 'reference'; name: string }
  | { kind: 'object'; fields: FieldSummary[] };

export interface FieldSummary {
  name: string;
  optional: boolean;
  description?: string;
  type: TypeSummary;
  defaultValue?: DefaultValue;
}

export interface ObjectDefinition {
  name?: string;
  description?: string;
  fields: FieldSummary[];
}

export interface MethodDefinition {
  name: string;
  description?: string;
  request?: TypeSummary;
  response?: TypeSummary;
  source: string;
  interfaces: Record<string, ObjectDefinition>;
}

export interface KotlinTemplateView {
  packageName: string;
  moduleName: string;
  methodName: string;
  className: string;
  fullName: string;
  params?: { value: string[]; str: string };
  results?: { value: string[]; str: string };
  request: KotlinModelView;
  response: KotlinModelView;
}

export interface KotlinModelView {
  topItem?: KotlinObjectView;
  clazz: KotlinInnerClass[];
}

export interface KotlinObjectView {
  needCompanion: boolean;
  items: KotlinFieldView[];
}

export interface KotlinInnerClass extends KotlinObjectView {
  name: string;
}

export interface KotlinFieldView {
  title: string;
  field: string;
  className: string;
  required: boolean;
  nullable: boolean;
  annotation?: string;
  default?: string;
  enumType?: string;
  enum?: KotlinEnumEntry[];
  explanation?: string;
  isLast: boolean;
}

export interface KotlinEnumEntry {
  wrapperName: string;
  wrapperValue: string;
  comma: boolean;
}

export interface SwiftFieldView {
  name: string;
  type: string;
  optional: boolean;
  comment?: string;
  defaultValue?: string;
  isLast: boolean;
}

export interface SwiftStructView {
  structName: string;
  objcName: string;
  hasFields: boolean;
  fields: SwiftFieldView[];
}

export interface SwiftTemplateView {
  moduleName: string;
  methodName: string;
  methodClassName: string;
  methodObjcName: string;
  request: SwiftStructView;
  response: SwiftStructView;
  extraTypes: SwiftStructView[];
}

export interface TypeScriptFieldView {
  name: string;
  type: string;
  optional: boolean;
  description?: string;
  defaultValue?: string;
  jsType?: string;
  isLast: boolean;
}

export interface TypeScriptInterfaceView {
  name: string;
  fields: TypeScriptFieldView[];
  hasFields: boolean;
  description?: string;
}

export interface TypeScriptValidationRule {
  field: string;
  type: 'null-check' | 'type-check' | 'range-check' | 'custom';
  jsType?: string;
  condition?: string;
  errorMessage: string;
  trimValue?: boolean;
  defaultValue?: string;
}

export interface TypeScriptTemplateView {
  moduleName: string;
  methodName: string;
  methodCamelCase: string;
  methodPascalCase: string;
  packageName: string;
  requestInterface: TypeScriptInterfaceView;
  responseInterface: TypeScriptInterfaceView;
  extraInterfaces: TypeScriptInterfaceView[];
  validationRules: TypeScriptValidationRule[];
  pipeCall: {
    methodString: string;
    params: string[];
  };
  hasRequestParams: boolean;
  hasResponseData: boolean;
}
