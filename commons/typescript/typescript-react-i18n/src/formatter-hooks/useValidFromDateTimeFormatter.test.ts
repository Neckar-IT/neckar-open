/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useLanguage } from "../utility-hooks/useLanguage";
import { useValidFromDateTimeFormatter } from "./useValidFromDateTimeFormatter";

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
 *
 * Inputs are constructed as local-time instants (no `Z` suffix) so that `new Date(...)`
 * resolves to exactly the wall-clock time stated, independent of the host timezone.
 */

describe("useValidFromDateTimeFormatter", () => {
  it("returns date only for a local-midnight instant in de-DE", () => {
    vi.mocked(useLanguage).mockReturnValue("de");
    const { result } = renderHook(() => useValidFromDateTimeFormatter());

    expect(result.current("2020-01-15T00:00:00")).toBe("15.01.2020");
  });

  it("returns date only for a local-midnight instant in en-US", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useValidFromDateTimeFormatter());

    expect(result.current("2020-01-15T00:00:00")).toBe("01/15/2020");
  });

  it("returns date + time for a non-midnight instant in de-DE", () => {
    vi.mocked(useLanguage).mockReturnValue("de");
    const { result } = renderHook(() => useValidFromDateTimeFormatter());

    expect(result.current("2020-01-15T14:30:00")).toBe("15.01.2020, 14:30");
  });

  it("returns date + time for a non-midnight instant in en-US", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useValidFromDateTimeFormatter());

    expect(result.current("2020-01-15T14:30:00")).toBe("01/15/2020, 14:30");
  });

  it("treats any non-zero time component as non-midnight", () => {
    vi.mocked(useLanguage).mockReturnValue("de");
    const { result } = renderHook(() => useValidFromDateTimeFormatter());

    expect(result.current("2020-01-15T00:00:01")).toBe("15.01.2020, 00:00");
  });
});
