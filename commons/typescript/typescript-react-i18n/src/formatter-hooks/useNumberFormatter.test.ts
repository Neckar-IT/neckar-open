/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useLanguage } from "../utility-hooks/useLanguage";
import { useNumberFormatter } from "./useNumberFormatter";

/**
 * Mocks
 */

const mocks = vi.hoisted(() => ({
  useLanguage: vi.fn(),
}));

vi.mock("../utility-hooks/useLanguage", () => {
  return {
    useLanguage: mocks.useLanguage,
  };
});

beforeEach(() => {
  vi.resetAllMocks();
});

/**
 * Tests
 */

describe("useNumberFormatter", () => {
  it("should format the number with the default options", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(useNumberFormatter);

    expect(result.current(123456.789)).toBe("123,456.789");
    expect(result.current(123456)).toBe("123,456");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(123456.789)).toBe("123.456,789");
    expect(result.current(123456)).toBe("123.456");
  });

  it("should pass the fractionDigit option through to the Intl.NumberFormat constructor", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(() => useNumberFormatter(2));

    expect(result.current(123456.789)).toBe("123,456.79");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(123456.789)).toBe("123.456,79");
  });

  it("should handle edge cases", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(useNumberFormatter);

    expect(result.current(Number.NaN)).toBe("NaN");
    expect(result.current(Number.POSITIVE_INFINITY)).toBe("∞");
    expect(result.current(Number.NEGATIVE_INFINITY)).toBe("-∞");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(Number.NaN)).toBe("NaN");
    expect(result.current(Number.POSITIVE_INFINITY)).toBe("∞");
    expect(result.current(Number.NEGATIVE_INFINITY)).toBe("-∞");
  });
});
