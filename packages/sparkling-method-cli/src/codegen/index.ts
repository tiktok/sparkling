// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'path';
import fs from 'fs-extra';
import fg from 'fast-glob';

import { CodegenOptions, MethodDefinition, ModuleConfig } from './types';
import { parseDefinitionFile } from './definition-parser';
import { buildMetadataFileName, writeMetadataFile } from './metadata';
import { buildKotlinView, writeKotlinFile } from './kotlin';
import { buildSwiftView, writeSwiftFile } from './swift';
import { buildTypeScriptView, writeTypeScriptFiles } from './typescript';
import { ui } from '../ui';
import { isVerboseEnabled, verboseLog } from '../verbose';

const TEMPLATE_ROOT = path.resolve(__dirname, './template');

export async function runCodegen(options: CodegenOptions = {}): Promise<void> {
  const projectRoot = process.cwd();
  const config = await readModuleConfig(projectRoot);
  const srcDir = path.resolve(projectRoot, options.src ?? 'src');

  if (!await fs.pathExists(srcDir)) {
    throw new Error(`Source directory not found at ${srcDir}`);
  }

  const definitionFiles = await fg('**/*.d.ts', { cwd: srcDir, absolute: true });
  if (isVerboseEnabled()) {
    verboseLog(`Scanning for definitions in ${srcDir}; found ${definitionFiles.length} file(s).`);
  }
  if (definitionFiles.length === 0) {
    console.log(ui.error('No .d.ts files found under src; please add your method definitions.'));
    return;
  }

  const templates = await readTemplates();
  const methods: MethodDefinition[] = [];

  for (const file of definitionFiles) {
    const source = await fs.readFile(file, 'utf8');
    const parsed = parseDefinitionFile(file, source);
    methods.push(...parsed);
  }

  if (methods.length === 0) {
    console.log(ui.warn('No method definitions were discovered.'));
    return;
  }

  const metadataDir = path.join(projectRoot, 'generated', 'metadata');
  await fs.ensureDir(metadataDir);
  const metadataNameUsage: Record<string, number> = {};

  for (const method of methods) {
    const metaFile = buildMetadataFileName(method.name, metadataNameUsage);
    if (isVerboseEnabled()) {
      verboseLog(`Generating artifacts for method ${method.name} (metadata file ${metaFile})`);
    }
    await writeMetadataFile(metadataDir, metaFile, method, config, projectRoot);

    const kotlinView = buildKotlinView(method, config);
    const swiftView = buildSwiftView(method, config);
    const typeScriptView = buildTypeScriptView(method, config);

    await writeKotlinFile(projectRoot, config, method, kotlinView, templates.kotlin);
    await writeSwiftFile(projectRoot, config, method, swiftView, templates.swift);
    await writeTypeScriptFiles(projectRoot, config, method, typeScriptView, templates.typescript);

    console.log(ui.success(`Generated metadata, Kotlin, Swift, and TypeScript IDL for ${method.name}`));
  }

  await refreshDeclaredMethods(projectRoot, methods);
}

/**
 * Write the method names back into module.config.json.
 *
 * `sparkling autolink` builds the Android registration from the `methods` map -
 * it is the only place the method names exist by the time the app is built - so
 * a map that is not kept in step with the definitions means a method that was
 * generated, compiled and linked but never registered. Codegen has just parsed
 * the definitive list, so it writes it.
 *
 * Anything already described for a method is preserved; only the set of names
 * is authoritative here.
 */
async function refreshDeclaredMethods(root: string, methods: MethodDefinition[]): Promise<void> {
  const configPath = path.join(root, 'module.config.json');
  if (!await fs.pathExists(configPath)) {
    return;
  }
  const raw = await fs.readJson(configPath);
  const existing = (raw.methods && typeof raw.methods === 'object' && !Array.isArray(raw.methods))
    ? raw.methods as Record<string, unknown>
    : {};

  const declared: Record<string, unknown> = {};
  for (const method of methods) {
    declared[method.name] = existing[method.name] ?? { description: method.description ?? '' };
  }

  const unchanged = Object.keys(declared).length === Object.keys(existing).length
    && Object.keys(declared).every((name) => name in existing);
  if (unchanged) {
    return;
  }

  raw.methods = declared;
  await fs.writeJson(configPath, raw, { spaces: 2 });
  if (isVerboseEnabled()) {
    verboseLog(`module.config.json methods updated: ${Object.keys(declared).join(', ') || '(none)'}`);
  }
}

async function readModuleConfig(root: string): Promise<ModuleConfig> {
  const configPath = path.join(root, 'module.config.json');
  if (!await fs.pathExists(configPath)) {
    throw new Error('module.config.json not found. Please run `sparkling-method-cli init <name>` first.');
  }
  const config = await fs.readJson(configPath);
  if (!config.packageName || !config.moduleName) {
    throw new Error('module.config.json must include packageName and moduleName fields.');
  }
  return config as ModuleConfig;
}

async function readTemplates(): Promise<{
  kotlin: string;
  swift: string;
  typescript: { idl: string; impl: string; index: string }
}> {
  const kotlinPath = path.join(TEMPLATE_ROOT, 'android', 'kotlin-idl-template');
  const swiftPath = path.join(TEMPLATE_ROOT, 'ios', 'swift-idl-template');
  const tsIdlPath = path.join(TEMPLATE_ROOT, 'ts', 'ts-idl-template');
  const tsImplPath = path.join(TEMPLATE_ROOT, 'ts', 'ts-impl-template');
  const tsIndexPath = path.join(TEMPLATE_ROOT, 'ts', 'ts-index-template');

  const [kotlin, swift, tsIdl, tsImpl, tsIndex] = await Promise.all([
    fs.readFile(kotlinPath, 'utf8'),
    fs.readFile(swiftPath, 'utf8'),
    fs.readFile(tsIdlPath, 'utf8'),
    fs.readFile(tsImplPath, 'utf8'),
    fs.readFile(tsIndexPath, 'utf8')
  ]);

  return {
    kotlin,
    swift,
    typescript: {
      idl: tsIdl,
      impl: tsImpl,
      index: tsIndex
    }
  };
}
