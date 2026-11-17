import { describe, expect, it } from "vitest";
import { adjustableValueTextEquals, recalculatedAdjustableValueText } from "./recalculatedAdjustableValueText";

describe("recalculatedAdjustableValueText", () => {
  it("starts at Auto when nothing is stored", () => {
    expect(recalculatedAdjustableValueText(null, "Harper Lee")).toEqual({ type: "Auto", value: "Harper Lee" });
  });

  it("follows the fresh calculation while Auto", () => {
    expect(recalculatedAdjustableValueText({ type: "Auto", value: "Harper Le" }, "Harper Lee")).toEqual({
      type: "Auto",
      value: "Harper Lee",
    });
  });

  it("keeps an override and refreshes the calculated side", () => {
    expect(
      recalculatedAdjustableValueText({ type: "Adjusted", calculated: "Eric Blair", value: "George Orwell" }, "Eric Arthur Blair"),
    ).toEqual({
      type: "Adjusted",
      calculated: "Eric Arthur Blair",
      value: "George Orwell",
    });
  });

  it("promotes a Manual value once a calculation exists", () => {
    expect(recalculatedAdjustableValueText({ type: "Manual", value: "Anonymus" }, "Jane Austen")).toEqual({
      type: "Adjusted",
      calculated: "Jane Austen",
      value: "Anonymus",
    });
  });

  it("demotes an Adjusted value to Manual when the calculation loses its result", () => {
    expect(recalculatedAdjustableValueText({ type: "Adjusted", calculated: "Eric Blair", value: "George Orwell" }, null)).toEqual({
      type: "Manual",
      value: "George Orwell",
    });
  });

  it("keeps a Manual value without a result", () => {
    expect(recalculatedAdjustableValueText({ type: "Manual", value: "Anonymus" }, null)).toEqual({ type: "Manual", value: "Anonymus" });
  });

  it("drops an Auto value without a result", () => {
    expect(recalculatedAdjustableValueText({ type: "Auto", value: "Harper Lee" }, null)).toBeNull();
    expect(recalculatedAdjustableValueText(null, null)).toBeNull();
  });
});

describe("adjustableValueTextEquals", () => {
  it("compares type, value and the calculated side", () => {
    expect(adjustableValueTextEquals(null, null)).toBe(true);
    expect(adjustableValueTextEquals(null, { type: "Auto", value: "x" })).toBe(false);
    expect(adjustableValueTextEquals({ type: "Auto", value: "x" }, { type: "Auto", value: "x" })).toBe(true);
    expect(adjustableValueTextEquals({ type: "Auto", value: "x" }, { type: "Manual", value: "x" })).toBe(false);
    expect(
      adjustableValueTextEquals({ type: "Adjusted", calculated: "a", value: "x" }, { type: "Adjusted", calculated: "b", value: "x" }),
    ).toBe(false);
    expect(
      adjustableValueTextEquals({ type: "Adjusted", calculated: "a", value: "x" }, { type: "Adjusted", calculated: "a", value: "x" }),
    ).toBe(true);
  });
});
