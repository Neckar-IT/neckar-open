/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { isGoodResponse } from "./isGoodResponse";

/**
 * Tests
 */

describe("isGoodResponse", () => {
  it("should return true for valid GoodResponse objects", () => {
    const validGoodResponse = { type: "good", data: {} };
    expect(isGoodResponse(validGoodResponse)).toBe(true);
  });

  it("should return false for invalid GoodResponse objects", () => {
    const invalidGoodResponse = { type: "bad/some-error" };
    expect(isGoodResponse(invalidGoodResponse)).toBe(false);
  });

  it("should return false for non-object types", () => {
    expect(isGoodResponse(null)).toBe(false);
    expect(isGoodResponse(undefined)).toBe(false);
    expect(isGoodResponse(42)).toBe(false);
    expect(isGoodResponse("string")).toBe(false);
    expect(isGoodResponse([])).toBe(false);
  });
});
