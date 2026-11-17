/**
 * Tests for PatchableRequired type, factory functions, and type guards.
 */

import { describe, expect, it } from "vitest";
import {
  getPatchableRequiredValue,
  isPatchableRequiredKeep,
  isPatchableRequiredUpdate,
  PatchableRequired,
  patchableRequired,
} from "./PatchableRequired";

describe("PatchableRequired", () => {
  describe("factory functions", () => {
    it("should create Update with PatchableRequired.update()", () => {
      const update = PatchableRequired.update("new value");

      expect(update).toEqual({ type: "Update", value: "new value" });
    });

    it("should create Keep with PatchableRequired.keep()", () => {
      const keep = PatchableRequired.keep();

      expect(keep).toEqual({ type: "Keep" });
    });

    it("should NOT have a delete() method", () => {
      expect("delete" in PatchableRequired).toBe(false);
    });

    it("should work with different value types", () => {
      const numberPatch = PatchableRequired.update(42);
      const objectPatch = PatchableRequired.update({ name: "test" });

      expect(numberPatch).toEqual({ type: "Update", value: 42 });
      expect(objectPatch).toEqual({ type: "Update", value: { name: "test" } });
    });
  });

  describe("PatchableRequired.from()", () => {
    it("should create Update from non-undefined value", () => {
      const patch = PatchableRequired.from("value");

      expect(patch).toEqual({ type: "Update", value: "value" });
    });

    it("should create Keep from undefined", () => {
      const patch = PatchableRequired.from(undefined);

      expect(patch).toEqual({ type: "Keep" });
    });

    it("should treat null as Update (not Delete)", () => {
      const patch = PatchableRequired.from(null);

      expect(patch).toEqual({ type: "Update", value: null });
    });

    it("should work with different value types", () => {
      expect(PatchableRequired.from(42)).toEqual({ type: "Update", value: 42 });
      expect(PatchableRequired.from({ name: "test" })).toEqual({ type: "Update", value: { name: "test" } });
    });
  });

  describe("patchableRequired() shorthand", () => {
    it("should create Update from non-undefined value", () => {
      const patch = patchableRequired("value");

      expect(patch).toEqual({ type: "Update", value: "value" });
    });

    it("should create Keep from undefined", () => {
      const patch = patchableRequired(undefined);

      expect(patch).toEqual({ type: "Keep" });
    });

    it("should treat null as Update (not Delete)", () => {
      const patch = patchableRequired(null);

      expect(patch).toEqual({ type: "Update", value: null });
    });

    it("should work with different value types", () => {
      expect(patchableRequired(42)).toEqual({ type: "Update", value: 42 });
      expect(patchableRequired({ name: "test" })).toEqual({ type: "Update", value: { name: "test" } });
    });
  });

  describe("type guards", () => {
    it("should identify Update variant", () => {
      const update: PatchableRequired<string> = { type: "Update", value: "new value" };

      expect(isPatchableRequiredUpdate(update)).toBe(true);
      expect(isPatchableRequiredKeep(update)).toBe(false);
    });

    it("should identify Keep variant", () => {
      const keep: PatchableRequired<string> = { type: "Keep" };

      expect(isPatchableRequiredUpdate(keep)).toBe(false);
      expect(isPatchableRequiredKeep(keep)).toBe(true);
    });
  });

  describe("getPatchableRequiredValue", () => {
    it("should return new value for Update", () => {
      const update: PatchableRequired<string> = { type: "Update", value: "new value" };

      expect(getPatchableRequiredValue(update, "current")).toBe("new value");
    });

    it("should return current value for Keep", () => {
      const keep: PatchableRequired<string> = { type: "Keep" };

      expect(getPatchableRequiredValue(keep, "current")).toBe("current");
    });
  });

  describe("discriminated union narrowing", () => {
    it("should narrow type correctly in switch statement", () => {
      const patchValue = { type: "Update", value: "test" } as PatchableRequired<string>;

      let result: string;

      switch (patchValue.type) {
        case "Update":
          result = patchValue.value;
          break;
        case "Keep":
          result = "kept";
          break;
        default:
          throw new Error(`Unexpected value in Switch Statement: patchValue.type`);
      }

      expect(result).toBe("test");
    });

    it("should be exhaustive (no Delete case needed)", () => {
      const patchValue: PatchableRequired<string> = { type: "Keep" };

      function applyPatch(patch: PatchableRequired<string>, current: string): string {
        switch (patch.type) {
          case "Update":
            return patch.value;
          case "Keep":
            return current;
          default:
            throw new Error(`Unexpected value in Switch Statement: patch.type`);
        }
      }

      expect(applyPatch(patchValue, "existing")).toBe("existing");
    });
  });
});
