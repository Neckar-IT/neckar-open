import { defineConfig } from "vitest/config";
import { sharedVitestTestOptions } from "./src/vitest-config.js";

// The shared options come in through a relative path rather than through the package name every
// other package uses: this package has no "exports" field, and a Node self-reference needs one.
export default defineConfig({
  test: {
    ...sharedVitestTestOptions,
    environment: "node",
    include: ["./src/**/*.{test,spec}.{ts,tsx}"],
  },
});
