#!/usr/bin/env node
/**
 * Monorepo release helper:
 * - check/fix that all workspace packages share the same `version`
 * - verify publishing is done to public npm (npmjs.org) with an expected npm account
 * - publish non-private packages in dependency order
 *
 * No external deps (works with Node >= 18).
 */

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { spawnSync } from "node:child_process";

const NPMJS_REGISTRY = "https://registry.npmjs.org/";
const DEFAULT_RC_IDENT = "rc";
const NPM_CMD_TIMEOUT_MS = 15_000;

function usage() {
  return [
    "Usage:",
    "  node scripts/release-unify-and-publish.mjs <check|fix|publish> [options]",
    "",
    "Options:",
    "  --version <x.y.z>           Target version to enforce (defaults to root package.json version)",
    "  --expected-npm-user <name>  Optional safety check; if provided, `npm whoami` must match",
    "  --release                   Publish a release build (dist-tag defaults to latest)",
    "  --tag <dist-tag>            npm dist-tag for publish (default: rc, unless --release)",
    "  --no-auto-rc                For rc publishes, do not auto-generate/increment -rc.N (requires --version to already be an rc prerelease)",
    "  --skip <names>              Skip publishing certain packages (comma-separated list of package names)",
    "  --publish-last <names>      Publish these packages last, after all others (comma-separated list of package names)",
    "  --registry <url>            npm registry to use (default: https://registry.npmjs.org/)",
    "  --dry-run                   Do everything except the actual publish",
    "  --no-git-checks             Skip pnpm git checks during publish (default: enabled)",
    "  --git-checks                Enable pnpm git checks during publish",
    "  --include-private           Also attempt to publish private packages (default: false)",
    "",
    "Env var equivalents (useful for pnpm scripts to avoid `--` arg forwarding):",
    "  SPARKLING_VERSION, SPARKLING_EXPECTED_NPM_USER, SPARKLING_RELEASE=1, SPARKLING_TAG,",
    "  SPARKLING_SKIP, SPARKLING_PUBLISH_LAST, SPARKLING_REGISTRY, SPARKLING_DRY_RUN=1,",
    "  SPARKLING_GIT_CHECKS=1, SPARKLING_INCLUDE_PRIVATE=1, SPARKLING_NO_AUTO_RC=1",
    "",
    "Examples:",
    "  pnpm release:check",
    "  pnpm release:fix --version 2.0.0",
    `  pnpm release:publish  --version 2.0.0 --expected-npm-user your_npm_user  # recommended safety check`,
    "  pnpm release:publish  --version 2.0.0 --release",
    "  SPARKLING_VERSION=2.0.0-rc.0 pnpm release:publish",
  ].join("\n");
}

function fail(msg) {
  process.stderr.write(`${msg}\n`);
  process.exit(1);
}

