import path from "node:path";
import { sharedVitestTestOptions } from "@neckar-hub/vite-config";
import { defineConfig } from "vitest/config";

/**
 * Vitest Configuration
 */

export default defineConfig({
  resolve: {
    alias: {
      "@test": path.resolve(import.meta.dirname, "tests/vitest"),
    },
  },
  test: {
    ...sharedVitestTestOptions,
    environment: "jsdom",
    include: ["./src/**/*.{test,spec}.{ts,tsx}"],
    setupFiles: ["./tests/vitest/setup.ts"],
  },
});
