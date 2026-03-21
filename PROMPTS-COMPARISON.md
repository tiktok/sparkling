# CLI Prompts Migration: inquirer → @clack/prompts

Comparison of interactive prompt output before and after migration.

> **Note:** The "After" output uses @clack/prompts' box-drawing style (◆/◇/│/└).
> The "Before" output uses inquirer's `?`/`❯` style.
> Raw captures were taken via `expect` + `script`; clack's character-level rendering
> doesn't capture cleanly in PTY recordings, so the "After" sections below are
> reconstructed from the raw ANSI stream.

---

## 1. `create-sparkling-app test-app --no-install --no-git`

### Before (inquirer)

```
create-sparkling-app  v2.0.0-rc.6-local
┌──────────────────────────────────────────────────────────────┐
│ Welcome to Sparkling!                                        │
│ Create a Lynx app with various native abilities in minutes.  │
└──────────────────────────────────────────────────────────────┘
  ↳ Will create in test-app

? Choose a template (Use arrow keys)
❯ sparkling-default (Official Sparkling starter project)
  Custom template (Local path, Git URL, or npm package)

? Choose a template sparkling-default (Official Sparkling starter project)

? Android Gradle build files (Use arrow keys)
❯ Kotlin DSL (.gradle.kts)
  Groovy (.gradle)

? Android Gradle build files Kotlin DSL (.gradle.kts)

? Select development tools (Press <space> to select, <a> to toggle all,
  <i> to invert selection, and <enter> to proceed)
❯◯ Add ReactLynx Testing Library for unit testing

? Select development tools

? Select optional tooling (Press <space> to select, <a> to toggle all,
  <i> to invert selection, and <enter> to proceed)
❯◯ ESLint (Standard linting configuration)
 ◯ Prettier (Auto-formatting defaults)
 ◯ Biome (Biome + Biome formatter)

? Select optional tooling

? Package namespace (Android package / iOS bundle id) (com.test.app)

? Package namespace (Android package / iOS bundle id) com.test.app

Installing npm template: sparkling-app-template@latest
Building project with 2 steps
Running create-app actions...
Run action: create-app-project
Finished action: create-app-project (took 1431ms)

Output paths:
  - /private/tmp/sparkling-demo/before/test-app

All actions completed.
✔ Project created at test-app
Next steps
cd test-app
Install dependencies: pnpm install
pnpm run:ios
pnpm run:android
Tip iOS: ensure Xcode Command Line Tools are installed.
Tip Android: ensure ANDROID_HOME and SDK platforms are set.
Successfully created app project!
```

### After (@clack/prompts)

```
create-sparkling-app  v2.0.0-rc.6-local
┌──────────────────────────────────────────────────────────────┐
│ Welcome to Sparkling!                                        │
│ Create a Lynx app with various native abilities in minutes.  │
└──────────────────────────────────────────────────────────────┘
  ↳ Will create in test-app

│
◆  Choose a template
│  ● sparkling-default (Official Sparkling starter project)
│  ○ Custom template (Local path, Git URL, or npm package)
└

◇  Choose a template
│  sparkling-default (Official Sparkling starter project)
│
◆  Android Gradle build files
│  ● Kotlin DSL (.gradle.kts)
│  ○ Groovy (.gradle)
└

◇  Android Gradle build files
│  Kotlin DSL (.gradle.kts)
│
◆  Select development tools
│  ◻ Add ReactLynx Testing Library for unit testing
└

◇  Select development tools
│  none
│
◆  Select optional tooling
│  ◻ ESLint (Standard linting configuration)
│  ◻ Prettier (Auto-formatting defaults)
│  ◻ Biome (Biome + Biome formatter)
└

◇  Select optional tooling
│  none
│
◆  Package namespace (Android package / iOS bundle id)
│  com.test.app_
└

◇  Package namespace (Android package / iOS bundle id)
│  com.test.app

Installing npm template: sparkling-app-template@latest
Building project with 2 steps
Running create-app actions...
Run action: create-app-project
Finished action: create-app-project (took 1066ms)

Output paths:
  - /private/tmp/sparkling-demo/after/test-app

All actions completed.
✔ Project created at test-app
Next steps
cd test-app
Install dependencies: pnpm install
pnpm run:ios
pnpm run:android
Tip iOS: ensure Xcode Command Line Tools are installed.
Tip Android: ensure ANDROID_HOME and SDK platforms are set.
Successfully created app project!
```

