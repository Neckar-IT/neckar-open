import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { describe, expect, it } from "vitest";
import { needsEditing } from "./needsEditing";

describe("needsEditing", () => {
  it("returns true for a draft with an invalid validation result", () => {
    expect(needsEditing({ confirmationStatus: ConfirmationStatus.Draft, validationResult: { type: "Invalid" } })).toBe(true);
  });

  it("returns false for a draft with a valid validation result", () => {
    expect(needsEditing({ confirmationStatus: ConfirmationStatus.Draft, validationResult: { type: "Valid" } })).toBe(false);
  });

  it("returns false for a confirmed entity regardless of validation result", () => {
    expect(needsEditing({ confirmationStatus: ConfirmationStatus.Confirmed, validationResult: { type: "Invalid" } })).toBe(false);
    expect(needsEditing({ confirmationStatus: ConfirmationStatus.Confirmed, validationResult: { type: "Valid" } })).toBe(false);
  });
});
