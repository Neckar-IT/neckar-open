import { defineConfig } from "vitest/config";
import { sharedVitestTestOptions } from "./src/vitest-config.js";

// The shared options come in through a relative path rather than through the package name every
// other package uses: the package name resolves to `dist/`, which exists only after `bundle`.
export default defineConfig({
  test: {
    ...sharedVitestTestOptions,
    environment: "node",
    include: ["./src/**/*.{test,spec}.{ts,tsx}"],
  },
});
