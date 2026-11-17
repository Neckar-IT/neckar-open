import path from "node:path";
import { sharedVitestTestOptions } from "@neckar-hub/vite-config";
/**
 * Imports
 */
import { defineConfig } from "vitest/config";

/**
 * Vitest Configuration
 */

export default defineConfig({
  resolve: {
    alias: {
      "@test": path.resolve(__dirname, "tests/vitest"),
    },
  },
  test: {
    ...sharedVitestTestOptions,
    environment: "jsdom",
    include: ["./src/**/*.test.tsx", "./src/**/*.test.ts", "./scripts/**/*.test.ts"],
    exclude: ["./tests/playwright/**"],
    setupFiles: ["./tests/vitest/setup.ts"],
    coverage: {
      provider: "v8",
      reporter: ["text", "html"],
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/**/*.test.{ts,tsx}", "src/**/*.stories.tsx", "src/**/index.ts", "src/**/*.d.ts"],
    },
  },
});
