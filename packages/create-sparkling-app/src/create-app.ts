/// <reference types="node" />
// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from "fs";
import path from "path";

import { ActionRunner } from "./core/actions/action-runner";
import { ProjectBuilder } from "./core/project-builder/project-builder";
import { formatProjectName } from "./core/project-builder/template";
import { defaultLogger } from "./logger";
import { readPackageJson } from "./utils/common";
import {
  applyPackageNamespace,
  deriveDefaultNamespace,
  isValidNamespace,
} from "./create-app/namespace";
import {
  CUSTOM_TEMPLATE_OPTION,
  DEFAULT_TEMPLATE_NAME,
  lookupBuiltinTemplate,
} from "./create-app/constants";
import {
  applyAndroidDslChoice,
  type AndroidDslChoice,
} from "./create-app/android-dsl";
import {
  detectPackageManager,
  installDependencies,
  initializeGitRepo,
  showCompletionNotes,
} from "./create-app/post-create";
import { isDirEmpty } from "./create-app/fs-utils";
import { ui } from "./ui";
import {
  resolveCustomTemplate,
  resolveDefaultTemplate,
  validateTemplate,
} from "./create-app/template-manager";
import type { CreateSparklingAppOptions } from "./create-app/types";
import {
  askAdditionalTools,
  askAndroidDsl,
  askCustomTemplatePath,
  askDevTools,
  askNamespace,
  askProjectName,
  askTemplate,
  askWebPlatform,
  confirmInitGit,
  confirmInstall,
  confirmRemoveExistingDir,
} from "./create-app/user-prompts";
import { enableVerboseLogging, isVerboseEnabled, verboseLog } from "./utils/verbose";

export type {
  CreateAppArgs,
  CreateAppFlags,
  CreateSparklingAppOptions,
} from "./create-app/types";

function ensureExecutable(filePath: string): void {
  try {
    if (fs.existsSync(filePath)) {
      const stat = fs.statSync(filePath);
      const executableMode = stat.mode | 0o111;
      if (stat.mode !== executableMode) {
        fs.chmodSync(filePath, executableMode);
      }
    }
  } catch (error) {
    console.warn(
      `Warning: failed to set executable on ${filePath}: ${(error as Error).message}`
    );
  }
}

