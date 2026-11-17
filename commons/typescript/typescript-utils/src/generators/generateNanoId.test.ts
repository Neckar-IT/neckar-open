/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { generateNanoId } from "./generateNanoId";

/**
 * Tests
 */

describe("generateNanoId", () => {
  it("should generate a nano id with 10 characters by default", () => {
    const nanoId = generateNanoId();
    expect(nanoId.length).toBe(10);
    expect(nanoId).toMatch(/^[0-9A-Za-z]{10}$/);
  });

  it("should respect the length parameter", () => {
    const longNanoId = generateNanoId(16);
    expect(longNanoId.length).toBe(16);
  });
});
