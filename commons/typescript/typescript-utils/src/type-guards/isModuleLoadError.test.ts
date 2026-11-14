/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { isModuleLoadError } from "./isModuleLoadError";

/**
 * Tests
 */

describe("isModuleLoadError", () => {
  it.each([
    ["Chromium", "Failed to fetch dynamically imported module: http://localhost:3000/assets/FloorPlanEditorPage-a1b2c3.js"],
    ["Firefox", "error loading dynamically imported module: http://localhost:3000/assets/FloorPlanEditorPage-a1b2c3.js"],
    ["Safari", "Importing a module script failed."],
  ])("matches the import() rejection of %s: %s", (_browser, message) => {
    expect(isModuleLoadError(new TypeError(message))).toBe(true);
  });

  it("does not match a fetch() network error", () => {
    expect(isModuleLoadError(new TypeError("Failed to fetch"))).toBe(false);
  });
});
