/**
 * Tests for Patchable type, factory functions, and type guards.
 */

import { describe, expect, it } from "vitest";
import { getPatchableValue, isPatchableDelete, isPatchableKeep, isPatchableUpdate, Patchable, patchable } from "./Patchable";

describe("Patchable", () => {
  describe("factory functions", () => {
    it("should create Update with Patchable.update()", () => {
      const update = Patchable.update("new value");

      expect(update).toEqual({ type: "Update", value: "new value" });
    });

    it("should create Keep with Patchable.keep()", () => {
      const keep = Patchable.keep();

      expect(keep).toEqual({ type: "Keep" });
    });

    it("should create Delete with Patchable.delete()", () => {
      const del = Patchable.delete();

      expect(del).toEqual({ type: "Delete" });
    });
  });

  describe("Patchable.from()", () => {
    it("should create Update from non-null value", () => {
      const patch = Patchable.from("value");

      expect(patch).toEqual({ type: "Update", value: "value" });
    });

    it("should create Delete from null", () => {
      const patch = Patchable.from(null);

      expect(patch).toEqual({ type: "Delete" });
    });

    it("should create Keep from undefined", () => {
      const patch = Patchable.from(undefined);

      expect(patch).toEqual({ type: "Keep" });
    });

    it("should work with different value types", () => {
      expect(Patchable.from(42)).toEqual({ type: "Update", value: 42 });
      expect(Patchable.from({ name: "test" })).toEqual({ type: "Update", value: { name: "test" } });
      expect(Patchable.from([1, 2, 3])).toEqual({ type: "Update", value: [1, 2, 3] });
    });
  });

  describe("patchable() shorthand", () => {
    it("should create Update from non-null value", () => {
      const patch = patchable("value");

      expect(patch).toEqual({ type: "Update", value: "value" });
    });

    it("should create Delete from null", () => {
      const patch = patchable(null);

      expect(patch).toEqual({ type: "Delete" });
    });

    it("should create Keep from undefined", () => {
      const patch = patchable(undefined);

      expect(patch).toEqual({ type: "Keep" });
    });

    it("should work with different value types", () => {
      expect(patchable(42)).toEqual({ type: "Update", value: 42 });
      expect(patchable({ name: "test" })).toEqual({ type: "Update", value: { name: "test" } });
      expect(patchable([1, 2, 3])).toEqual({ type: "Update", value: [1, 2, 3] });
    });
  });

  describe("type guards", () => {
    it("should identify Update variant", () => {
      const update: Patchable<string> = { type: "Update", value: "new value" };

      expect(isPatchableUpdate(update)).toBe(true);
      expect(isPatchableKeep(update)).toBe(false);
      expect(isPatchableDelete(update)).toBe(false);
    });

    it("should identify Keep variant", () => {
      const keep: Patchable<string> = { type: "Keep" };

      expect(isPatchableUpdate(keep)).toBe(false);
      expect(isPatchableKeep(keep)).toBe(true);
      expect(isPatchableDelete(keep)).toBe(false);
    });

    it("should identify Delete variant", () => {
      const del: Patchable<string> = { type: "Delete" };

      expect(isPatchableUpdate(del)).toBe(false);
      expect(isPatchableKeep(del)).toBe(false);
      expect(isPatchableDelete(del)).toBe(true);
    });
  });

  describe("getPatchableValue", () => {
    it("should return new value for Update", () => {
      const update: Patchable<string> = { type: "Update", value: "new value" };

      expect(getPatchableValue(update, "current")).toBe("new value");
    });

    it("should return current value for Keep", () => {
      const keep: Patchable<string> = { type: "Keep" };

      expect(getPatchableValue(keep, "current")).toBe("current");
    });

    it("should return null for Delete", () => {
      const del: Patchable<string> = { type: "Delete" };

      expect(getPatchableValue(del, "current")).toBe(null);
    });

    it("should handle null current value with Keep", () => {
      const keep: Patchable<string> = { type: "Keep" };

      expect(getPatchableValue(keep, null)).toBe(null);
    });
  });

  describe("discriminated union narrowing", () => {
    it("should narrow type correctly in switch statement", () => {
      const patchValue = { type: "Update", value: "test" } as Patchable<string>;

      let result: string | null = null;

      switch (patchValue.type) {
        case "Update":
          result = patchValue.value;
          break;
        case "Keep":
          result = "kept";
          break;
        case "Delete":
          result = null;
          break;
        default:
          throw new Error(`Unexpected value in Switch Statement: patchValue.type`);
      }

      expect(result).toBe("test");
    });
  });
});
