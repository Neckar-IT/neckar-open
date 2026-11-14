/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useLanguage } from "../utility-hooks/useLanguage";
import { useCurrencyFormatter } from "./useCurrencyFormatter";

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

describe("useCurrencyFormatter", () => {
  it("should format the number as euro by default", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(() => useCurrencyFormatter());

    expect(result.current(1234.56)).toBe("€1,234.56");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(1234.56)).toBe("1.234,56 €");
  });

  it("should format the number as currency with USD", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(() => useCurrencyFormatter({ currency: "USD" }));

    expect(result.current(1234.56)).toBe("$1,234.56");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(1234.56)).toBe("1.234,56 $");
  });

  it("should return the placeholder if the value is undefined", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useCurrencyFormatter({ emptyPlaceholder: "-" }));

    expect(result.current(undefined)).toBe("-");
  });
});
