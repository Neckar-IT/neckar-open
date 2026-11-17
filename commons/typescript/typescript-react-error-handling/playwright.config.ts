/**
 * Imports
 */
import { storybookSuiteConfig, storybookWebServer } from "@neckar-hub/playwright-config";
import { defineConfig } from "@playwright/test";

export default defineConfig({
  ...storybookSuiteConfig,
  testDir: "./tests/playwright",
  webServer: storybookWebServer({ basePort: 62020 }),
});
