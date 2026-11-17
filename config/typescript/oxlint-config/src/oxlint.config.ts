import { defineConfig } from "oxlint";

/**
 * Shared oxlint configuration for all TypeScript projects.
 *
 * Replaces the previous ESLint configuration (@neckar-hub/eslint-config).
 * Rules are mapped from the ESLint flat config to oxlint equivalents.
 *
 * Plugin coverage:
 * - Built-in: @typescript-eslint, react-hooks, jsx-a11y, promise, node, unicorn, oxc
 * - Not available (deactivated): @tanstack/query, testing-library, @eslint-react, astro
 * - Replaced by oxfmt: prettier, tailwindcss, json formatting
 */
export default defineConfig({
  plugins: ["react", "typescript", "unicorn", "oxc", "jsx-a11y", "promise", "node"],

  // Loaded for every package so the repo-wide custom rules below apply
  // everywhere, not only in the packages that opted in individually. Rules that
  // are still being rolled out per package (`neckar/no-raw-id-types`) stay off
  // here and are switched on in the package's own oxlint.config.ts.
  jsPlugins: ["@neckar-hub/oxlint-plugin"],

  categories: {
    // All three categories break the build (#1743): correctness and suspicious
    // are bug-finders, perf catches real inefficiencies. New violations must
    // fail, not accumulate as warnings. Rules whose existing violations are too
    // costly to clean up now are individually set to `off` below with a
    // rationale and a #1954 reference — the category default stays `error`, so
    // only the named exceptions are exempt, not whole categories.
    correctness: "error",
    suspicious: "error",
    perf: "error",
  },

  rules: {
    // ── Core JS rules ──────────────────────────────────────────────────────────

    "no-void": "off",
    "no-console": "off",
    // Pure naming convention — `_unused`, `_private`, `_internal` patterns
    // are widely used and intentional. No bugs.
    "no-underscore-dangle": "off",
    // Promoted to error (#1743): no existing violations, each catches a real
    // redundancy. `no-useless-rename` (`{ a: a }`), `no-useless-catch`
    // (try/catch that only rethrows), `no-extra-boolean-cast` (`!!x` in a
    // boolean context), `prefer-const` (auto-fixable, never-reassigned `let`).
    "no-useless-rename": "error",
    "no-useless-catch": "error",
    "no-extra-boolean-cast": "error",
    "prefer-const": "error",
    "no-fallthrough": "error",
    "default-case": "error",
    "default-case-last": "error",
    // Disabled (#1743): valuable (shadowing hides bugs) but ~62 existing
    // violations are mechanical renames out of scope here. Cleanup-and-promote
    // tracked in #1954.
    "no-shadow": "off",

    // Prevent using ambiguous global names as variable names.
    // Only includes names commonly used accidentally as local variables.
    // Browser APIs like document, navigator, setTimeout are NOT restricted.
    "no-restricted-globals": [
      "error",
      "alert",
      "blur",
      "close",
      "closed",
      "confirm",
      "event",
      "external",
      "find",
      "focus",
      "length",
      "name",
      "open",
      "opener",
      "origin",
      "parent",
      "print",
      "prompt",
      "scroll",
      "self",
      "status",
      "stop",
      "top",
    ],

    // ── TypeScript rules ───────────────────────────────────────────────────────

    "typescript/no-explicit-any": ["error", { ignoreRestArgs: true }],
    // Kept on warn (#1743) — the one deliberate exception to the no-warn policy.
    // Deprecation is a tech-debt radar, not a build gate: `error` would force
    // immediate replacement of still-working deprecated APIs (and would fire on
    // our own "deprecate and keep" pattern, where a deprecated symbol's barrel
    // `index.ts` re-export and `.stories.tsx` always re-trigger it), while `off`
    // would hide genuine external-library deprecations (`z.email()`, Vitest
    // matchers, React `FormEvent`). `warn` keeps them visible without breaking
    // the build. The lint scripts do not pass `--max-warnings`, so these stay
    // non-blocking.
    "typescript/no-deprecated": "warn",
    // Promoted to error (#2725): `typescript/no-deprecated` does not see the
    // deprecated `message` parameter of Zod schemas and checks — it is a
    // property key inside a params object literal, not a referenced symbol, so
    // the whole class of `z.string().min(1, { message })` calls stayed silent
    // until Zod removes the parameter. The custom rule matches that shape
    // syntactically. Takes effect only where the package loads
    // `@neckar-hub/oxlint-plugin` via `jsPlugins`.
    "neckar/no-deprecated-zod-message": "error",
    "typescript/no-inferrable-types": "off",
    // Promoted to error (#1743): no existing violations of the TS-aware variant.
    "typescript/no-unused-vars": [
      "error",
      {
        argsIgnorePattern: "^_",
        varsIgnorePattern: "^_",
        caughtErrors: "all",
        caughtErrorsIgnorePattern: "^_ignore",
      },
    ],
    "typescript/no-use-before-define": "off",
    "typescript/consistent-type-definitions": "off",
    // Promoted to error (#1743): both are auto-fixable and keep type-only
    // imports/exports out of the runtime graph. consistent-type-exports has no
    // existing violations; consistent-type-imports violations are auto-fixed.
    "typescript/consistent-type-exports": "error",
    "typescript/consistent-type-imports": "error",
    // Pure refactoring rule — flags casts that TypeScript would accept without
    // them. The cast might still be there as documentation of the assumption
    // at a type-boundary; removing it doesn't make the code better. No bugs.
    "typescript/no-unnecessary-type-assertion": "off",
    "typescript/no-unnecessary-type-arguments": "off", // too aggressive, often we want to specify type arguments for clarity even if they could be inferred
    // Disabled (#1743): cosmetic, not a bug-finder. A type parameter used once
    // (e.g. a phantom/factory param like `createUsePathParams<AppRoutes>()`)
    // changes no runtime behaviour; at worst it is mild API-signature noise.
    // Per the "rules must find bugs, cosmetic rules are disabled" policy.
    "typescript/no-unnecessary-type-parameters": "off",
    // Fires on every `as X` cast, including legitimate ones: library APIs that
    // return `any`/`unknown` by design (React Hook Form `field.value`,
    // `@tanstack/table` `getFilterValue()`, DOM `event.error`), documented
    // TS limits (CSS custom properties on `CSSProperties`, `Omit<T,K>` not
    // assignable back to `T`, `keyof typeof` lookups), and OpenAPI-generated
    // discriminated unions. Trivially worked around with a typed local
    // declaration (`const x: T = anyValue`) that has identical runtime risk —
    // the rule prefers a syntactic form, not actual safety. Real unsafe
    // casts are already covered by `no-explicit-any` (for `as any`) and
    // `no-non-null-assertion` (for `!`). See discussion in #1782.
    "typescript/no-unsafe-type-assertion": "off",
    "typescript/no-mixed-enums": "error",
    "typescript/no-unsafe-enum-comparison": "error",
    // Disabled (#1743): static-only classes are an intentional namespacing
    // pattern in this codebase (e.g. FeatureFlagsSupport). The rule's preferred
    // "standalone functions" alternative loses the grouping the class provides.
    "typescript/no-extraneous-class": "off",
    "typescript/no-empty-object-type": "off",
    // Promoted to error (#1743): no existing violations; flags void expressions
    // used where a value is expected. Arrow shorthand stays allowed.
    "typescript/no-confusing-void-expression": ["error", { ignoreArrowShorthand: true }],
    "typescript/no-misused-promises": ["error", { checksVoidReturn: { attributes: false } }],
    // Disabled (#1743): under type-aware checking this fires on defensive
    // runtime guards the types declare unnecessary but which protect against
    // backend/frontend drift (the same intent as the mandatory `default:` in
    // exhaustive switches). High false-positive rate; genuine dead conditions
    // are reviewed for promotion in #1954.
    "typescript/no-unnecessary-condition": "off",
    "typescript/strict-boolean-expressions": [
      "error",
      {
        allowNullableBoolean: true,
        allowNullableEnum: true,
        allowNullableString: true,
      },
    ],
    // Discriminated-union switches must list every case explicitly AND keep a
    // `default: throw new Error(...)` branch as a runtime safety net against
    // backend/frontend drift. `considerDefaultExhaustiveForUnions: false` makes
    // the rule keep checking for missing union cases even when a `default:`
    // branch is present, so the compile-time guarantee is not weakened by the
    // mandatory `default-case` rule above. `requireDefaultForNonUnion: true`
    // covers plain string/number/enum switches. See
    // `internal/patterns/docs/typescript/exhaustive-switch.md`.
    "typescript/switch-exhaustiveness-check": [
      "error",
      {
        allowDefaultCaseForExhaustiveSwitch: true,
        considerDefaultExhaustiveForUnions: false,
        requireDefaultForNonUnion: true,
      },
    ],
    "typescript/consistent-return": "off",
    // Promoted to error (#1743): prefers `??` over `||` so a falsy-but-nullish
    // number (0) is not accidentally replaced. Booleans and strings stay exempt
    // because `x || default` on them is a deliberate idiom: `flag || default`
    // for booleans and `name.trim() || "fallback"` for the empty-string case,
    // where `??` would wrongly let "" through.
    "typescript/prefer-nullish-coalescing": ["error", { ignorePrimitives: { boolean: true, string: true } }],

    // ── React rules ────────────────────────────────────────────────────────────

    "react/exhaustive-deps": "error",
    "react/jsx-key": "error",
    "react/react-in-jsx-scope": "off",
    // Disabled (#1743): all valuable, but each has a large body of existing
    // violations that needs a manual refactor per site, out of scope here.
    // no-unstable-nested-components (~82): components defined during render.
    // no-array-index-key (~48): array index used as React key.
    // no-object-type-as-default-prop (~6): `new`/object-literal default prop
    // breaks referential equality. Cleanup-and-promote tracked in #1954.
    "react/no-unstable-nested-components": "off",
    "react/no-array-index-key": "off",
    "react/no-object-type-as-default-prop": "off",

    // ── React Compiler rules ───────────────────────────────────────────────────

    // These are the React Compiler's own diagnostics. The compiler is not part of
    // any build here; without it they describe missed memoization, not defects —
    // it bails out per component instead of failing. `react/hooks` is the
    // exception and stays at the category default: a conditional or nested hook
    // call is wrong today, compiler or not.

    // Fires on react-hook-form's `useForm`/`useFieldArray`, whose returned
    // functions cannot be memoized. A property of the library, not of our call
    // sites — stays off as long as react-hook-form is in use.
    "react/incompatible-library": "off",
    // The rest need a manual, behaviour-changing refactor per site (ref writes
    // during render, state derived in effects, purity). Enabling them belongs
    // with the decision to adopt the React Compiler, not before it.
    "react/refs": "off",
    "react/set-state-in-effect": "off",
    "react/exhaustive-effect-dependencies": "off",
    "react/immutability": "off",
    "react/purity": "off",
    "react/capitalized-calls": "off",
    "react/memo-dependencies": "off",
    "react/no-deriving-state-in-effects": "off",

    // ── JSX Accessibility rules ────────────────────────────────────────────────

    // Promoted to error (#1743): no existing violations; missing `alt` text is
    // an unambiguous accessibility defect.
    "jsx-a11y/alt-text": "error",
    "jsx-a11y/no-autofocus": "off",
    // Disabled (#1743): the remaining a11y rules below fire on shared UI
    // primitives (Combobox, ButtonGroup, Breadcrumb, NumberInput …) where the
    // fix is a non-mechanical markup/ARIA change with behavioural risk. A
    // dedicated accessibility cleanup-and-promote pass is tracked in #1954.
    "jsx-a11y/prefer-tag-over-role": "off",
    "jsx-a11y/role-has-required-aria-props": "off",
    "jsx-a11y/no-static-element-interactions": "off",
    "jsx-a11y/click-events-have-key-events": "off",
    "jsx-a11y/control-has-associated-label": "off",

    // ── Unicorn rules ──────────────────────────────────────────────────────────

    "unicorn/no-empty-file": "off",
    // `on*` property assignment is a documented part of the DOM spec and fine
    // for set-once single handlers. The rule cannot distinguish that case from
    // genuine bugs (overwritten library handlers, missed options), so it
    // produces noise on legitimate code. Re-enable when a real incident hits.
    "unicorn/prefer-add-event-listener": "off",
    // Stylistic — wants nested functions hoisted to module scope if they don't
    // close over locals. Has no bug-finding value; nesting at the use site is
    // often the more readable choice.
    "unicorn/consistent-function-scoping": "off",
  },

  ignorePatterns: ["**/build/", "**/dist/", "**/node_modules/", "**/tmp/", "**/.astro", "**/env.d.ts", "**/attic", "**/sandbox"],

  settings: {
    react: {
      formComponents: [],
      linkComponents: [],
      componentWrapperFunctions: [],
    },
  },
});
