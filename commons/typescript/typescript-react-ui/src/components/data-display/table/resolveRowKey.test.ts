/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { resolveRowKey } from "./resolveRowKey";

/**
 * Tests
 */

describe("resolveRowKey", () => {
  it("uses the string id when the item has one", () => {
    expect(resolveRowKey({ id: "abc" }, 3)).toBe("abc");
  });

  it("falls back to the index when the id is not a string", () => {
    expect(resolveRowKey({ id: 42 }, 3)).toBe("3");
  });

  it("falls back to the index when the item has no id property", () => {
    expect(resolveRowKey({ name: "no-id" }, 7)).toBe("7");
  });

  it("falls back to the index for a null item", () => {
    expect(resolveRowKey(null, 1)).toBe("1");
  });

  it("falls back to the index for a primitive item", () => {
    expect(resolveRowKey("just-a-string", 5)).toBe("5");
  });

  it("falls back to the index when id is null", () => {
    expect(resolveRowKey({ id: null }, 2)).toBe("2");
  });
});