function parseArgs(argv) {
  const args = {
    command: null,
    version: null,
    expectedNpmUser: null,
    tag: null,
    release: false,
    autoRc: true,
    skip: [],
    publishLast: [],
    registry: NPMJS_REGISTRY,
    dryRun: false,
    noGitChecks: true,
    includePrivate: false,
    trustedPublishing: false,
  };

  const positional = [];
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    // Some runners (or wrapper scripts) may forward the `--` separator as a literal arg.
    // Treat it as a no-op separator for robustness.
    if (a === "--") continue;
    if (!a.startsWith("--")) {
      positional.push(a);
      continue;
    }
    switch (a) {
      case "--version":
        args.version = argv[++i];
        break;
      case "--expected-npm-user":
        args.expectedNpmUser = argv[++i];
        break;
      case "--release":
        args.release = true;
        break;
      case "--tag":
        args.tag = argv[++i];
        break;
      case "--no-auto-rc":
        args.autoRc = false;
        break;
      case "--skip": {
        const raw = argv[++i] ?? "";
        const names = raw
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean);
        args.skip.push(...names);
        break;
      }
      case "--publish-last": {
        const raw = argv[++i] ?? "";
        const names = raw
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean);
        args.publishLast.push(...names);
        break;
      }
      case "--registry":
        args.registry = argv[++i];
        break;
      case "--dry-run":
        args.dryRun = true;
        break;
      case "--no-git-checks":
        args.noGitChecks = true;
        break;
      case "--git-checks":
        args.noGitChecks = false;
        break;
      case "--include-private":
        args.includePrivate = true;
        break;
      case "--trusted-publishing":
        args.trustedPublishing = true;
        break;
      default:
        fail(`Unknown flag: ${a}\n\n${usage()}`);
    }
  }

  args.command = positional[0] ?? null;

  // Env var fallbacks so `pnpm run <script>` can be used without `-- <args>`.
  // CLI flags always win over env vars.
  const env = process.env;
  if (!args.version && env.SPARKLING_VERSION) args.version = env.SPARKLING_VERSION;
  if (!args.expectedNpmUser && env.SPARKLING_EXPECTED_NPM_USER) {
    args.expectedNpmUser = env.SPARKLING_EXPECTED_NPM_USER;
  }
  if (!args.tag && env.SPARKLING_TAG) args.tag = env.SPARKLING_TAG;
  if (!args.registry && env.SPARKLING_REGISTRY) args.registry = env.SPARKLING_REGISTRY;

  if (!args.release && (env.SPARKLING_RELEASE === "1" || env.SPARKLING_RELEASE === "true")) {
    args.release = true;
  }
  if (!args.dryRun && (env.SPARKLING_DRY_RUN === "1" || env.SPARKLING_DRY_RUN === "true")) {
    args.dryRun = true;
  }
  if (env.SPARKLING_GIT_CHECKS === "1" || env.SPARKLING_GIT_CHECKS === "true") {
    args.noGitChecks = false;
  }
  if (env.SPARKLING_INCLUDE_PRIVATE === "1" || env.SPARKLING_INCLUDE_PRIVATE === "true") {
    args.includePrivate = true;
  }
  if (env.SPARKLING_NO_AUTO_RC === "1" || env.SPARKLING_NO_AUTO_RC === "true") {
    args.autoRc = false;
  }
  if (!args.trustedPublishing && (env.SPARKLING_TRUSTED_PUBLISHING === "1" || env.SPARKLING_TRUSTED_PUBLISHING === "true")) {
    args.trustedPublishing = true;
  }
  if (args.skip.length === 0 && env.SPARKLING_SKIP) {
    args.skip = env.SPARKLING_SKIP.split(",").map((s) => s.trim()).filter(Boolean);
  }
  if (args.publishLast.length === 0 && env.SPARKLING_PUBLISH_LAST) {
    args.publishLast = env.SPARKLING_PUBLISH_LAST.split(",").map((s) => s.trim()).filter(Boolean);
  }

  return args;
}

async function readJson(jsonPath) {
  const raw = await fs.readFile(jsonPath, "utf8");
  return JSON.parse(raw);
}

async function writeJson(jsonPath, obj) {
  const raw = JSON.stringify(obj, null, 2) + "\n";
  await fs.writeFile(jsonPath, raw, "utf8");
}

async function pathExists(p) {
  try {
    await fs.stat(p);
    return true;
  } catch {
    return false;
  }
}

async function listDirs(parentDir) {
  const entries = await fs.readdir(parentDir, { withFileTypes: true });
  return entries.filter((e) => e.isDirectory()).map((e) => path.join(parentDir, e.name));
}

async function getWorkspacePackageDirs(repoRoot) {
  // Keep this aligned with `pnpm-workspace.yaml` without having to parse YAML.
  const dirs = [];
  const packagesDir = path.join(repoRoot, "packages");
  const templateDir = path.join(repoRoot, "template");

  if (await pathExists(packagesDir)) {
    for (const d of await listDirs(packagesDir)) {
      const base = path.basename(d);
      if (base === "website") continue;
      dirs.push(d);
    }
    const methodsDir = path.join(packagesDir, "methods");
    if (await pathExists(methodsDir)) {
      for (const d of await listDirs(methodsDir)) dirs.push(d);
    }
  }

  if (await pathExists(templateDir)) {
    for (const d of await listDirs(templateDir)) dirs.push(d);
  }

  // Filter for directories that contain a package.json, and exclude test packages.
  const out = [];
  for (const d of dirs) {
    if (d.split(path.sep).includes("test")) continue;
    const pj = path.join(d, "package.json");
    if (await pathExists(pj)) out.push(d);
  }
  out.sort();
  return out;
}

function run(cmd, args, opts) {
  const res = spawnSync(cmd, args, { stdio: "inherit", ...opts });
  if (res.error) throw res.error;
  if (res.status !== 0) {
    const joined = [cmd, ...args].join(" ");
    fail(`Command failed (${res.status}): ${joined}`);
  }
}

