import { configDefaults } from "vitest/config";

/**
 * The shape of the shared options, structural rather than derived from `ViteUserConfig["test"]`: the
 * derived type binds the constant to one Vitest instance, the structural one survives a second. A
 * second is kept out of the store by the `@types/node` override in `pnpm-workspace.yaml`, which this
 * package does not own.
 */
interface SharedVitestTestOptions {
  detectAsyncLeaks: boolean;
  isolate: boolean;
  passWithNoTests: boolean;
  reporters: string[];
  testTimeout: number;
}

/**
 * The Vitest test options every package in the monorepo shares. Spread into the `test` block:
 *
 * ```ts
 * export default defineConfig({
 *   test: { ...sharedVitestTestOptions, environment: "jsdom" },
 * })
 * ```
 *
 * A spread rather than a `mergeConfig` wrapper, because the same constant has to work in a plain
 * `vitest.config.ts` and in a `vite.config.ts` that carries a `test` block next to its build
 * configuration — and because `mergeConfig` concatenates arrays instead of replacing them, which
 * would silently widen a project's `include` rather than let the project decide it.
 *
 * Deliberately absent: `include` and `environment`. Those decide which files run and in which
 * environment — a per-package decision that has to stay visible in the package's own config.
 */
export const sharedVitestTestOptions: SharedVitestTestOptions = {
  // workaround: dependency=pnpm:jsdom:30.1.3 observed-on=2026-10-23
  // `document.cookie` runs tough-cookie's `getCookieStringSync`, whose Promise never settles, so every test file importing i18next-browser-languagedetector reports a leak.
  detectAsyncLeaks: false,

  // The test files of a package share one worker, which cuts the run time; each file has to be independent
  // of the module state and the order of the others, and `--sequence.shuffle` finds the ones that are not.
  isolate: false,

  // Not every package has tests yet; an empty run is not a failure.
  passWithNoTests: true,

  // CI takes dot: it drops a per-file success list hundreds of lines long and keeps every failure in
  // full. Elsewhere Vitest's default adapts to the session. The copy is required — `configDefaults`
  // is only shallow-frozen, so this would otherwise hand out Vitest's own mutable array.
  reporters: process.env.CI ? ["dot"] : [...configDefaults.reporters],

  // CI runs Vitest next to eight Gradle workers on a runner with concurrency 2, where a test using
  // `userEvent.type` takes several times its local duration. Locally Vitest's 5 s default stays.
  testTimeout: process.env.CI ? 30_000 : 5_000,
};
