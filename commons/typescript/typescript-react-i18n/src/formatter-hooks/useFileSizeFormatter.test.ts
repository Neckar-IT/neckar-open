/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useLanguage } from "../utility-hooks/useLanguage";
import { useFileSizeFormatter } from "./useFileSizeFormatter";

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

describe("useFileSizeFormatter", () => {
  it("should pick the largest unit in which the size is at least 1", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(useFileSizeFormatter);

    expect(result.current(0)).toBe("0 B");
    expect(result.current(512)).toBe("512 B");
    expect(result.current(1023)).toBe("1,023 B");
    expect(result.current(1024)).toBe("1 KB");
    expect(result.current(12 * 1024)).toBe("12 KB");
    expect(result.current(2.5 * 1024 * 1024)).toBe("2.5 MB");
    expect(result.current(3 * 1024 ** 3)).toBe("3 GB");
    expect(result.current(1.5 * 1024 ** 4)).toBe("1.5 TB");
  });

  it("should stay in TB for sizes beyond it", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(useFileSizeFormatter);

    expect(result.current(2048 * 1024 ** 4)).toBe("2,048 TB");
  });

  it("should move a size that rounds up to 1024 of its unit to the next one", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(useFileSizeFormatter);

    expect(result.current(1024 * 1024 - 1)).toBe("1 MB");
  });

  it("should round to at most one fraction digit", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(useFileSizeFormatter);

    expect(result.current(1234)).toBe("1.2 KB");
    expect(result.current(1024 * 1024 + 1)).toBe("1 MB");
  });

  it("should use the separators of the language", () => {
    vi.mocked(useLanguage).mockReturnValue("de");
    const { result } = renderHook(useFileSizeFormatter);

    expect(result.current(2.5 * 1024 * 1024)).toBe("2,5 MB");
  });

  it("should throw for a negative or not finite size", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(useFileSizeFormatter);

    expect(() => result.current(-1)).toThrow(/not negative/);
    expect(() => result.current(Number.NaN)).toThrow(/finite/);
  });
});
