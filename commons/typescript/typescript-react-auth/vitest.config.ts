import { sharedVitestTestOptions } from "@neckar-hub/vite-config";
import { defineConfig } from "vitest/config";

/**
 * Vitest Configuration
 */
export default defineConfig({
  test: {
    ...sharedVitestTestOptions,
    environment: "jsdom",
    include: ["./src/**/*.{test,spec}.{ts,tsx}"],
    setupFiles: ["./tests/vitest/setup.ts"],
  },
});
