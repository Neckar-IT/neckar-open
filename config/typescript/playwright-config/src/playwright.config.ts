/**
 * Imports
 */

import type { PlaywrightTestConfig } from "@playwright/test";
import { defineConfig, devices } from "@playwright/test";

/**
 * The single web server entry of a Playwright config — `PlaywrightTestConfig["webServer"]` also
 * admits an array and `undefined`, neither of which `ladleWebServer` and `storybookWebServer` return.
 */
type WebServerConfig = Extract<NonNullable<PlaywrightTestConfig["webServer"]>, { command: string }>;

/**
 * Shareable Playwright configuration
 *
 * Carries no `webServer`: a suite either drives a story browser (see `storybookWebServer`) or the
 * product's own dev server, and a shared default would silently point the wrong one at a port.
 */

export const sharedConfig = defineConfig({
  testDir: "./tests/playwright",
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"], locale: "de" } },
    // { name: "firefox", use: devices["Desktop Firefox"] },
  ],
  fullyParallel: true,
  workers: "50%",
  reporter: process.env.CI ? "dot" : "html",
  retries: process.env.CI ? 2 : 0,
});

/**
 * Configuration of a suite against the package's stories, the suite the Gradle task
 * `integrationTest` runs, with its baselines in `snapshotDir`.
 */
function storySuiteConfig(snapshotDir: string): PlaywrightTestConfig {
  return defineConfig({
    ...sharedConfig,
    // In CI a failure keeps its trace: network, console and DOM per step.
    projects: sharedConfig.projects?.map((project) => ({ ...project, use: { ...project.use, trace: process.env.CI ? "retain-on-failure" : "off" } })),
    // Also the budget of toHaveScreenshot: a stable screenshot takes at least two captures, and one
    // full-page capture of an app page took 4 s on a busy CI runner; Playwright's 5 s left room for one.
    expect: { timeout: 30_000 },
    // The JUnit file sits where build-result.json reads the `integrationTest` results, so a failed
    // comparison becomes a `failedTests` entry.
    outputDir: "./build/playwright/test-results",
    snapshotDir,
    reporter: [
      [process.env.CI ? "dot" : "list"],
      ["junit", { outputFile: "./build/test-results/integrationTest/TEST-playwright.xml" }],
      ["html", { outputFolder: "./build/reports/playwright" }],
    ],
  });
}

/**
 * Configuration of a suite against the package's Ladle stories; add `webServer: ladleWebServer(…)`.
 * The Gradle task `extractLadleBaselines` unpacks the baselines from the branch data/ladle-baselines.
 */
export const ladleSuiteConfig = storySuiteConfig("./build/ladle-baselines");

/**
 * Configuration of a suite against the package's Storybook stories; add
 * `webServer: storybookWebServer(…)`. The Gradle task `extractStorybookBaselines` unpacks the
 * baselines from the branch data/storybook-baselines.
 */
export const storybookSuiteConfig = storySuiteConfig("./build/storybook-baselines");

/**
 * Web server entry that serves a package's Ladle stories for screenshot tests.
 *
 * `basePort` is the package's slot in `LadleDevPorts` (Kotlin side, checked by
 * `./gradlew verifyLadlePorts`). Gradle's `integrationTest` task overrides it through `LADLE_PORT`
 * with the port of the current worktree, so screenshot runs in parallel worktrees neither share a
 * server nor fight over the port.
 */
export function ladleWebServer(options: { basePort: number }): WebServerConfig {
  // An empty LADLE_PORT counts as unset: a shell wrapper that exports the variable without a value
  // would otherwise produce `--port ` and a URL that never answers.
  const portFromEnvironment = process.env.LADLE_PORT;
  const port = portFromEnvironment === undefined || portFromEnvironment === "" ? String(options.basePort) : portFromEnvironment;
  // Published to the test files, which collect the story list from the running server before the
  // first test runs and therefore need the port outside of any fixture. Playwright's workers
  // inherit the environment of the process that loads this config.
  process.env.LADLE_PORT = port;

  return {
    // Bound to the address `url` polls: Ladle's default `localhost` resolves to ::1 alone in the
    // CI container, and Playwright waited for 127.0.0.1 until its timeout.
    command: `pnpm run ladle --port ${port} --host 127.0.0.1`,
    url: `http://127.0.0.1:${port}`,
    // Locally a story browser is often already open on this port and reusing it saves the startup;
    // in CI a listening server can only be a leftover from an earlier job, and testing against it
    // would compare stories from another commit.
    reuseExistingServer: process.env.CI === undefined,
  };
}

/**
 * Base URL of the story browser the current run serves, for test files that query it directly
 * (`/meta.json`, story pages).
 */
export function ladleServerUrl(): string {
  const port = process.env.LADLE_PORT;
  if (port === undefined) {
    throw new Error("LADLE_PORT is not set — the package's playwright.config.ts must use `ladleWebServer`");
  }

  return `http://127.0.0.1:${port}`;
}

/**
 * Web server entry that serves a package's Storybook stories for screenshot tests.
 *
 * `basePort` is the package's slot in `StorybookDevPorts` (Kotlin side, checked by
 * `./gradlew verifyStorybookPorts`). Gradle's `integrationTest` task overrides it through
 * `STORYBOOK_PORT` with the port of the current worktree.
 */
export function storybookWebServer(options: { basePort: number }): WebServerConfig {
  // An empty STORYBOOK_PORT counts as unset, as in `ladleWebServer`.
  const portFromEnvironment = process.env.STORYBOOK_PORT;
  const port = portFromEnvironment === undefined || portFromEnvironment === "" ? String(options.basePort) : portFromEnvironment;
  // Published to the test files, which read the story index before the first test runs.
  process.env.STORYBOOK_PORT = port;

  return {
    // `--exact-port` fails instead of moving to the next free port, which `url` would never reach;
    // `--ci` skips every interactive prompt.
    command: `pnpm run storybook --port ${port} --host 127.0.0.1 --exact-port --ci`,
    url: `http://127.0.0.1:${port}/index.json`,
    // Locally a Storybook is often already open on this port; in CI a listening server can only be
    // a leftover from an earlier job, serving the stories of another commit.
    reuseExistingServer: process.env.CI === undefined,
  };
}

/**
 * Base URL of the Storybook the current run serves, for test files that read `/index.json` or open
 * `/iframe.html?id=<story id>` directly.
 */
export function storybookServerUrl(): string {
  const port = process.env.STORYBOOK_PORT;
  if (port === undefined) {
    throw new Error("STORYBOOK_PORT is not set — the package's playwright.config.ts must use `storybookWebServer`");
  }

  return `http://127.0.0.1:${port}`;
}

export { openLadleBuildUnderStaticSitePolicy, openStorybookBuildUnderStaticSitePolicy } from "./static-site-server";
export { testDataFile } from "./test-data-file";
export { storybookStories, type StorybookStory } from "./storybook-stories";
