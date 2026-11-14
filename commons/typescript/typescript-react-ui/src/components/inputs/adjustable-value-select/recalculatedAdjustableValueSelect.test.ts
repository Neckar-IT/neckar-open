import { describe, expect, it } from "vitest";
import { adjustableValueSelectEquals, recalculatedAdjustableValueSelect } from "./recalculatedAdjustableValueSelect";

/**
 * Tests
 *
 * `recalculatedAdjustableValueSelect` re-wraps a field around a fresh calculated value: a result keeps
 * the field on the `Auto`/`Adjusted` axis, its absence drops it to the `null`/`Manual` axis. The
 * override — where the user has one — always survives.
 */

describe("recalculatedAdjustableValueSelect", () => {
  describe("with a calculated result", () => {
    it("makes an inherited field follow the result", () => {
      expect(recalculatedAdjustableValueSelect(null, "silver")).toEqual({ type: "Auto", value: "silver" });
      expect(recalculatedAdjustableValueSelect({ type: "Auto", value: "black" }, "silver")).toEqual({ type: "Auto", value: "silver" });
    });

    it("keeps an override and refreshes its calculated counterpart", () => {
      // The new calculated value ("black") differs from the current one ("silver"), so the assertion
      // proves the calculated side is actually refreshed rather than left untouched.
      expect(recalculatedAdjustableValueSelect({ type: "Adjusted", calculated: "silver", value: "silver" }, "black")).toEqual({
        type: "Adjusted",
        calculated: "black",
        value: "silver",
      });
    });

    it("promotes a manual override to an adjusted one once a counterpart exists", () => {
      expect(recalculatedAdjustableValueSelect({ type: "Manual", value: "silver" }, "black")).toEqual({
        type: "Adjusted",
        calculated: "black",
        value: "silver",
      });
    });
  });

  describe("without a calculated result", () => {
    it("drops an inherited field to nothing", () => {
      expect(recalculatedAdjustableValueSelect(null, null)).toBeNull();
      expect(recalculatedAdjustableValueSelect({ type: "Auto", value: "silver" }, null)).toBeNull();
    });

    it("drops an adjusted override to a manual one", () => {
      expect(recalculatedAdjustableValueSelect({ type: "Adjusted", calculated: "black", value: "silver" }, null)).toEqual({
        type: "Manual",
        value: "silver",
      });
    });

    it("leaves a manual override untouched", () => {
      expect(recalculatedAdjustableValueSelect({ type: "Manual", value: "silver" }, null)).toEqual({ type: "Manual", value: "silver" });
    });
  });
});

describe("adjustableValueSelectEquals", () => {
  it("treats two nulls as equal and a null against a value as unequal", () => {
    expect(adjustableValueSelectEquals(null, null)).toBe(true);
    expect(adjustableValueSelectEquals(null, { type: "Auto", value: "silver" })).toBe(false);
  });

  it("compares the effective value", () => {
    expect(adjustableValueSelectEquals({ type: "Auto", value: "silver" }, { type: "Auto", value: "silver" })).toBe(true);
    expect(adjustableValueSelectEquals({ type: "Auto", value: "silver" }, { type: "Auto", value: "black" })).toBe(false);
  });

  it("compares the calculated counterpart of adjusted values", () => {
    expect(
      adjustableValueSelectEquals(
        { type: "Adjusted", calculated: "black", value: "silver" },
        { type: "Adjusted", calculated: "silver", value: "silver" },
      ),
    ).toBe(false);
  });
});