function runCapture(cmd, args, opts) {
  const res = spawnSync(cmd, args, { encoding: "utf8", ...opts });
  if (res.error) {
    return {
      ok: false,
      stdout: res.stdout ?? "",
      stderr: res.stderr ?? "",
      error: res.error,
    };
  }
  if (res.status !== 0) {
    return { ok: false, stdout: res.stdout ?? "", stderr: res.stderr ?? "" };
  }
  return { ok: true, stdout: res.stdout ?? "", stderr: res.stderr ?? "" };
}

function topoSortPackages(packages, getDeps) {
  const byName = new Map(packages.map((p) => [p.name, p]));
  const inDeg = new Map(packages.map((p) => [p.name, 0]));
  const adj = new Map(packages.map((p) => [p.name, []]));

  for (const p of packages) {
    for (const depName of getDeps(p)) {
      if (!byName.has(depName)) continue;
      adj.get(depName).push(p.name);
      inDeg.set(p.name, inDeg.get(p.name) + 1);
    }
  }

  const queue = [];
  for (const [name, deg] of inDeg.entries()) {
    if (deg === 0) queue.push(name);
  }
  queue.sort();

  const out = [];
  while (queue.length) {
    const name = queue.shift();
    out.push(byName.get(name));
    for (const nxt of adj.get(name)) {
      inDeg.set(nxt, inDeg.get(nxt) - 1);
      if (inDeg.get(nxt) === 0) {
        queue.push(nxt);
        queue.sort();
      }
    }
  }

  if (out.length !== packages.length) {
    const remaining = packages.map((p) => p.name).filter((n) => !out.some((p) => p.name === n));
    fail(`Dependency cycle detected among publishable packages: ${remaining.join(", ")}`);
  }
  return out;
}

async function loadWorkspacePackages(repoRoot) {
  const dirs = await getWorkspacePackageDirs(repoRoot);
  const pkgs = [];
  for (const dir of dirs) {
    const jsonPath = path.join(dir, "package.json");
    const json = await readJson(jsonPath);
    if (!json.name || !json.version) {
      fail(`Invalid package.json (missing name/version): ${path.relative(repoRoot, jsonPath)}`);
    }
    pkgs.push({
      dir,
      jsonPath,
      name: json.name,
      version: json.version,
      private: Boolean(json.private),
      json,
    });
  }
  return pkgs;
}

async function checkVersions(pkgs) {
  const versions = new Map();
  for (const p of pkgs) {
    if (!versions.has(p.version)) versions.set(p.version, []);
    versions.get(p.version).push(p.name);
  }
  if (versions.size === 1) return { ok: true, versions };
  return { ok: false, versions };
}

async function setAllVersions(repoRoot, pkgs, targetVersion) {
  for (const p of pkgs) {
    if (p.json.version !== targetVersion) {
      p.json.version = targetVersion;
      await writeJson(p.jsonPath, p.json);
    }
  }
  const rootPackageJsonPath = path.join(repoRoot, "package.json");
  const rootJson = await readJson(rootPackageJsonPath);
  if (rootJson.version && rootJson.version !== targetVersion) {
    rootJson.version = targetVersion;
    await writeJson(rootPackageJsonPath, rootJson);
  }
}

function internalDeps(pkgJson, internalNames) {
  const deps = new Set();
  for (const field of ["dependencies", "optionalDependencies"]) {
    const obj = pkgJson[field];
    if (!obj) continue;
    for (const name of Object.keys(obj)) {
      if (internalNames.has(name)) deps.add(name);
    }
  }
  return [...deps].sort();
}

function parseVersionsJsonMaybe(stdout) {
  const s = String(stdout ?? "").trim();
  if (!s) return [];
  try {
    const v = JSON.parse(s);
    if (Array.isArray(v)) return v.map(String);
    if (typeof v === "string") return [v];
    return [];
  } catch {
    // Best-effort fallback for odd npm output.
    if (s.startsWith("[") || s.startsWith("{")) return [];
    return [s];
  }
}

function npmPackageVersionExists({ name, version, registry }) {
  const res = runCapture("npm", ["view", `${name}@${version}`, "version", "--registry", registry], {
    stdio: "pipe",
    timeout: NPM_CMD_TIMEOUT_MS,
  });
  if (!res.ok) return false;
  return res.stdout.split(/\s+/).some((token) => token.trim() === version);
}

