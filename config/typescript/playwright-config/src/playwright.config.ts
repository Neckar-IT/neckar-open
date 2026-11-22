/**
 * Imports
 */

import type { PlaywrightTestConfig } from "@playwright/test";
import { defineConfig, devices } from "@playwright/test";

/**
 * The single web server entry of a Playwright config — `PlaywrightTestConfig["webServer"]` also
 * admits an array and `undefined`, neither of which `storybookWebServer` returns.
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
 * Configuration of a suite against the package's Storybook stories, the suite the Gradle task
 * `integrationTest` runs; add `webServer: storybookWebServer(…)`. The Gradle task
 * `extractStorybookBaselines` unpacks the baselines from the branch data/storybook-baselines.
 */
export const storybookSuiteConfig: PlaywrightTestConfig = defineConfig({
  ...sharedConfig,
  // In CI a failure keeps its trace: network, console and DOM per step.
  projects: sharedConfig.projects?.map((project) => ({ ...project, use: { ...project.use, trace: process.env.CI ? "retain-on-failure" : "off" } })),
  // Also the budget of toHaveScreenshot: a stable screenshot takes at least two captures, and one
  // full-page capture of an app page took 4 s on a busy CI runner; Playwright's 5 s left room for one.
  expect: { timeout: 30_000 },
  // The JUnit file sits where build-result.json reads the `integrationTest` results, so a failed
  // comparison becomes a `failedTests` entry.
  outputDir: "./build/playwright/test-results",
  snapshotDir: "./build/storybook-baselines",
  reporter: [
    [process.env.CI ? "dot" : "list"],
    ["junit", { outputFile: "./build/test-results/integrationTest/TEST-playwright.xml" }],
    ["html", { outputFolder: "./build/reports/playwright" }],
  ],
});

/**
 * Web server entry that serves a package's Storybook stories for screenshot tests.
 *
 * `basePort` is the package's slot in `StorybookDevPorts` (Kotlin side, checked by
 * `./gradlew verifyStorybookPorts`). Gradle's `integrationTest` task overrides it through
 * `STORYBOOK_PORT` with the port of the current worktree.
 */
export function storybookWebServer(options: { basePort: number }): WebServerConfig {
  // An empty STORYBOOK_PORT counts as unset: a shell wrapper that exports the variable without a
  // value would otherwise produce `--port ` and a URL that never answers.
  const portFromEnvironment = process.env.STORYBOOK_PORT;
  const port = portFromEnvironment === undefined || portFromEnvironment === "" ? String(options.basePort) : portFromEnvironment;
  // Published to the test files, which read the story index before the first test runs.
  process.env.STORYBOOK_PORT = port;

  return {
    // `--exact-port` fails instead of moving to the next free port, which `url` would never reach;
    // `--ci` skips every interactive prompt.
    command: `pnpm run storybook --port ${port} --exact-port --ci`,
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

export { openStorybookBuildUnderStaticSitePolicy } from "./static-site-server";
export { testDataFile } from "./test-data-file";
export { storybookStories, type StorybookStory } from "./storybook-stories";
