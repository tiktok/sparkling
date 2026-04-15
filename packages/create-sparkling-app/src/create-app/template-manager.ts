// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

import { DEFAULT_TEMPLATE_PACKAGE } from './constants';
import { ui } from '../ui';

export const sanitizeCacheKey = (packageName: string, version: string) => {
  const normalized = packageName.replace(/[\\/]/g, '_');
  const versionLabel = version || 'latest';
  return `${normalized}@${versionLabel}`;
};

const NPM_TEMPLATE_PREFIX = 'npm:';

const SAFE_GIT_REF = /^[a-zA-Z0-9._\-/]+$/;

function validateGitRef(value: string, label: string): void {
  if (!SAFE_GIT_REF.test(value)) {
    throw new Error(`Invalid characters in GitHub ${label}: ${value}`);
  }
}

export async function resolveCustomTemplate(templateInput: string, version?: string): Promise<string> {
  const trimmedInput = templateInput.trim();

  if (trimmedInput.startsWith(NPM_TEMPLATE_PREFIX)) {
    const packageName = trimmedInput.slice(NPM_TEMPLATE_PREFIX.length).trim();

    if (!packageName) {
      throw new Error('Invalid npm template specifier. Provide a package name after "npm:".');
    }

    return resolveNpmTemplate(packageName, version);
  }

  if (trimmedInput.startsWith('@') || (!trimmedInput.includes('/') && !trimmedInput.startsWith('http') && !trimmedInput.startsWith('.'))) {
    return resolveNpmTemplate(trimmedInput, version);
  }

  const githubMatch = trimmedInput.match(/^https?:\/\/github\.com\/([^\/]+)\/([^\/]+)(?:\/tree\/([^\/]+))?(?:\/(.*))?$/);

  if (githubMatch) {
    const [, owner, repo, branch = 'main', subPath = ''] = githubMatch;
    validateGitRef(owner, 'owner');
    validateGitRef(repo, 'repo');
    validateGitRef(branch, 'branch');
    const templateDir = path.join(process.cwd(), '.temp-templates', `${owner}-${repo}-${branch}`);

    try {
      if (fs.existsSync(templateDir)) {
        execFileSync('git', ['-C', templateDir, 'pull'], { stdio: 'pipe' });
      } else {
        execFileSync('git', ['clone', '--depth', '1', '--branch', branch, `https://github.com/${owner}/${repo}.git`, templateDir], { stdio: 'pipe' });
      }

      const fullPath = subPath ? path.join(templateDir, subPath) : templateDir;
      if (!fs.existsSync(fullPath)) {
        throw new Error(`Template path not found in repository: ${subPath || '/'}`);
      }

      return fullPath;
    } catch (error) {
      throw new Error(`Failed to fetch GitHub template: ${(error as Error).message}`);
    }
  }

  const localPath = path.resolve(trimmedInput);
  if (fs.existsSync(localPath)) {
    const stat = fs.statSync(localPath);
    if (stat.isDirectory()) {
      return localPath;
    }
    throw new Error(`Template path is not a directory: ${localPath}`);
  }

  throw new Error(`Template not found: ${templateInput}`);
}

export async function validateTemplate(templatePath: string): Promise<boolean> {
  try {
    const packageJsonPath = path.join(templatePath, 'package.json');
    const hasPackageJson = fs.existsSync(packageJsonPath);

    if (!hasPackageJson) {
      console.warn(ui.warn('Warning: Template does not contain package.json'));
    } else {
      try {
        const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
        if (!packageJson.name) {
          console.warn(ui.warn('Warning: Template package.json does not have a name field'));
        }
      } catch (error) {
        console.warn(ui.warn(`Warning: Template package.json is invalid: ${(error as Error).message}`));
      }
    }

    const files = fs.readdirSync(templatePath);
    if (files.length === 0) {
      throw new Error('Template directory is empty');
    }

    const hasSourceFiles = files.some(file =>
      file.endsWith('.js') ||
      file.endsWith('.ts') ||
      file.endsWith('.tsx') ||
      file.endsWith('.jsx') ||
      fs.statSync(path.join(templatePath, file)).isDirectory()
    );

    if (!hasSourceFiles && !hasPackageJson) {
      console.warn(ui.warn('Warning: Template does not contain any source files or package.json'));
    }

    return true;
  } catch (error) {
    console.error(ui.error(`Template validation failed: ${(error as Error).message}`));
    return false;
  }
}

export async function resolveDefaultTemplate(version?: string): Promise<string> {
  try {
    const tag = version ?? 'latest';
    const lowerTag = tag.toLowerCase();
    const forceLatest = !version || lowerTag === 'latest';
    return await resolveNpmTemplate(DEFAULT_TEMPLATE_PACKAGE, tag, { forceLatest });
  } catch (error) {
    // Do not fallback to repo-relative paths; ensure template is available via npm
    throw error;
  }
}

