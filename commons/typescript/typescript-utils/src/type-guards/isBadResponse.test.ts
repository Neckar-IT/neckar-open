/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { isBadResponse } from "./isBadResponse";

/**
 * Tests
 */

describe("isBadResponse", () => {
  it("should return true for valid failure response objects", () => {
    const validFailureResponse = { type: "bad/some-error" };
    expect(isBadResponse(validFailureResponse)).toBe(true);
  });

  it("should return false for a subtype-less bad discriminator", () => {
    expect(isBadResponse({ type: "bad" })).toBe(false);
  });

  it("should return false for a discriminator that only starts with bad", () => {
    expect(isBadResponse({ type: "badger" })).toBe(false);
  });

  it("should return false for invalid failure response objects", () => {
    const invalidFailureResponse = { type: "good" };
    expect(isBadResponse(invalidFailureResponse)).toBe(false);
  });

  it("should return false for non-object types", () => {
    expect(isBadResponse(null)).toBe(false);
    expect(isBadResponse(undefined)).toBe(false);
    expect(isBadResponse(42)).toBe(false);
    expect(isBadResponse("string")).toBe(false);
    expect(isBadResponse([])).toBe(false);
  });
});
