import { describe, expect, it } from "vitest";
import { toLocalDate } from "./toLocalDate";

describe("toLocalDateString", () => {
  it("works", () => {
    const date = new Date(2024, 0, 15);
    expect(toLocalDate(date)).toBe("2024-01-15");
  });

  it("pads month and day", () => {
    const date = new Date(2024, 8, 5);
    expect(toLocalDate(date)).toBe("2024-09-05");
  });

  it("handles null", () => {
    expect(toLocalDate(null)).toBeNull();
  });

  it("handles undefined", () => {
    // eslint-disable-next-line @typescript-eslint/no-confusing-void-expression
    expect(toLocalDate(undefined)).toBeUndefined();
  });
});