### Key visual differences — create-sparkling-app

| Aspect | inquirer | @clack/prompts |
|--------|----------|----------------|
| Prompt indicator | `?` prefix | `◆` (active) / `◇` (answered) |
| Selection cursor | `❯` arrow | `●` filled / `○` empty radio |
| Checkbox style | `◯` circle | `◻` / `◼` square |
| Instructions | `(Use arrow keys)`, `(Press <space>...)` | None (cleaner) |
| Answered state | Inline after `?` | Collapsed below `◇` with dim text |
| Connector lines | None | `│` vertical lines connecting prompts |
| Empty selection | Blank | Shows `none` explicitly |
| Text input | Inline after `?` | Inside box with cursor `_` |

---

## 2. `sparkling-method-cli init my-method`

### Before (inquirer)

```
? Namespace / bundle identifier (e.g. com.example): (org.sparkling) com.example.demo

? Namespace / bundle identifier (e.g. com.example): com.example.demo

? Module name (PascalCase, e.g. Storage): (MyMethod) DemoModule

? Module name (PascalCase, e.g. Storage): DemoModule

? Android Gradle DSL: (Use arrow keys)
❯ Kotlin (.gradle.kts)
  Groovy (.gradle)

? Android Gradle DSL: Kotlin (.gradle.kts)

✨ Initializing sparkling method project in /private/tmp/.../my-method

✅ Project created successfully.
Tip cd my-method
Tip Edit and rename src/method.d.ts to describe your APIs.
Tip Run `npm run codegen` to generate native stubs.
```

### After (@clack/prompts)

```
│
◆  Namespace / bundle identifier (e.g. com.example):
│  org.sparkling_
└

◇  Namespace / bundle identifier (e.g. com.example):
│  com.example.demo
│
◆  Module name (PascalCase, e.g. Storage):
│  MyMethod_
└

◇  Module name (PascalCase, e.g. Storage):
│  DemoModule
│
◆  Android Gradle DSL:
│  ● Kotlin (.gradle.kts)
│  ○ Groovy (.gradle)
└

◇  Android Gradle DSL:
│  Kotlin (.gradle.kts)

✨ Initializing sparkling method project in /private/tmp/.../my-method

✅ Project created successfully.
Tip cd my-method
Tip Edit and rename src/method.d.ts to describe your APIs.
Tip Run `npm run codegen` to generate native stubs.
```

### Key visual differences — sparkling-method-cli

| Aspect | inquirer | @clack/prompts |
|--------|----------|----------------|
| Default value display | `(org.sparkling)` inline dim | Shown as placeholder in input box |
| Text typing | Characters appear inline | Characters appear inside bordered box |
| Selection list | `❯ Option` cursor | `● Option` / `○ Option` radio |
| Prompt flow | Each prompt is independent, flat | Prompts are connected by `│` lines |

---

## 3. Non-interactive mode (`--yes` / all flags provided)

Output is **identical** between before and after — prompts are completely skipped.

```
# create-sparkling-app test-app --yes --no-install --no-git
# (output is the same for both)

create-sparkling-app  v2.0.0-rc.6-local
┌──────────────────────────────────────────────────────────────┐
│ Welcome to Sparkling!                                        │
│ Create a Lynx app with various native abilities in minutes.  │
└──────────────────────────────────────────────────────────────┘
  ↳ Will create in test-app
Installing npm template: sparkling-app-template@latest
Building project with 2 steps
...
Successfully created app project!
```

```
# sparkling-method-cli init my-method --package-name com.example.demo
#   --module-name DemoModule --android-dsl kts
# (output is the same for both)

✨ Initializing sparkling method project in .../my-method

✅ Project created successfully.
Tip cd my-method
Tip Edit and rename src/method.d.ts to describe your APIs.
Tip Run `npm run codegen` to generate native stubs.
```
