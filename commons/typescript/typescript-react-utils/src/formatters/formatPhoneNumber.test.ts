/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { formatPhoneNumber } from "./formatPhoneNumber";

/**
 * Tests
 */

describe("formatPhoneNumber", () => {
  it("should format a german phone number correctly", () => {
    expect(formatPhoneNumber("074739594960")).toStrictEqual({ isValid: true, value: "07473 9594960" });
    expect(formatPhoneNumber("004974739594960")).toStrictEqual({ isValid: true, value: "07473 9594960" });
    expect(formatPhoneNumber("01795468156")).toStrictEqual({ isValid: true, value: "0179 5468156" });
    expect(formatPhoneNumber("07115432345")).toStrictEqual({ isValid: true, value: "0711 5432345" });
  });

  it("should format an international phone number correctly", () => {
    expect(formatPhoneNumber("+41719986810")).toStrictEqual({ isValid: true, value: "+41 71 998 68 10" });
    expect(formatPhoneNumber("+4357200200")).toStrictEqual({ isValid: true, value: "+43 57 200 200" });
  });

  it("should return the original string if it is not a valid phone number", () => {
    expect(formatPhoneNumber("017996904")).toStrictEqual({ isValid: false, value: "017996904" });
    expect(formatPhoneNumber("074739")).toStrictEqual({ isValid: false, value: "074739" });
  });
});
