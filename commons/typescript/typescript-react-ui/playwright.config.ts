/**
 * Imports
 */
import { storybookSuiteConfig, storybookWebServer } from "@neckar-hub/playwright-config";
import { defineConfig } from "@playwright/test";

export default defineConfig({
  ...storybookSuiteConfig,
  webServer: storybookWebServer({ basePort: 62000 }),
});
