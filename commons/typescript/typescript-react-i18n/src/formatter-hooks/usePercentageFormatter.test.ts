/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useLanguage } from "../utility-hooks/useLanguage";
import { usePercentageFormatter } from "./usePercentageFormatter";

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

describe("usePercentageFormatter", () => {
  it("should format the percentage with the default options", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(usePercentageFormatter);

    expect(result.current(0.1234)).toBe("12%");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(0.1234)).toBe("12 %");
  });

  it("should pass the fractionDigit option through to the Intl.NumberFormat constructor", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(() => usePercentageFormatter(2));

    expect(result.current(0.1234)).toBe("12.34%");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(0.1234)).toBe("12,34 %");
  });
});
