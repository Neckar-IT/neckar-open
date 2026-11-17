import { describe, expect, it } from "vitest";
import { enumTranslationKeys } from "./enumTranslationKeys";

describe("enumTranslationKeys", () => {
  it("looks up the shared flat key when no enum is named", () => {
    expect(enumTranslationKeys("Selected")).toEqual(["enums.Selected"]);
  });

  it("puts the qualified key in front of the flat one", () => {
    expect(enumTranslationKeys("None", "MonitoringSystem")).toEqual(["enums.MonitoringSystem.None", "enums.None"]);
  });

  /**
   * The two collisions the qualified namespace was introduced for: `enums.None` is taken by "without
   * cable" and `enums.Optional` by plain "optional", so both dropdowns showed the other enum's label.
   */
  it("separates the two values that used to collide", () => {
    expect(enumTranslationKeys("None", "MonitoringSystem")[0]).not.toBe(enumTranslationKeys("None", "CableRouting")[0]);
    expect(enumTranslationKeys("Optional", "ConfiguredOptionality")[0]).not.toBe(enumTranslationKeys("Optional", "Optionality")[0]);
  });

  it("keeps a numeric value usable as a key segment", () => {
    expect(enumTranslationKeys(70, "InverterSetting")).toEqual(["enums.InverterSetting.70", "enums.70"]);
  });
});
