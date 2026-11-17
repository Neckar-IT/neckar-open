/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { isExpectedApplicationError, isUnexpectedApplicationError } from "./applicationErrors";

/**
 * Tests
 */

describe("isExpectedApplicationError", () => {
  it("matches the ExpectedError discriminator", () => {
    expect(isExpectedApplicationError({ type: "ExpectedError", errorCode: "AUTHZ-1" })).toBe(true);
  });

  it("rejects the unexpected variant and a payload without a discriminator", () => {
    expect(isExpectedApplicationError({ type: "UnexpectedError" })).toBe(false);
    expect(isExpectedApplicationError({ errorCode: "AUTHZ-1" })).toBe(false);
  });

  // Callers pass raw response bodies: `error.response?.data` is undefined on a network failure
  // and null on an empty JSON body. A throw here would take down the global error handler.
  it("returns false instead of throwing for non-object bodies", () => {
    expect(isExpectedApplicationError(undefined)).toBe(false);
    expect(isExpectedApplicationError(null)).toBe(false);
    expect(isExpectedApplicationError("")).toBe(false);
    expect(isExpectedApplicationError("<html>502 Bad Gateway</html>")).toBe(false);
  });
});

describe("isUnexpectedApplicationError", () => {
  it("matches the UnexpectedError discriminator", () => {
    expect(isUnexpectedApplicationError({ type: "UnexpectedError", message: "boom" })).toBe(true);
  });

  it("rejects the expected variant and non-object bodies", () => {
    expect(isUnexpectedApplicationError({ type: "ExpectedError" })).toBe(false);
    expect(isUnexpectedApplicationError(undefined)).toBe(false);
    expect(isUnexpectedApplicationError(null)).toBe(false);
  });
});