function nextRcVersion(baseVersion, publishedVersions) {
  // Accept only `x.y.z` base versions here (no prerelease).
  if (baseVersion.includes("-")) return null;
  const prefix = `${baseVersion}-${DEFAULT_RC_IDENT}.`;
  let maxN = -1;
  for (const v of publishedVersions) {
    if (!v.startsWith(prefix)) continue;
    const rest = v.slice(prefix.length);
    if (!/^[0-9]+$/.test(rest)) continue;
    const n = Number(rest);
    if (Number.isFinite(n)) maxN = Math.max(maxN, n);
  }
  return `${baseVersion}-${DEFAULT_RC_IDENT}.${maxN + 1}`;
}

async function computeNextRcVersion({ baseVersion, registry, candidatePackageNames }) {
  // Query npm for versions. `npm view` does not require auth for public packages.
  let best = null;
  for (const name of candidatePackageNames) {
    const res = runCapture("npm", ["view", name, "versions", "--json", "--registry", registry], {
      stdio: "pipe",
      timeout: NPM_CMD_TIMEOUT_MS,
    });
    if (!res.ok && res.error) {
      // If we can't query the registry at all, don't burn time retrying other packages.
      const code = res.error.code;
      const msg = String(res.error.message ?? "");
      const isNetworkish =
        code === "ETIMEDOUT" ||
        code === "ENOTFOUND" ||
        code === "EAI_AGAIN" ||
        code === "ECONNREFUSED" ||
        msg.toLowerCase().includes("timed out");
      if (isNetworkish) return `${baseVersion}-${DEFAULT_RC_IDENT}.0`;
    }
    if (!res.ok) continue; // package may not exist yet; try next
    const versions = parseVersionsJsonMaybe(res.stdout);
    const next = nextRcVersion(baseVersion, versions);
    if (!next) continue;
    if (!best) {
      best = next;
      continue;
    }
    const a = Number(best.split(`${DEFAULT_RC_IDENT}.`)[1]);
    const b = Number(next.split(`${DEFAULT_RC_IDENT}.`)[1]);
    if (Number.isFinite(a) && Number.isFinite(b) && b > a) best = next;
  }
  return best ?? `${baseVersion}-${DEFAULT_RC_IDENT}.0`;
}

async function resolvePublishVersionAndTag({ args, rootVersion, registry, publishablePackageNames }) {
  const requested = args.version ?? rootVersion;
  if (!requested) fail("Unable to determine version. Pass --version <x.y.z>.");

  const distTag = args.tag ?? (args.release ? "latest" : DEFAULT_RC_IDENT);

  if (args.release) {
    if (requested.includes("-")) {
      fail(
        `Refusing to publish prerelease version with --release: ${requested}\n` +
          "Remove the prerelease suffix (e.g. publish 2.0.1) or omit --release."
      );
    }
    return { targetVersion: requested, distTag };
  }

  // rc flow
  if (requested.includes("-")) {
    // When user pins an explicit rc version, treat it as an exact target; do not auto-bump.
    if (!requested.includes(`-${DEFAULT_RC_IDENT}.`)) {
      fail(
        `Refusing to publish non-rc prerelease by default: ${requested}\n` +
          `Use an rc version like 2.0.1-${DEFAULT_RC_IDENT}.0, or pass a plain base version like 2.0.1 to auto-generate.`
      );
    }
    return { targetVersion: requested, distTag };
  }

  if (!args.autoRc) {
    fail(
      `--no-auto-rc requires --version to already be an rc prerelease (e.g. 2.0.1-${DEFAULT_RC_IDENT}.0).`
    );
  }

  const targetVersion = await computeNextRcVersion({
    baseVersion: requested,
    registry,
    candidatePackageNames: publishablePackageNames,
  });
  return { targetVersion, distTag };
}

