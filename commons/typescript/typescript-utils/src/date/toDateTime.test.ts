import { describe, expect, it } from "vitest";
import { toDateTime } from "./toDateTime";

describe("toDateTimeString", () => {
  it("works", () => {
    const date = new Date(Date.UTC(2024, 0, 15, 12, 30, 45));
    expect(toDateTime(date)).toBe("2024-01-15T12:30:45.000Z");
  });

  it("handles null", () => {
    expect(toDateTime(null)).toBeNull();
  });

  it("handles undefined", () => {
    // eslint-disable-next-line @typescript-eslint/no-confusing-void-expression
    expect(toDateTime(undefined)).toBeUndefined();
  });
});
