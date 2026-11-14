/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { useLanguage } from "../utility-hooks/useLanguage";
import { useRelativeTimeFormatter } from "./useRelativeTimeFormatter";

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

const systemTime = new Date("2025-01-01T12:00:00.000Z");

/**
 * Setup
 */

beforeEach(() => {
  vi.resetAllMocks();
});

beforeAll(() => {
  vi.setSystemTime(systemTime);
});

afterAll(() => {
  vi.useRealTimers();
});

/**
 * Tests
 */

describe("useRelativeTimeFormatter", () => {
  it("should format a date to a relative time string", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(useRelativeTimeFormatter);

    expect(result.current(new Date("2025-01-01T11:59:00.000Z"))).toBe("a minute ago");
    expect(result.current(new Date("2025-01-01T12:01:00.000Z"))).toBe("in a minute");
  });

  it("should remove the suffix if the option is set", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result } = renderHook(() => useRelativeTimeFormatter({ withoutAffix: true }));

    expect(result.current(new Date("2025-01-01T11:59:00.000Z"))).toBe("a minute");
    expect(result.current(new Date("2025-01-01T12:01:00.000Z"))).toBe("a minute");
  });

  it("should use the locale from useLanguage hook", () => {
    vi.mocked(useLanguage).mockReturnValue("en");
    const { result, rerender } = renderHook(() => useRelativeTimeFormatter());

    expect(result.current(new Date("2025-01-01T11:59:00.000Z"))).toBe("a minute ago");

    vi.mocked(useLanguage).mockReturnValue("de");
    rerender();

    expect(result.current(new Date("2025-01-01T11:59:00.000Z"))).toBe("vor einer Minute");
  });
});
