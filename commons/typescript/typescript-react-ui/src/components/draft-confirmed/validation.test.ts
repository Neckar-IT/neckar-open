/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { collectFieldErrors, type FieldValidationError } from "./validation";

/**
 * Tests
 */

describe("collectFieldErrors", () => {
  it("returns an empty list for an empty record", () => {
    expect(collectFieldErrors({})).toEqual([]);
  });

  it("flattens the errors of a single field", () => {
    const errors: FieldValidationError[] = [{ message: "required" }, { message: "too short" }];

    expect(collectFieldErrors({ name: errors })).toEqual(errors);
  });

  it("concatenates errors across multiple fields", () => {
    const result = collectFieldErrors({
      name: [{ message: "required" }],
      email: [{ message: "invalid" }, { message: "taken" }],
    });

    expect(result).toEqual([{ message: "required" }, { message: "invalid" }, { message: "taken" }]);
  });

  it("skips fields whose value is undefined", () => {
    const result = collectFieldErrors({
      name: [{ message: "required" }],
      email: undefined,
    });

    expect(result).toEqual([{ message: "required" }]);
  });

  it("treats an empty error array as contributing nothing", () => {
    const result = collectFieldErrors({
      name: [],
      email: [{ message: "invalid" }],
    });

    expect(result).toEqual([{ message: "invalid" }]);
  });

  it("preserves the linkTo property of an error", () => {
    const result = collectFieldErrors({
      name: [{ message: "see details", linkTo: "https://example.com/help" }],
    });

    expect(result).toEqual([{ message: "see details", linkTo: "https://example.com/help" }]);
  });
});
