/**
 * Imports
 */
import { ladleSuiteConfig, ladleWebServer } from "@neckar-hub/playwright-config";
import { defineConfig } from "@playwright/test";

export default defineConfig({
  ...ladleSuiteConfig,
  testDir: "./tests/playwright",
  webServer: ladleWebServer({ basePort: 61010 }),
});
