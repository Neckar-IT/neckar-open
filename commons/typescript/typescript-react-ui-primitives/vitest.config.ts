import { sharedVitestTestOptions } from "@neckar-hub/vite-config";
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    ...sharedVitestTestOptions,
    include: ["./test/**/*.test.ts"],
  },
});