export async function resolveNpmTemplate(
  packageName: string,
  version?: string,
  options?: { forceLatest?: boolean },
): Promise<string> {
  const normalizedName = packageName.trim();

  if (!normalizedName) {
    throw new Error('Invalid npm template specifier. Provide a package name after "npm:".');
  }

  const versionSpecifier = version && version.trim() && version.trim().toLowerCase() !== 'latest'
    ? version.trim()
    : 'latest';
  const cacheKey = sanitizeCacheKey(normalizedName, versionSpecifier);
  const templateDir = path.join(process.cwd(), '.temp-templates', cacheKey);
  const installRoot = path.dirname(templateDir);
  const packagePath = path.join(installRoot, 'node_modules', normalizedName);
  const forceLatest = options?.forceLatest ?? versionSpecifier === 'latest';
  const skipInstall = process.env.SPK_TEMPLATE_SKIP_INSTALL === '1';
  const shouldReuseCache = !forceLatest && fs.existsSync(templateDir);

  try {
    if (shouldReuseCache) {
      console.log(ui.muted(`Using cached npm template: ${normalizedName}@${versionSpecifier}`));
      return templateDir;
    }

    fs.rmSync(templateDir, { recursive: true, force: true });
    if (forceLatest && !skipInstall) {
      fs.rmSync(packagePath, { recursive: true, force: true });
    }

    console.log(ui.headline(`Installing npm template: ${normalizedName}@${versionSpecifier}`));

    fs.mkdirSync(installRoot, { recursive: true });
    // Anchor an isolated package.json to prevent npm from traversing upward
    // and interacting with a parent project's workspaces/package.json.
    const anchorPkgJson = path.join(installRoot, 'package.json');
    if (!fs.existsSync(anchorPkgJson)) {
      try {
        const minimal = { name: 'sparkling-template-cache', private: true } as const;
        fs.writeFileSync(anchorPkgJson, `${JSON.stringify(minimal, null, 2)}\n`, 'utf8');
      } catch {
        // Non-fatal if writing fails; proceed regardless.
      }
    }

    // Install the npm template package. Keep stdio piped so we can surface
    // precise error output below if installation fails.
    if (!skipInstall) {
      try {
        execFileSync('npm', ['install', `${normalizedName}@${versionSpecifier}`, '--no-save', '--package-lock=false', '--no-audit', '--no-fund', '--silent'],
          {
            cwd: installRoot,
            stdio: 'pipe',
          });
      } catch (installErr: any) {
        const code = installErr?.status ?? installErr?.code;
        const stderr = typeof installErr?.stderr === 'string'
          ? installErr.stderr
          : installErr?.stderr?.toString?.() ?? '';
        const stdout = typeof installErr?.stdout === 'string'
          ? installErr.stdout
          : installErr?.stdout?.toString?.() ?? '';

        // Combine message and captured streams so users can see the real reason
        // (e.g., auth failure, network issues, registry not reachable, 404, etc.).
        const streams = [stderr, stdout].filter(Boolean).join('\n').trim();
        const extra = code !== undefined ? ` (exit code: ${code})` : '';
        const msg = [`npm install ${normalizedName} failed${extra}.`, streams].filter(Boolean).join('\n');

        throw new Error(msg);
      }
    }

    if (!fs.existsSync(packagePath)) {
      const reason = skipInstall
        ? 'Template package not found and installation skipped (SPK_TEMPLATE_SKIP_INSTALL=1)'
        : 'Failed to install npm package';
      throw new Error(`${reason}: ${normalizedName}@${versionSpecifier}`);
    }

    const possibleTemplatePaths = [
      path.join(packagePath, 'template'),
      path.join(packagePath, 'templates', 'app'),
      path.join(packagePath, 'templates', 'default'),
      packagePath,
    ];

    for (const pathCandidate of possibleTemplatePaths) {
      if (fs.existsSync(pathCandidate) && fs.statSync(pathCandidate).isDirectory()) {
        fs.cpSync(pathCandidate, templateDir, { recursive: true });
        return templateDir;
      }
    }

    throw new Error(`No valid template found in npm package: ${normalizedName}`);
  } catch (error) {
    if (fs.existsSync(templateDir)) {
      fs.rmSync(templateDir, { recursive: true, force: true });
    }

    const e = error as any;
    const code = e?.status ?? e?.code;
    const stderr = typeof e?.stderr === 'string' ? e.stderr : e?.stderr?.toString?.() ?? '';
    const stdout = typeof e?.stdout === 'string' ? e.stdout : e?.stdout?.toString?.() ?? '';
    const streams = [stderr, stdout].filter(Boolean).join('\n').trim();
    const extra = code !== undefined ? ` (exit code: ${code})` : '';
    const details = [e?.message, streams].filter(Boolean).join('\n');

    throw new Error(`Failed to resolve npm template${extra}: ${details}`);
  }
}