function removeWebSupport(projectDir: string): void {
  // Remove web-related scripts and dependencies from package.json
  const packageJsonPath = path.join(projectDir, "package.json");
  if (fs.existsSync(packageJsonPath)) {
    const pkg = JSON.parse(fs.readFileSync(packageJsonPath, "utf8")) as Record<string, unknown>;

    const scripts = (pkg.scripts ?? {}) as Record<string, string>;
    delete scripts["dev:web"];
    delete scripts["build:web"];
    delete scripts["run:web"];

    const devDeps = (pkg.devDependencies ?? {}) as Record<string, string>;
    delete devDeps["sparkling-web-shell"];
    delete devDeps["@lynx-js/web-core"];
    delete devDeps["@lynx-js/web-elements"];

    fs.writeFileSync(packageJsonPath, `${JSON.stringify(pkg, null, 2)}\n`);
  }

  // Remove environments config from app.config.ts, restoring flat assetPrefix
  const appConfigPath = path.join(projectDir, "app.config.ts");
  if (fs.existsSync(appConfigPath)) {
    let content = fs.readFileSync(appConfigPath, "utf8");
    // Remove the environments block and restore assetPrefix at top level
    content = content.replace(
      /\s*environments:\s*\{[\s\S]*?\n\s*\},\n/,
      "\n"
    );
    // Add assetPrefix back to output if not present
    if (!content.includes("assetPrefix")) {
      content = content.replace(
        /output:\s*\{/,
        "output: {\n    assetPrefix: 'asset:///',",
      );
    }
    fs.writeFileSync(appConfigPath, content);
  }
}

function toPascalCase(input: string): string {
  const base = input.includes("/") ? (input.split("/").pop() ?? input) : input;
  return base
    .split(/[^\p{L}\p{N}]+/u)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join("");
}

export async function createSparklingApp(
  options: CreateSparklingAppOptions
): Promise<void> {
  const cwd = options.cwd ?? process.cwd();
  const args = options.args ?? {};
  const flags = options.flags ?? {};

  if (flags.verbose) {
    enableVerboseLogging(true);
  }

  if (isVerboseEnabled()) {
    verboseLog(`cwd: ${cwd}`);
    verboseLog(`project arg: ${args.name ?? "(prompted)"}`);
    verboseLog(`flags: ${JSON.stringify(flags)}`);
  }

  const pkgRoot = path.resolve(__dirname, "..");
  const { version } = await readPackageJson(pkgRoot);

  const isWorkspaceBuild = fs.existsSync(
    path.resolve(pkgRoot, "..", "..", "pnpm-workspace.yaml")
  );
  const versionLabel = version
    ? `${version}${isWorkspaceBuild ? "-local" : ""}`
    : "0.0.0";

  console.log(ui.headline(`create-sparkling-app  v${versionLabel}`));
  console.log(ui.headline("┌──────────────────────────────────────────────────────────────┐"));
  console.log(ui.headline("│ Welcome to Sparkling!                                        │"));
  console.log(ui.headline("│ Create a Lynx app with various native abilities in minutes.  │"));
  console.log(ui.headline("└──────────────────────────────────────────────────────────────┘"));

  const projectName = args.name ?? (await askProjectName(flags));

  const formatted = formatProjectName(projectName);
  const { packageName, targetDir } = formatted;
  const distFolder = path.isAbsolute(targetDir)
    ? targetDir
    : path.join(cwd, targetDir);

  console.log(ui.muted(`  ↳ Will create in ${targetDir}`));
  if (isVerboseEnabled()) {
    verboseLog(`Absolute target directory: ${distFolder}`);
  }

  if (fs.existsSync(distFolder) && !isDirEmpty(distFolder)) {
    if (flags.force || flags.yes) {
      fs.rmSync(distFolder, { recursive: true, force: true });
    } else {
      const shouldEmpty = await confirmRemoveExistingDir(targetDir, flags);

      if (shouldEmpty) {
        fs.rmSync(distFolder, { recursive: true, force: true });
      } else {
        return;
      }
    }
  }

  let template = await askTemplate(flags, flags.template);
  const templateFlagProvided = flags.template !== undefined;
  let isCustomTemplate = false;
  let customTemplatePath: string | null = null;
  let selectedTemplateName = DEFAULT_TEMPLATE_NAME;
  const userProvidedTemplateVersion = flags.templateVersion;
  let templateVersion = userProvidedTemplateVersion;

  const builtinTemplate = lookupBuiltinTemplate(template);
  if (isVerboseEnabled()) {
    verboseLog(`template flag: ${template} (builtin: ${builtinTemplate ? "yes" : "no"})`);
  }

  if (template === CUSTOM_TEMPLATE_OPTION) {
    if (flags.yes) {
      throw new Error(
        "Custom template requires interactive input or a direct --template path."
      );
    }

    customTemplatePath = await askCustomTemplatePath(flags);
    isCustomTemplate = true;
  } else if (!builtinTemplate) {
    customTemplatePath = template;
    isCustomTemplate = true;
  } else {
    selectedTemplateName = builtinTemplate.canonicalName;
  }

  if (customTemplatePath && isVerboseEnabled()) {
    verboseLog(`custom template path: ${customTemplatePath}`);
  }

  if (templateVersion?.toLowerCase() === "latest" || templateVersion === "") {
    templateVersion = undefined;
  }

  if (builtinTemplate?.canonicalName === DEFAULT_TEMPLATE_NAME) {
    // Always use the latest template from npm unless the user explicitly
    // provides a specific version via --template-version.
    if (!userProvidedTemplateVersion) {
      templateVersion = undefined; // resolves to "latest" in resolveDefaultTemplate
    }
  }

  const androidDsl: AndroidDslChoice = await askAndroidDsl(flags);

  const devTools = await askDevTools(flags);

  const additionalTools = await askAdditionalTools(flags);

  const enableWeb = await askWebPlatform(flags);

  const defaultNamespace = deriveDefaultNamespace(packageName);
  const packageNamespace = await askNamespace(defaultNamespace, flags);

  if (!isValidNamespace(packageNamespace)) {
    throw new Error(
      "Invalid package namespace. Use reverse-DNS format, e.g. com.example.app"
    );
  }

  const enableESLintPrettier =
    additionalTools.includes("eslint") || additionalTools.includes("prettier");

  const packageManager = flags.pm ?? detectPackageManager();
  if (isVerboseEnabled()) {
    verboseLog(`using package manager: ${packageManager}`);
  }

  const shouldInitGit = await confirmInitGit(flags);

  const builder = ProjectBuilder.create({
    checkEmpty: false,
    packageName,
    targetDir: distFolder,
    version: version ?? "0.0.0",
    override: flags.force || flags.yes,
  });

  let templateFolder: string;

  if (isCustomTemplate && customTemplatePath) {
    console.log(ui.headline(`Resolving custom template: ${customTemplatePath}`));
    templateFolder = await resolveCustomTemplate(
      customTemplatePath,
      templateVersion
    );

    const isValid = await validateTemplate(templateFolder);
    if (!isValid) {
      throw new Error("Template validation failed");
    }

    console.log(ui.success(`✔ Using custom template: ${templateFolder}`));
  } else {
    if (!builtinTemplate) {
      throw new Error(`Unknown template "${template}".`);
    }

    if (builtinTemplate.canonicalName === DEFAULT_TEMPLATE_NAME) {
      templateFolder = await resolveDefaultTemplate(templateVersion);
    } else {
      const builtinPath = builtinTemplate.path;

      if (!builtinPath || !fs.existsSync(builtinPath)) {
        throw new Error(
          `Built-in template not found${builtinPath ? ` at ${builtinPath}` : ""}.`
        );
      }

      templateFolder = builtinPath;
    }
  }

  builder.addStep({
    from: templateFolder,
    variables: {
      // Record raw app name (without npm scope) and its PascalCase form for platform display names
      appName: packageName.includes("/")
        ? packageName.split("/").pop()!
        : packageName,
      appNameCamel: toPascalCase(packageName),
      version: version ?? "0.0.0",
      enableESLint: enableESLintPrettier.toString(),
      enablePrettier: enableESLintPrettier.toString(),
    },
    skipFiles: ["LICENSE"],
    async postHook(config) {
      applyAndroidDslChoice({
        allowGroovyOverrides: selectedTemplateName === DEFAULT_TEMPLATE_NAME,
        projectDir: config.targetDir,
        selection: androidDsl,
      });
      applyPackageNamespace(config.targetDir, packageNamespace);
      ensureExecutable(path.join(config.targetDir, "android", "gradlew"));
      if (!enableWeb) {
        removeWebSupport(config.targetDir);
      }
    },
  });

  builder.addStep({
    async postHook(config) {
      const packageJsonPath = path.resolve(config.targetDir, "package.json");
      if (fs.existsSync(packageJsonPath)) {
        const packageJson = JSON.parse(
          fs.readFileSync(packageJsonPath, "utf8")
        ) as Record<string, unknown>;

        packageJson.version = "1.0.0";

        if (devTools.includes("testing")) {
          const scripts = (packageJson.scripts ?? {}) as Record<string, string>;
          packageJson.scripts = scripts;
          scripts.test = scripts.test ?? "jest";
        }

        fs.writeFileSync(
          packageJsonPath,
          `${JSON.stringify(packageJson, null, 2)}\n`
        );
      }
    },
  });

  const actionContext = {
    devMode: false,
    environment: "development" as const,
    logger: defaultLogger,
    projectRoot: cwd,
  };

  const runner = new ActionRunner(actionContext);
  const projectAction = builder.toSingleAction(
    "create-app-project",
    `Create app project '${packageName}'`
  );

  runner.addAction(projectAction);
  await runner.run();

  // Prompt for JS dependency install after scaffolding completes
  const shouldInstall = await confirmInstall(packageManager, flags);
  let didInstall = false;
  if (shouldInstall && packageManager) {
    didInstall = await installDependencies(distFolder, packageManager);
  }

  if (shouldInitGit) {
    console.log(ui.info('Setting up git repository, this may take a moment...'));
    await initializeGitRepo(distFolder);
  }

  showCompletionNotes(targetDir, packageManager, didInstall, enableWeb);
}
