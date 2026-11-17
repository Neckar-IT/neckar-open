import { sharedVitestTestOptions } from "@neckar-hub/vite-config";
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    ...sharedVitestTestOptions,
    setupFiles: ["./src/__tests__/setup.ts"],
    environment: "jsdom",
    include: ["./src/**/*.{test,spec}.{ts,tsx}"],
  },
});
