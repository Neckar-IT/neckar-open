/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { parseNumber } from "./parseNumber";

/**
 * Tests
 */

describe("parseNumber", () => {
  it("should parse the input correctly with '.' as decimal separator", () => {
    const decimalSeparator = ".";

    expect(parseNumber("123", decimalSeparator)).toBe(123);
    expect(parseNumber("1.234", decimalSeparator)).toBe(1.234);
    expect(parseNumber("1,234", decimalSeparator)).toBe(1234);
    expect(parseNumber("12,345", decimalSeparator)).toBe(12345);
    expect(parseNumber("12,345.67", decimalSeparator)).toBe(12345.67);
    expect(parseNumber("-12,345.67", decimalSeparator)).toBe(-12345.67);
  });

  it("should parse the input correctly with ',' as decimal separator", () => {
    const decimalSeparator = ",";

    expect(parseNumber("123", decimalSeparator)).toBe(123);
    expect(parseNumber("1,234", decimalSeparator)).toBe(1.234);
    expect(parseNumber("1.234", decimalSeparator)).toBe(1234);
    expect(parseNumber("12.345", decimalSeparator)).toBe(12345);
    expect(parseNumber("12.345,67", decimalSeparator)).toBe(12345.67);
    expect(parseNumber("-12.345,67", decimalSeparator)).toBe(-12345.67);
  });

  it("should round to the specified number of fraction digits", () => {
    expect(parseNumber("12.34567", ".", 2)).toBe(12.35);
    expect(parseNumber("12.34567", ".", 3)).toBe(12.346);
    expect(parseNumber("12.34567", ".", 0)).toBe(12);
  });

  it("should round negative numbers to the specified number of fraction digits", () => {
    expect(parseNumber("-12.34567", ".", 2)).toBe(-12.35);
    expect(parseNumber("-12.34567", ".", 3)).toBe(-12.346);
    expect(parseNumber("-12.34567", ".", 0)).toBe(-12);
  });

  it("should remove all invalid characters", () => {
    expect(parseNumber("abc123def", ".")).toBe(123);
    expect(parseNumber("$1,234.56", ".")).toBe(1234.56);
    expect(parseNumber("12-34.56", ".")).toBe(1234.56);
  });

  it("should return null for empty string and only invalid characters", () => {
    expect(parseNumber("", ".")).toBeNull();
    expect(parseNumber("abc", ".")).toBeNull();
  });
});
