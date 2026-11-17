# Oxlint Plugin

Custom oxlint rules for the monorepo — the rules that enforce this codebase's own conventions, which no upstream rule covers.

The plugin is loaded once, centrally, in [`@neckar-hub/oxlint-config`](../oxlint-config/src/oxlint.config.ts) via `jsPlugins`. Every package that consumes the shared config gets it; no package declares the plugin itself.

## Rules

| Rule | Severity | Where |
|---|---|---|
| `neckar/no-raw-id-types` | `error` | opt-in — set in each package's own `oxlint.config.ts` |
| `neckar/no-deprecated-zod-message` | `error` | shared config, applies everywhere |

### `neckar/no-raw-id-types`

ID-shaped properties (`*Id`, `*Ids`, `*Uuid`, `*Uuids`, `*Ref`, `*Refs`) must carry the `Uuid` brand from `@neckar-hub/typescript-utils`, not a raw `string` / `number` / `bigint`.

```typescript
interface Order {
  customerId: string; // error
}

interface Order {
  customerId: Uuid; // ok — as are Uuid[], Array<Uuid>, Uuid | null, Uuid | undefined, Customer["id"]
}
```

Still rolling out per package (#1954); `git grep -l '"neckar/no-raw-id-types": "error"' -- '*oxlint.config.ts'` lists the packages with the rule on. Switch it on in a package by adding it to that package's rules:

```typescript
import sharedConfig from "@neckar-hub/oxlint-config";
import { defineConfig } from "oxlint";

export default defineConfig({
  ...sharedConfig,
  rules: {
    ...sharedConfig.rules,
    "neckar/no-raw-id-types": "error",
  },
});
```

### `neckar/no-deprecated-zod-message`

Zod deprecated the `message` parameter of schemas and checks in favour of `error` (#2725). The rule matches the params object of a Zod call and flags a `message` key in it.

```typescript
z.string().min(1, { message: "required" }); // error
z.string().min(1, { error: "required" }); // ok
```

The params object is the first argument for checks without a value (`.positive({ … })`) and for bare type constructors (`z.string({ … })`); it follows the value everywhere else (`.min(1, { … })`, `.refine(check, { … })`, `z.enum(values, { … })`). Distinguishing the two positions is what keeps declarations of a field named `message` out of the rule:

```typescript
z.object({ message: z.string() }); // ok — declares a field, does not set an error text
base.extend({ message: z.string() }); // ok
ctx.addIssue({ code: "custom", message: "missing" }); // ok — Zod issue payload, not deprecated
form.setError("icon", { message: "pick a file" }); // ok — react-hook-form, a different API
```

`typescript/no-deprecated` does not catch this: `message` is a property key inside an object literal, not a referenced symbol, so a rule that flags deprecated *symbols* never sees it.

## Adding a rule

1. Write the rule in [`src/index.mjs`](src/index.mjs) and register it under `rules`. The file is `.mjs` because oxlint loads JS plugins through Node's native ESM loader, which has no TypeScript support — keep it dependency-free.
2. Add `src/rules/<rule-name>.test.ts`, modelled on the existing ones: fixtures are written to a temp dir and linted by a real oxlint process, so the test exercises the rule exactly as a package build does. Cover both directions — a `VIOLATIONS` list and a `NON_VIOLATIONS` list.
3. Decide where the rule is switched on:
   - repo-wide from the start → add it to `rules` in [`@neckar-hub/oxlint-config`](../oxlint-config/src/oxlint.config.ts)
   - existing violations to clean up first → leave it out of the shared config and switch it on per package as those packages are cleaned

A rule must find bugs or enforce an architectural decision. Cosmetic rules are disabled — see the rationale comments in the shared config.

## Tests

```shell
pnpm -C internal/open/config/typescript/oxlint-plugin test
```
