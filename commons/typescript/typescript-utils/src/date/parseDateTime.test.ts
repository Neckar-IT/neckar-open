import { describe, expect, it } from "vitest";
import { parseDateTime } from "./parseDateTime";

describe("parseDateTime", () => {
  it("parses valid ISO date-time strings", () => {
    const date = parseDateTime("2024-06-15T13:45:30.123Z");
    expect(date.getUTCFullYear()).toBe(2024);
    expect(date.getUTCMonth()).toBe(5); // Months are zero-based
    expect(date.getUTCDate()).toBe(15);
    expect(date.getUTCHours()).toBe(13);
    expect(date.getUTCMinutes()).toBe(45);
    expect(date.getUTCSeconds()).toBe(30);
    expect(date.getUTCMilliseconds()).toBe(123);
  });

  it("throws error for invalid format", () => {
    expect(() => parseDateTime("2024-06-15 13:45:30")).toThrow(
      "DateTime must be in ISO format: 'YYYY-MM-DDTHH:mm:ss(.sss)Z' but was [2024-06-15 13:45:30]",
    );
    expect(() => parseDateTime("2024-06-15T13:45:30")).toThrow(
      "DateTime must be in ISO format: 'YYYY-MM-DDTHH:mm:ss(.sss)Z' but was [2024-06-15T13:45:30]",
    );
    expect(() => parseDateTime("2024/06/15T13:45:30Z")).toThrow(
      "DateTime must be in ISO format: 'YYYY-MM-DDTHH:mm:ss(.sss)Z' but was [2024/06/15T13:45:30Z]",
    );
  });

  it("throws error for invalid dates", () => {
    expect(() => parseDateTime("2024-02-30T13:45:30Z")).toThrow("Invalid date-time components");
    expect(() => parseDateTime("2024-04-31T13:45:30Z")).toThrow("Invalid date-time components");
    expect(() => parseDateTime("2024-13-01T13:45:30Z")).toThrow("Invalid ISO date-time");
  });

  it("throws error for invalid times", () => {
    expect(() => parseDateTime("2024-06-15T25:45:30Z")).toThrow("Invalid ISO date-time");
    expect(() => parseDateTime("2024-06-15T13:61:30Z")).toThrow("Invalid ISO date-time");
    expect(() => parseDateTime("2024-06-15T13:45:61Z")).toThrow("Invalid ISO date-time");
  });

  it("returns null for null input", () => {
    const result = parseDateTime(null);
    expect(result).toBeNull();
  });

  it("returns undefined for undefined input", () => {
    // eslint-disable-next-line @typescript-eslint/no-confusing-void-expression
    const result = parseDateTime(undefined);
    expect(result).toBeUndefined();
  });
});
