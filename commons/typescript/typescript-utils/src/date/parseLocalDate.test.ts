import { describe, expect, it } from "vitest";
import { parseLocalDate } from "./parseLocalDate";

describe("parseLocalDate", () => {
  it("parses valid date strings", () => {
    const date = parseLocalDate("2024-06-15");
    expect(date.getFullYear()).toBe(2024);
    expect(date.getMonth()).toBe(5); // Months are zero-based
    expect(date.getDate()).toBe(15);
  });

  it("throws error for invalid format", () => {
    expect(() => parseLocalDate("15-06-2024")).toThrow("Date must be in 'YYYY-MM-DD' format but was: [15-06-2024]");
    expect(() => parseLocalDate("2024/06/15")).toThrow("Date must be in 'YYYY-MM-DD' format but was: [2024/06/15]");
    expect(() => parseLocalDate("June 15, 2024")).toThrow("Date must be in 'YYYY-MM-DD' format but was: [June 15, 2024]");
  });

  it("throws error for invalid dates", () => {
    expect(() => parseLocalDate("2024-02-30")).toThrow("Invalid date");
    expect(() => parseLocalDate("2024-04-31")).toThrow("Invalid date");
    expect(() => parseLocalDate("2024-13-01")).toThrow("Invalid date");
  });

  it("returns null for null input", () => {
    const result = parseLocalDate(null);
    expect(result).toBeNull();
  });

  it("returns undefined for undefined input", () => {
    // eslint-disable-next-line @typescript-eslint/no-confusing-void-expression
    const result = parseLocalDate(undefined);
    expect(result).toBeUndefined();
  });
});
