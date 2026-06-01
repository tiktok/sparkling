import { mkdirSync, existsSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(__dirname, '..');
const outputRoot = resolve(repoRoot, 'coverage-reports', 'ts');

const targets = [
  {
    name: 'create-sparkling-app',
    filter: 'create-sparkling-app',
    type: 'jest',
  },
  {
    name: 'sparkling-method-cli',
    filter: 'sparkling-method-cli',
    type: 'jest',
  },
  {
    name: 'sparkling-app-cli',
    filter: 'sparkling-app-cli',
    type: 'jest',
  },
  {
    name: 'sparkling-media',
    filter: 'sparkling-media',
    type: 'jest',
  },
  {
    name: 'sparkling-navigation',
    filter: 'sparkling-navigation',
    type: 'jest',
  },
  {
    name: 'sparkling-storage',
    filter: 'sparkling-storage',
    type: 'jest',
  },
  {
    name: 'sparkling-playground',
    filter: 'sparkling-playground',
    type: 'vitest',
  },
  {
    name: 'sparkling-app-template',
    filter: 'sparkling-app-template',
    type: 'vitest',
  },
];

if (existsSync(outputRoot)) {
  rmSync(outputRoot, { recursive: true, force: true });
}
mkdirSync(outputRoot, { recursive: true });

const failures = [];

const runCommand = (cmd, args) =>
  spawnSync(cmd, args, {
    cwd: repoRoot,
    stdio: 'inherit',
    env: process.env,
  });

console.log('\n[coverage:ts] Building shared TypeScript workspace deps');
const sharedPrebuild = runCommand('pnpm', ['--filter', 'sparkling-method', 'build']);
if (sharedPrebuild.status !== 0) {
  failures.push('sparkling-method (prebuild)');
}

for (const target of targets) {
  if (failures.length > 0) {
    break;
  }

  const outDir = join(outputRoot, target.name);
  mkdirSync(outDir, { recursive: true });

  if (target.name === 'sparkling-playground') {
    console.log('\n[coverage:ts] Building workspace deps for sparkling-playground');
    const prebuild = runCommand('pnpm', [
      '--filter',
      'sparkling-method',
      '--filter',
      'sparkling-navigation',
      '--filter',
      'sparkling-storage',
      '--filter',
      'sparkling-media',
      'build',
    ]);
    if (prebuild.status !== 0) {
      failures.push(`${target.name} (prebuild)`);
      continue;
    }
  }

  const args =
    target.type === 'jest'
      ? [
          '--filter',
          target.filter,
          'exec',
          'jest',
          '--coverage',
          '--coverageDirectory',
          outDir,
          '--coverageThreshold',
          '{}',
        ]
      : [
          '--filter',
          target.filter,
          'exec',
          'vitest',
          'run',
          '--passWithNoTests',
          '--coverage',
          '--coverage.include',
          'src/**/*.{ts,tsx}',
          '--coverage.reportsDirectory',
          outDir,
        ];

  console.log(`\n[coverage:ts] Running ${target.name} (${target.type})`);
  const run = runCommand('pnpm', args);

  if (run.status !== 0) {
    failures.push(target.name);
  }
}

if (failures.length > 0) {
  console.error(`\n[coverage:ts] Failed modules: ${failures.join(', ')}`);
  process.exit(1);
}

console.log(`\n[coverage:ts] Reports generated at: ${outputRoot}`);
