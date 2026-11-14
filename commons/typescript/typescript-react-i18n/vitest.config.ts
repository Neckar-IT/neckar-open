import { commonsSourceExports, sharedVitestTestOptions } from "@neckar-hub/vite-config";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [commonsSourceExports()],
  test: {
    ...sharedVitestTestOptions,
    environment: "jsdom",
    include: ["./src/**/*.{test,spec}.{ts,tsx}"],
    setupFiles: ["tests/vitest/setup.ts"],
  },
});
