import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useNumberSeparators } from "./useNumberSeparators";

/**
 * Mocks
 */

const mocks = vi.hoisted(() => ({
  useNumberFormatter: vi.fn(),
}));

vi.mock("../formatter-hooks/useNumberFormatter", () => {
  return {
    useNumberFormatter: mocks.useNumberFormatter,
  };
});

beforeEach(() => {
  vi.resetAllMocks();
});

/**
 * Tests
 */

describe("useNumberSeparators", () => {
  it("should return the correct decimal and thousand separators for English", () => {
    mocks.useNumberFormatter.mockReturnValue(() => {
      return "1,234.5"; // US format
    });

    const { result } = renderHook(useNumberSeparators);
    expect(result.current).toEqual({
      decimalSeparator: ".",
      thousandSeparator: ",",
    });
  });

  it("should return the correct decimal and thousand separators for German", () => {
    mocks.useNumberFormatter.mockReturnValue(() => {
      return "1.234,5"; // US format
    });

    const { result } = renderHook(useNumberSeparators);
    expect(result.current).toEqual({
      decimalSeparator: ",",
      thousandSeparator: ".",
    });
  });
});
