/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { formatErrorBody } from "./formatErrorBody";

/**
 * Tests
 */

describe("formatErrorBody", () => {
  it("returns undefined for empty payloads", () => {
    expect(formatErrorBody(undefined)).toBeUndefined();
    expect(formatErrorBody(null)).toBeUndefined();
    expect(formatErrorBody("")).toBeUndefined();
  });

  it("returns strings unchanged", () => {
    expect(formatErrorBody("boom")).toBe("boom");
  });

  it("pretty-prints objects as JSON", () => {
    expect(formatErrorBody({ message: "boom" })).toBe('{\n  "message": "boom"\n}');
  });

  it("stringifies primitive numbers and booleans", () => {
    expect(formatErrorBody(42)).toBe("42");
    expect(formatErrorBody(false)).toBe("false");
  });

  it("keeps a body at or below the length limit intact", () => {
    const body = "x".repeat(100);
    expect(formatErrorBody(body, 100)).toBe(body);
  });

  it("truncates a body above the length limit and keeps the prefix", () => {
    const body = "x".repeat(50);
    const result = formatErrorBody(body, 10);
    expect(result).toBe(`${"x".repeat(10)}\n… (truncated, 50 characters total)`);
  });

  it("cuts before an emoji the limit would split", () => {
    const body = `${"x".repeat(9)}😀${"x".repeat(40)}`;

    expect(formatErrorBody(body, 10)).toBe(`${"x".repeat(9)}\n… (truncated, 51 characters total)`);
  });

  it("keeps an emoji that ends exactly at the limit", () => {
    const body = `${"x".repeat(9)}😀${"x".repeat(40)}`;

    expect(formatErrorBody(body, 11)).toBe(`${"x".repeat(9)}😀\n… (truncated, 51 characters total)`);
  });

  it("names size and type of a Blob instead of serialising it as an empty object", () => {
    expect(formatErrorBody(new Blob(['{"message":"boom"}'], { type: "application/json" }))).toBe("[Blob, 18 bytes, application/json]");
    expect(formatErrorBody(new Blob(["abc"]))).toBe("[Blob, 3 bytes]");
  });

  it("returns a marker for an unserializable object instead of throwing", () => {
    const circular: Record<string, unknown> = {};
    circular.self = circular;
    expect(formatErrorBody(circular)).toBe("[unserializable]");
  });
});