async function main() {
  const args = parseArgs(process.argv);
  if (!["check", "fix", "publish"].includes(args.command)) {
    fail(`Missing/invalid command.\n\n${usage()}`);
  }

  const repoRoot = process.cwd();
  const rootJson = await readJson(path.join(repoRoot, "package.json"));
  const rootVersion = rootJson.version;
  const requestedVersion = args.version ?? rootVersion;
  if (!requestedVersion) fail("Unable to determine target version. Pass --version <x.y.z>.");

  const pkgs = await loadWorkspacePackages(repoRoot);

  const versionCheck = await checkVersions(pkgs);
  if (args.command === "check") {
    const onlyVersion = versionCheck.ok ? [...versionCheck.versions.keys()][0] : null;
    if (versionCheck.ok && onlyVersion === requestedVersion) {
      process.stdout.write(`OK: all workspace packages are version ${requestedVersion}\n`);
      return;
    }
    process.stderr.write("Version mismatch detected:\n");
    process.stderr.write(`  root package.json: ${rootJson.version ?? "(no version)"}\n`);
    for (const [v, names] of versionCheck.versions.entries()) {
      process.stderr.write(`  ${v}: ${names.join(", ")}\n`);
    }
    process.exit(2);
  }

  if (args.command === "fix") {
    await setAllVersions(repoRoot, pkgs, requestedVersion);
    process.stdout.write(`Updated versions to ${requestedVersion} across workspace packages (and root).\n`);
    return;
  }

  // publish
  // Guardrails: force public npmjs unless overridden explicitly.
  if (args.registry !== NPMJS_REGISTRY) {
    fail(
      `Refusing to publish to non-npmjs registry: ${args.registry}\n` +
        `Pass --registry ${NPMJS_REGISTRY} to publish to public npm.`
    );
  }

  // Hard exclusions: these packages should not be published to npm.
  // Note: sparkling-sdk is now published to npm (iOS source bundled in ios/ directory).
  const excludedNames = new Set(["sparkling-playground", "sparkling-app-template", ...args.skip]);

  const publishable = pkgs.filter((p) => {
    if (excludedNames.has(p.name)) return false;
    if (args.includePrivate) return true;
    return !p.private;
  });
  const publishablePackageNames = publishable.map((p) => p.name).sort();

  const { targetVersion, distTag } = await resolvePublishVersionAndTag({
    args,
    rootVersion,
    registry: args.registry,
    publishablePackageNames,
  });

  // Verify `npm whoami` against expected account on the target registry.
  // Skip when using Trusted Publishing (OIDC) — npm whoami does not work with OIDC tokens.
  if (args.trustedPublishing) {
    process.stdout.write("Trusted Publishing (OIDC) enabled — skipping npm whoami check.\n");
  } else {
    const who = runCapture("npm", ["whoami", "--registry", args.registry], {
      stdio: "pipe",
      timeout: NPM_CMD_TIMEOUT_MS,
    });
    if (!who.ok) {
      fail(
        "Failed to run `npm whoami` against npmjs.org. Make sure you are logged in:\n" +
          `  npm login --registry ${args.registry}\n` +
          (who.stderr ? `\n${who.stderr}` : "")
      );
    }
    const npmUser = who.stdout.trim();
    if (args.expectedNpmUser && npmUser !== args.expectedNpmUser) {
      fail(
        `npm user mismatch: expected ${args.expectedNpmUser}, got ${npmUser}\n` +
          "Refusing to publish with an unexpected account."
      );
    }
  }

  // Ensure workspace versions match the resolved publish version.
  await setAllVersions(repoRoot, pkgs, targetVersion);

  const internalNames = new Set(publishable.map((p) => p.name));
  const ordered = topoSortPackages(publishable, (p) => internalDeps(p.json, internalNames));

  // Move --publish-last packages to the end, preserving their relative order.
  const publishLastSet = new Set(args.publishLast);
  const orderedMain = ordered.filter((p) => !publishLastSet.has(p.name));
  const orderedLast = args.publishLast
    .map((name) => ordered.find((p) => p.name === name))
    .filter(Boolean);
  const finalOrder = [...orderedMain, ...orderedLast];

  const pnpmArgsBase = ["publish", "--registry", args.registry, "--access", "public", "--tag", distTag];
  if (args.dryRun) pnpmArgsBase.push("--dry-run");
  if (args.noGitChecks) pnpmArgsBase.push("--no-git-checks");

  let publishedCount = 0;
  let skippedExistingCount = 0;
  for (const p of finalOrder) {
    const alreadyPublished = npmPackageVersionExists({
      name: p.name,
      version: targetVersion,
      registry: args.registry,
    });
    if (alreadyPublished) {
      process.stdout.write(`\n==> Skipping ${p.name}@${targetVersion} (already published)\n`);
      skippedExistingCount += 1;
      continue;
    }

    if (publishLastSet.has(p.name)) {
      process.stdout.write(`\n==> Publishing ${p.name}@${targetVersion} (publish-last)\n`);
    } else {
      process.stdout.write(`\n==> Publishing ${p.name}@${targetVersion}\n`);
    }
    run("pnpm", pnpmArgsBase, { cwd: p.dir });
    publishedCount += 1;
  }

  process.stdout.write(
    `\nPublish summary: published=${publishedCount}, skipped_already_published=${skippedExistingCount}\n`
  );
}

main().catch((err) => {
  process.stderr.write(String(err?.stack || err) + "\n");
  process.exit(1);
});
