/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useLanguage } from "../utility-hooks/useLanguage";
import { useDateTimeFormatter } from "./useDateTimeFormatter";

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

describe("useDateTimeFormatter", () => {
  const date = new Date("2021-02-03T12:34:56");

  it("should format a date to a string in the default format", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(useDateTimeFormatter);

    expect(result.current(date)).toBe("02/03/2021");
  });

  it("should start the new day with 00:00:00", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter("time-with-seconds"));

    expect(result.current(new Date("2021-01-01T00:12:34"))).toBe("00:12:34");
  });

  it("should use the locale from useLanguage hook", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(() => useDateTimeFormatter("date-with-weekday"));

    expect(result.current(date)).toBe("Wed, 02/03/2021");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(date)).toBe("Mi., 03.02.2021");
  });

  it("should output the correct format for the 'date' style", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter("date"));

    expect(result.current(date)).toBe("02/03/2021");
  });

  it("should output the correct format for the 'date-and-time' style", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter("date-and-time"));

    expect(result.current(date)).toBe("02/03/2021, 12:34");
  });

  it("should output the correct format for the 'date-and-time-with-seconds' style", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter("date-and-time-with-seconds"));

    expect(result.current(date)).toBe("02/03/2021, 12:34:56");
  });

  it("should output the correct format for the 'date-with-weekday' style", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter("date-with-weekday"));

    expect(result.current(date)).toBe("Wed, 02/03/2021");
  });

  it("should output the correct format for the 'date-with-weekday-and-time' style", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter("date-with-weekday-and-time"));

    expect(result.current(date)).toBe("Wed, 02/03/2021, 12:34");
  });

  it("should output the correct format for the 'time' style", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter("time"));

    expect(result.current(date)).toBe("12:34");
  });

  it("should output the correct format for the 'time-with-seconds' style", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter("time-with-seconds"));

    expect(result.current(date)).toBe("12:34:56");
  });

  it("should throw an error if the date is invalid", () => {
    const originalConsoleError = console.error;

    // suppress console.error output
    console.error = vi.fn();

    const { result } = renderHook(useDateTimeFormatter);

    try {
      expect(() => result.current(new Date("invalid"))).toThrow();
      expect(() => result.current(new Date(NaN))).toThrow();
    } finally {
      // restore console.error
      console.error = originalConsoleError;
    }
  });

  it("should handle null and undefined values", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useDateTimeFormatter());

    expect(result.current(null)).toBeNull();
    // eslint-disable-next-line @typescript-eslint/no-confusing-void-expression
    expect(result.current(undefined)).toBeUndefined();
  });
});
