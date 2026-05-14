# Contributing to Sparkling

Thanks for your interest in Sparkling! This guide explains how contributors can propose ideas, report issues, and land changes while keeping the project healthy.

## How Can I Contribute?

### Reporting Bugs
- Use clear, descriptive titles and fill in steps to reproduce.
- Include environment details (host OS, platform target Android/iOS, Node.js version, pnpm version, Lynx version,Sparkling version) and screenshots/logs when possible.

### Suggesting Enhancements
- Open a feature request issue describing the problem and the value to Sparkling developers.
- Call out whether the change targets the SDK, CLI, templates.
- If you already have an approach in mind, outline it briefly so maintainers can give early feedback.

### Your First Code Contribution
- Start with small, contained changes so you can validate your setup end-to-end.
- Look for issues labeled “good first issue” (when available) or ask maintainers to confirm scope.
- Prefer to touch one package at a time (for example, `packages/sparkling-sdk` or `template/sparkling-app-template`) so reviews stay focused.

## Development Environment
- Install Node.js 22+ and pnpm `10.26.0` (matches the workspace lockfile).
- Install dependencies once: `pnpm install`.
- The workspace uses recursive scripts. Running commands from the repo root will target each package unless you filter them (e.g., `pnpm --filter sparkling-method build`).

## Pull Request Workflow
1. Fork the repository and clone your fork.
2. Create a feature branch: `git checkout -b <topic-branch>`.
3. Make your changes and keep them focused on one problem.
4. Run the checks listed in [Testing & Verification](#testing--verification) before opening a PR.
5. Write a well-structured commit message (see [Commit Message Format](#commit-message-format)).
6. Push your branch and open a Pull Request with:
   - A summary of what changed and why.
   - Links to related issues/docs if applicable.
   - Notes on testing coverage and any known gaps.

We welcome small, incremental PRs. If a change is large, break it into reviewable pieces. Maintainers may ask you to squash fixup commits before merging to keep history clean.

## Commit Message Format
Follow this template to help reviewers and future readers understand your change:

```
[Label] Concise title of the change

Summary of change:
- What changed and why.
- Previous behavior vs. new behavior.

issue: #123               # optional, link to GitHub issue
doc: https://...          # optional; required for Feature/Refactor if a design doc exists
TEST: pnpm -r test        # optional but recommended; list the commands or cases you ran
```

Labels (pick the first from this set): `[Feature]`, `[BugFix]`, `[Refactor]`, `[Optimize]`, `[Infra]`, `[Testing]`, `[Doc]`. Add secondary labels if helpful (e.g., `[Feature][iOS]`).

- The title line must contain at least one label and a non-empty description, separated by a space after the label block.
- Leave a blank line between the title and the summary section.
- Use the summary to capture context and impact; wrap lines at ~72 characters to keep logs readable.

## Testing
- Aim to add or update tests with your change; if you cannot, explain why in the PR.
- Core checks before submitting:
  - `pnpm -r build` to ensure all packages type-check and compile.
  - `pnpm -r test` to execute package tests (for example, Jest for `sparkling-method`).
  - `pnpm exec eslint .` to catch lint and license-header issues. See `docs/code-style/style-configs.md` for formatter locations and platform-specific configs.
- Targeted commands:
  - `pnpm --filter sparkling-method test` for the method CLI.
  - `pnpm --filter sparkling-app-cli build` to validate the CLI build.
- Application templates and the playground do not ship unit tests; validate changes by building/running flows instead.
- Template changes: generate a fresh app with `npx create-sparkling-app <dir> --ts`, then run the paths you touched (for example, `npx sparkling build` and `npx sparkling run:android --copy` or `run:ios --copy`).

If a check does not apply to your change, mention why in the PR description.

## Code Style
- Follow the platform guidelines documented in `docs/code-style/style-configs.md`.
- JavaScript/TypeScript uses the shared flat ESLint config in `eslint.config.js`. Keep import order tidy and ensure files carry the required license header where enforced.
- Use Prettier settings from `template/sparkling-app-template/.prettierrc` for frontend code, and platform configs (`.editorconfig`, `.clang-format`, `.swiftlint.yml`) within the template directories.
- Before submitting native code, run `scripts/lint.sh` to verify iOS Swift / ObjC / C++ and Android Kotlin formatting. CI runs the same script and will fail on any violation. If you touched native sources, run `scripts/lint.sh --fix` first to apply formatting in place, then re-run `scripts/lint.sh` to confirm a clean pass. Targets can be narrowed with `scripts/lint.sh [swift|clang|kotlin]`. Requires `swift-format`, `clang-format`, and `ktlint` on PATH (install via `brew install swift-format clang-format ktlint` on macOS).

## Reviews & Merges
- At least one maintainer review is required before merge. Reviewers may request changes or additional tests to reduce regression risk.
- CI must be green before a PR is merged. If CI exposes flakiness unrelated to your change, call it out in the PR.
- Maintainers will handle landing; please avoid force-pushes after review starts unless requested.

## Code of Conduct
Participation in this project is governed by our `CODE_OF_CONDUCT.md`. By contributing, you agree to uphold these standards.

Thank you for helping improve Sparkling!
