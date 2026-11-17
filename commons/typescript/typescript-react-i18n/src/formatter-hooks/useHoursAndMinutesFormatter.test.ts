/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useHoursAndMinutesFormatter } from "./useHoursAndMinutesFormatter";

/**
 * Tests
 */

describe("useHoursAndMinutesFormatter", () => {
  it("should format whole hours with zero minutes", () => {
    const { result } = renderHook(useHoursAndMinutesFormatter);

    expect(result.current(2)).toBe("2:00");
  });

  it("should format fractions of an hour as padded minutes", () => {
    const { result } = renderHook(useHoursAndMinutesFormatter);

    expect(result.current(1.5)).toBe("1:30");
    expect(result.current(0.05)).toBe("0:03");
  });

  it("should round to whole minutes and carry over into the hours", () => {
    const { result } = renderHook(useHoursAndMinutesFormatter);

    expect(result.current(0.999)).toBe("1:00");
  });

  it("should format zero as 0:00", () => {
    const { result } = renderHook(useHoursAndMinutesFormatter);

    expect(result.current(0)).toBe("0:00");
  });

  it("should return the same function on every render", () => {
    const { result, rerender } = renderHook(useHoursAndMinutesFormatter);
    const first = result.current;

    rerender();

    expect(result.current).toBe(first);
  });
});
